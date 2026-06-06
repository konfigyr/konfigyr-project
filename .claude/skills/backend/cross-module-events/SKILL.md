---
name: cross-module-events
description: Publishing domain events between modules, listening to events with @TransactionalEventListener, sealed event class hierarchies, and event-driven communication. Use when features span multiple modules or need decoupled side effects.
---

# Cross-Module Events

## Overview

Modules communicate through domain events, not direct service calls. This keeps modules loosely coupled and enables features to react to state changes in other modules.

**Example:** When a namespace is created in the `namespace` module, the `audit` module listens and logs the creation. They don't know about each other directly.

---

## Domain Events (Sealed Classes)

Domain events are sealed class hierarchies representing state changes. Each module publishes events from its domain.

### Basic Event Hierarchy

```java
// namespace/NamespaceEvent.java
public abstract sealed class NamespaceEvent extends EntityEvent implements Supplier<Namespace>
        permits NamespaceEvent.Created, 
                NamespaceEvent.Renamed, 
                NamespaceEvent.Deleted,
                NamespaceEvent.StatusChanged {

    private final Namespace namespace;

    protected NamespaceEvent(Namespace namespace) {
        super(namespace.id());
        this.namespace = namespace;
    }

    @Override
    public Namespace get() { 
        return namespace; 
    }

    // ===== Created Event =====
    @DomainEvent(name = "created", namespace = "namespaces")
    public static final class Created extends NamespaceEvent {
        public Created(Namespace namespace) { 
            super(namespace); 
        }
    }

    // ===== Renamed Event =====
    @DomainEvent(name = "renamed", namespace = "namespaces")
    public static final class Renamed extends NamespaceEvent {
        private final Slug from;
        private final Slug to;

        public Renamed(Namespace namespace, Slug from, Slug to) {
            super(namespace);
            this.from = from;
            this.to = to;
        }

        public Slug from() { return from; }
        public Slug to() { return to; }
    }

    // ===== Deleted Event =====
    @DomainEvent(name = "deleted", namespace = "namespaces")
    public static final class Deleted extends NamespaceEvent {
        public Deleted(Namespace namespace) { 
            super(namespace); 
        }
    }

    // ===== Status Changed Event =====
    @DomainEvent(name = "statusChanged", namespace = "namespaces")
    public static final class StatusChanged extends NamespaceEvent {
        private final NamespaceStatus from;
        private final NamespaceStatus to;

        public StatusChanged(Namespace namespace, NamespaceStatus from, NamespaceStatus to) {
            super(namespace);
            this.from = from;
            this.to = to;
        }

        public NamespaceStatus from() { return from; }
        public NamespaceStatus to() { return to; }
    }
}
```

### Base Event Class

```java
// shared/EntityEvent.java
public abstract class EntityEvent implements ApplicationEvent {
    private final EntityId entityId;
    private final Instant timestamp;

    protected EntityEvent(EntityId entityId) {
        this.entityId = entityId;
        this.timestamp = Instant.now();
    }

    public EntityId getEntityId() {
        return entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
```

---

## Publishing Events

Publish events from the service implementation using `ApplicationEventPublisher`:

```java
// namespace/DefaultNamespaceManager.java
@Slf4j
@RequiredArgsConstructor
class DefaultNamespaceManager implements NamespaceManager {

    private final DSLContext context;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional(label = "namespace-create")
    public Namespace create(@NonNull NamespaceDefinition definition) {
        // Create namespace in database
        Namespace namespace = context.insertInto(NAMESPACES)
                .set(NAMESPACES.SLUG, definition.slug().get())
                .set(NAMESPACES.NAME, definition.name())
                .returning(NAMESPACES.fields())
                .fetchOne(DefaultNamespaceManager::toNamespace);

        Assert.state(namespace != null, "Failed to insert namespace");

        // ===== Publish Event =====
        publisher.publishEvent(new NamespaceEvent.Created(namespace));

        return namespace;
    }

    @Override
    @Transactional(label = "namespace-rename")
    public Namespace rename(@NonNull String slug, @NonNull String newName) {
        Namespace existing = findBySlug(slug)
                .orElseThrow(() -> new NamespaceNotFoundException(slug));

        // Update database
        Namespace updated = context.update(NAMESPACES)
                .set(NAMESPACES.NAME, newName)
                .set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
                .where(NAMESPACES.SLUG.eq(slug))
                .returning(NAMESPACES.fields())
                .fetchOne(DefaultNamespaceManager::toNamespace);

        Assert.state(updated != null, "Failed to update namespace");

        // ===== Publish Event with Details =====
        publisher.publishEvent(
                new NamespaceEvent.Renamed(updated, Slug.of(slug), Slug.of(newName))
        );

        return updated;
    }

    @Override
    @Transactional(label = "namespace-delete")
    public void delete(@NonNull String slug) {
        Namespace existing = findBySlug(slug)
                .orElseThrow(() -> new NamespaceNotFoundException(slug));

        context.deleteFrom(NAMESPACES)
                .where(NAMESPACES.SLUG.eq(slug))
                .execute();

        // ===== Publish Event =====
        publisher.publishEvent(new NamespaceEvent.Deleted(existing));
    }
}
```

---

## Listening to Events

Listen to events in other modules using `@TransactionalEventListener`:

### @TransactionalEventListener (Recommended)

Fires AFTER the publishing transaction commits. Safer for side effects:

```java
// audit/AuditEventListener.java
@Slf4j
@RequiredArgsConstructor
@Component
class AuditEventListener {

    private final AuditEventRepository auditEventRepository;

    @TransactionalEventListener(
            id = "audit.namespace-created",
            classes = NamespaceEvent.Created.class
    )
    public void on(NamespaceEvent.Created event) {
        log.info("Namespace created: {}", event.get().slug());

        AuditEvent auditEvent = AuditEvent.builder()
                .entityType("namespace")
                .entityId(event.getEntityId())
                .eventType("namespace.created")
                .actionType("CREATE")
                .timestamp(event.getTimestamp())
                .details(
                        Map.of(
                                "slug", event.get().slug(),
                                "name", event.get().name()
                        )
                )
                .build();

        auditEventRepository.insert(auditEvent);
    }

    @TransactionalEventListener(
            id = "audit.namespace-deleted",
            classes = NamespaceEvent.Deleted.class
    )
    public void on(NamespaceEvent.Deleted event) {
        log.info("Namespace deleted: {}", event.get().slug());

        AuditEvent auditEvent = AuditEvent.builder()
                .entityType("namespace")
                .entityId(event.getEntityId())
                .eventType("namespace.deleted")
                .actionType("DELETE")
                .timestamp(event.getTimestamp())
                .details(Map.of("slug", event.get().slug()))
                .build();

        auditEventRepository.insert(auditEvent);
    }
}
```

### @EventListener (Use When Not in Transaction)

Fires immediately, even if publishing transaction fails. Use for fire-and-forget operations:

```java
// notification/NotificationEventListener.java
@Slf4j
@RequiredArgsConstructor
@Component
class NotificationEventListener {

    private final Mailer mailer;

    // Use when you want to send email regardless of transaction
    @EventListener(classes = NamespaceEvent.Created.class)
    public void on(NamespaceEvent.Created event) {
        // This fires immediately, not after transaction commits
        Namespace ns = event.get();
        
        try {
            mailer.send(Mail.builder()
                    .to(ns.owner().email())
                    .subject("Namespace Created: " + ns.name())
                    .template("namespace-created")
                    .context(Map.of("namespace", ns))
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to send namespace notification", e);
            // Don't throw: failure to send email shouldn't fail the operation
        }
    }
}
```

---

## Event Listener Patterns

### Listen to Multiple Event Types

```java
@Component
class MetricsListener {

    @TransactionalEventListener
    public void on(NamespaceEvent.Created event) {
        metrics.recordCreated("namespace");
    }

    @TransactionalEventListener
    public void on(NamespaceEvent.Deleted event) {
        metrics.recordDeleted("namespace");
    }

    @TransactionalEventListener
    public void on(VaultEvent.Created event) {
        metrics.recordCreated("vault");
    }
}
```

### Conditional Listening

```java
@Component
class NotificationListener {

    @TransactionalEventListener(
            condition = "@featureFlags.isEnabled('send-notifications')"
    )
    public void on(NamespaceEvent.Created event) {
        // Only listen if feature flag is enabled
        sendNotification(event);
    }
}
```

### Async Event Processing

```java
@Component
class AsyncProcessingListener {

    private final TaskScheduler taskScheduler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NamespaceEvent.Created event) {
        // Schedule async task after transaction commits
        taskScheduler.schedule(
                () -> processNamespaceAsync(event),
                Instant.now().plusSeconds(5)
        );
    }

    private void processNamespaceAsync(NamespaceEvent.Created event) {
        // Heavy lifting happens here, outside transaction
        log.info("Processing namespace async: {}", event.get().slug());
    }
}
```

---

## Testing Domain Events

### Assert Event Published

```java
@Test
void shouldPublishNamespaceCreatedEvent(AssertablePublishedEvents events) {
    // Arrange
    NamespaceDefinition definition = NamespaceDefinition.builder()
            .owner(EntityId.from(1L))
            .slug("test")
            .name("Test")
            .build();

    // Act
    Namespace created = manager.create(definition);

    // Assert
    events.assertThat()
            .contains(NamespaceEvent.Created.class)
            .matching(event -> event.get().id().equals(created.id()));
}
```

### Assert Event Listener Triggered

```java
@Test
void shouldLogAuditEventWhenNamespaceCreated(
        AssertablePublishedEvents events,
        AuditEventRepository auditRepository) {

    // Arrange
    NamespaceDefinition definition = NamespaceDefinition.builder()
            .owner(EntityId.from(1L))
            .slug("test")
            .name("Test")
            .build();

    // Act
    Namespace created = manager.create(definition);

    // Assert - Event published
    events.assertThat()
            .contains(NamespaceEvent.Created.class);

    // Assert - Listener executed and audit record created
    List<AuditEvent> auditLogs = auditRepository.findByEntityId(created.id());
    assertThat(auditLogs)
            .isNotEmpty()
            .anyMatch(log -> log.getEventType().equals("namespace.created"));
}
```

### Test Event Listener in Isolation

```java
@Test
void shouldCreateAuditEventOnNamespaceCreated() {
    // Arrange
    Namespace namespace = Namespace.builder()
            .id(EntityId.from(1L))
            .slug("test")
            .name("Test")
            .build();

    NamespaceEvent.Created event = new NamespaceEvent.Created(namespace);

    // Act
    auditListener.on(event);

    // Assert
    then(auditRepository).should().insert(argThat(audit ->
            audit.getEventType().equals("namespace.created") &&
            audit.getEntityId().equals(EntityId.from(1L))
    ));
}
```

---

## Event Sourcing Considerations

If you later want event sourcing, design events now:

```java
// Make events self-contained (can be replayed)
@DomainEvent(name = "created", namespace = "namespaces")
public static final class Created extends NamespaceEvent {
    public Created(Namespace namespace) { 
        super(namespace); 
    }
    
    // Include all data needed to reconstruct state
    // Don't rely on external services to fill in details
}

// Snapshot structure if needed later
public record NamespaceSnapshot(
        Namespace namespace,
        OffsetDateTime snapshotTime,
        List<NamespaceEvent> eventsSinceSnapshot
) { }
```

---

## Event Ordering & Guarantees

### Ordering Within Same Module

Events published in the same transaction execute in order:

```java
@Transactional
public void renameAndArchive(String slug, String newName) {
    // These happen in order
    manager.rename(slug, newName);  // Fires Renamed event
    manager.archive(slug);          // Fires Archived event
}
```

### Cross-Module Ordering

No guaranteed order between events from different modules. Design defensively:

```java
// ✓ Good: Listeners can handle out-of-order events
@TransactionalEventListener
public void on(NamespaceEvent.Created event) {
    if (already exists) {
        log.debug("Already processed this event");
        return;
    }
    process(event);
}

// ✗ Bad: Assumes specific order
@TransactionalEventListener
public void on(NamespaceEvent.Deleted event) {
    // Assumes Renamed event fired before this
    // Not guaranteed!
}
```

---

## Common Patterns

### Saga Pattern (Multi-Step Process)

```java
// vault/VaultCreatedSaga.java
@Component
class VaultCreatedSaga {

    @TransactionalEventListener
    public void on(VaultEvent.Created event) {
        Vault vault = event.get();
        
        // Step 1: Initialize vault
        initializeVault(vault);
        
        // Step 2: Create default profile
        publisher.publishEvent(new VaultEvent.DefaultProfileCreated(vault));
    }
}

// audit/VaultAuditListener.java
@Component
class VaultAuditListener {

    @TransactionalEventListener
    public void on(VaultEvent.DefaultProfileCreated event) {
        // Reacts to the saga step
        logAuditEvent("vault.default-profile-created", event.get());
    }
}
```

### Dead Letter Queue Pattern

```java
@Component
class ReliableEventListener {

    private final DeadLetterQueue deadLetterQueue;

    @TransactionalEventListener
    public void on(NamespaceEvent.Created event) {
        try {
            processNamespace(event);
        } catch (Exception e) {
            log.error("Failed to process event", e);
            deadLetterQueue.enqueue(event);  // Retry later
        }
    }
}
```

---

## Common Mistakes

### ❌ Direct Service Call Instead of Event

```java
// ✗ Wrong: Tight coupling
class NamespaceManager {
    private final AuditLogger auditLogger;
    
    public Namespace create(NamespaceDefinition def) {
        Namespace ns = // ... create
        auditLogger.log("Created", ns);  // Direct call!
        return ns;
    }
}

// ✓ Correct: Event-driven
class NamespaceManager {
    private final ApplicationEventPublisher publisher;
    
    public Namespace create(NamespaceDefinition def) {
        Namespace ns = // ... create
        publisher.publishEvent(new NamespaceEvent.Created(ns));
        return ns;
    }
}
```

### ❌ Using @EventListener When Transaction Needed

```java
// ✗ Wrong: Event fires even if transaction rolls back
@EventListener
public void on(NamespaceEvent.Created event) {
    database.insert(...);  // May not persist if tx rolls back
}

// ✓ Correct: Ensure event fires only after commit
@TransactionalEventListener
public void on(NamespaceEvent.Created event) {
    database.insert(...);  // Guaranteed to run after commit
}
```

### ❌ Circular Event Publishing

```java
// ✗ Wrong: A publishes event that B listens to, B publishes event that A listens to
class AListener {
    @TransactionalEventListener
    public void on(BEvent.Created event) {
        publisher.publishEvent(new AEvent.Created(...));  // Cycle!
    }
}

class BListener {
    @TransactionalEventListener
    public void on(AEvent.Created event) {
        publisher.publishEvent(new BEvent.Created(...));  // Cycle!
    }
}

// ✓ Correct: Avoid cycles, use saga pattern for multi-step processes
```

---

## Verification Checklist

- [ ] All state changes publish events
- [ ] Events are sealed classes with specific subtypes
- [ ] Events include all data needed by listeners
- [ ] Listeners use `@TransactionalEventListener` (not `@EventListener`)
- [ ] Listeners are in the listening module, not publishing module
- [ ] No circular event dependencies
- [ ] Event listeners tested in isolation
- [ ] Events tested with integration tests
- [ ] Error handling in listeners (don't let listener failure fail main operation)
- [ ] No direct service calls between modules (events only)
- [ ] Listeners are idempotent (can handle same event twice)

---

## When to Ask for Help

- "Should this be an event or a direct service call?"
- "What data should this event include?"
- "How do I handle event listener failures?"
- "Is there a circular dependency in my events?"
- "Should I use @EventListener or @TransactionalEventListener?"
- "How do I test this event-driven feature?"
