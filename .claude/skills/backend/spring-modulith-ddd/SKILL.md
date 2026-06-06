---
name: spring-modulith-ddd
description: Creating new modules, defining domain objects (aggregates, value objects), service interfaces, and wiring beans via AutoConfiguration. Use when designing a new feature or refactoring existing modules.
---

# Spring Modulith & Domain-Driven Design

## Core Concepts

**Module** — A logical boundary representing a business domain (namespace, vault, audit, kms). Modules communicate via events, never direct dependencies.

**Aggregate** — A cluster of domain objects (entities, value objects) with a root entity that guarantees invariants. In Konfigyr, aggregates are immutable `record` types with inner `Builder` classes.

**Value Object** — An immutable domain concept with no identity (Email, Slug, UserId). Also records.

**Service Interface** — Public contract for a module's operations (e.g., `NamespaceManager`). Annotated with `@Repository` (jmolecules).

**Default Implementation** — Package-private class holding jOOQ `DSLContext`. Named `Default<InterfaceName>`. Never exposed outside the module.

---

## Creating a New Module

### Step 1: Plan the Module Structure

Before any code, answer:
- What is the business domain this module represents?
- What aggregates live in this module? (usually 1-3 per module)
- Which other modules does it depend on (event-wise)?
- What are the primary use cases (CRUD, business logic)?

**Module directory structure:**
```
src/main/java/com/konfigyr/<module>/
├── <Aggregate>.java                           # Aggregate root (record + Builder)
├── <ValueObject>.java                         # Value objects (records)
├── <Module>Exception.java                     # Base domain exception
├── <Specific>Exception.java                   # Specific exceptions
├── <Module>Manager.java                       # Service interface (@Repository)
├── Default<Module>Manager.java                # jOOQ implementation (package-private)
├── <Module>Event.java                         # Domain events (sealed class)
├── <Module>AutoConfiguration.java             # Spring beans wiring
└── controller/
    ├── <Aggregate>Controller.java             # REST controller (package-private)
    └── Assemblers.java                        # RepresentationModelAssembler factories
```

### Step 2: Define Domain Objects

**Aggregate Root (record with Builder):**
```java
// com.konfigyr.namespace.Namespace
@AggregateRoot
public record Namespace(
                @NonNull @Identity EntityId id,
                @NonNull String slug,
                @NonNull String name,
                @Nullable String description,
                @NonNull Avatar avatar,
                @Nullable OffsetDateTime createdAt,
                @Nullable OffsetDateTime updatedAt
        ) implements Serializable {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private EntityId id;
        private String slug;
        private String name;
        private String description;
        private Avatar avatar;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(EntityId id) { this.id = id; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder name(String name) { this.name = name; return this; }
        // ... other setters

        public Namespace build() {
            Assert.notNull(id, "Namespace entity identifier can not be null");
            Assert.hasText(slug, "Namespace slug can not be blank");
            Assert.hasText(name, "Namespace name can not be blank");
            // Validate invariants here, throw exceptions if invalid
            return new Namespace(id, slug, name, description, avatar, createdAt, updatedAt);
        }
    }
}
```

**Value Objects (immutable records):**
```java
@ValueObject
public record NamespaceDefinition(
        @NonNull EntityId owner,
        @NonNull Slug slug,
        @NonNull String name,
        @Nullable String description
) implements Serializable {
    public NamespaceDefinition {
        Assert.notNull(owner, "Owner is required");
        Assert.notNull(slug, "Slug is required");
        Assert.hasText(name, "Name is required");
    }
}
```

**Domain Exceptions:**
```java
public abstract class NamespaceException extends RuntimeException {
    public NamespaceException(String message) { super(message); }
    public NamespaceException(String message, Throwable cause) { super(message, cause); }
}

public final class NamespaceNotFoundException extends NamespaceException {
    public NamespaceNotFoundException(String slug) {
        super("Namespace not found: " + slug);
    }
}

public final class NamespaceExistsException extends NamespaceException {
    public NamespaceExistsException(String slug) {
        super("Namespace already exists: " + slug);
    }
}
```

### Step 3: Define Service Interface

Service interfaces are public, use `@Repository` annotation (from jmolecules), and define CRUD + business logic methods.

```java
// com.konfigyr.namespace.NamespaceManager
@NullMarked
@Repository
public interface NamespaceManager {

    Page<Namespace> search(SearchQuery query);

    Optional<Namespace> findById(EntityId id);

    Optional<Namespace> findBySlug(String slug);

    boolean exists(String slug);

    @DomainEventPublisher(publishes = "namespaces.created")
    Namespace create(NamespaceDefinition definition);

    @DomainEventPublisher(publishes = "namespaces.renamed")
    Namespace update(String slug, NamespaceDefinition definition);

    void delete(String slug);
}
```

### Step 4: Implement Service with jOOQ

Implementation is package-private (`Default<Name>`), holds `DSLContext`, and uses jOOQ directly. Never expose `DSLContext` outside this class.

```java
// com.konfigyr.namespace.DefaultNamespaceManager
@Slf4j
@RequiredArgsConstructor
class DefaultNamespaceManager implements NamespaceManager {

    private final DSLContext context;
    private final ApplicationEventPublisher publisher;

    @NonNull
    @Override
    @Transactional(readOnly = true, label = "namespace-slug-lookup")
    public Optional<Namespace> findBySlug(@NonNull String slug) {
        return fetch(NAMESPACES.SLUG.eq(slug));
    }

    @NonNull
    @Override
    @Transactional(label = "namespace-create")
    public Namespace create(@NonNull NamespaceDefinition definition) {
        final Namespace namespace = context.insertInto(NAMESPACES)
                .set(NAMESPACES.ID, EntityId.generate().map(EntityId::get))
                .set(NAMESPACES.SLUG, definition.slug().get())
                .set(NAMESPACES.NAME, definition.name())
                .set(NAMESPACES.CREATED_AT, OffsetDateTime.now())
                .set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
                .returning(NAMESPACES.fields())
                .fetchOne(DefaultNamespaceManager::toNamespace);

        Assert.state(namespace != null, () -> "Could not create namespace from: " + definition);

        publisher.publishEvent(new NamespaceEvent.Created(namespace));
        return namespace;
    }

    private Optional<Namespace> fetch(Condition condition) {
        return context.selectFrom(NAMESPACES)
                .where(condition)
                .fetchOptional(DefaultNamespaceManager::toNamespace);
    }

    private static Namespace toNamespace(Record record) {
        return Namespace.builder()
                .id(EntityId.from(record.get(NAMESPACES.ID)))
                .slug(record.get(NAMESPACES.SLUG))
                .name(record.get(NAMESPACES.NAME))
                .description(record.get(NAMESPACES.DESCRIPTION))
                .createdAt(record.get(NAMESPACES.CREATED_AT))
                .updatedAt(record.get(NAMESPACES.UPDATED_AT))
                .build();
    }
}
```

**Key patterns:**
- Private static mapper method (`toNamespace`) converts jOOQ Record to domain object
- All transactions labeled (`@Transactional(label = "...")`
- `DSLContext` used only inside this class
- Domain events published via `ApplicationEventPublisher`

### Step 5: Define Domain Events

Domain events are sealed class hierarchies. Publish them when state changes. Other modules listen via `@TransactionalEventListener`.

```java
// com.konfigyr.namespace.NamespaceEvent
public abstract sealed class NamespaceEvent extends EntityEvent implements Supplier<Namespace>
        permits NamespaceEvent.Created, NamespaceEvent.Renamed, NamespaceEvent.Deleted {

    private final Namespace namespace;

    protected NamespaceEvent(Namespace namespace) {
        super(namespace.id());
        this.namespace = namespace;
    }

    @Override
    public Namespace get() { return namespace; }

    @DomainEvent(name = "created", namespace = "namespaces")
    public static final class Created extends NamespaceEvent {
        public Created(Namespace namespace) { super(namespace); }
    }

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

    @DomainEvent(name = "deleted", namespace = "namespaces")
    public static final class Deleted extends NamespaceEvent {
        public Deleted(Namespace namespace) { super(namespace); }
    }
}
```

### Step 6: Wire Beans via AutoConfiguration

Each module has an `@AutoConfiguration` class that uses `@ConditionalOnMissingBean` for testability.

```java
// com.konfigyr.namespace.NamespaceManagementAutoConfiguration
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class NamespaceManagementAutoConfiguration {

    private final DSLContext context;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Bean
    @ConditionalOnMissingBean(NamespaceManager.class)
    NamespaceManager defaultNamespaceManager() {
        return new DefaultNamespaceManager(context, applicationEventPublisher);
    }
}
```

### Step 7: Create REST Controller

Controllers are in a `controller/` subpackage, package-private, thin, and delegate to the service interface.

```java
// com.konfigyr.namespace.controller.NamespaceController
@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces")
class NamespaceController {

    private final NamespaceManager namespaces;

    @GetMapping("/{slug}")
    @RequiresScope(OAuthScope.READ_NAMESPACES)
    EntityModel<Namespace> get(@PathVariable String slug) {
        return namespaces.findBySlug(slug)
                .map(Assemblers.namespace()::toModel)
                .orElseThrow(() -> new NamespaceNotFoundException(slug));
    }

    @PostMapping
    @RequiresScope(OAuthScope.WRITE_NAMESPACES)
    ResponseEntity<EntityModel<Namespace>> create(@Valid @RequestBody NamespaceDefinition definition) {
        final Namespace namespace = namespaces.create(definition);
        return ResponseEntity.created(URI.create("/namespaces/" + namespace.slug()))
                .body(Assemblers.namespace().toModel(namespace));
    }
}
```

---

## Common Patterns

### N-to-1 Relationships in Queries

Use EXISTS subqueries instead of joins when filtering by related data:

```java
// Don't do N+1:
List<Namespace> all = context.selectFrom(NAMESPACES).fetch();
all.forEach(ns -> ns.members = context.selectFrom(NAMESPACE_MEMBERS)
        .where(NAMESPACE_MEMBERS.NAMESPACE_ID.eq(ns.id()))
        .fetch());

// Do this instead:
List<Namespace> members = context.selectFrom(NAMESPACES)
        .where(DSL.exists(
                DSL.select(NAMESPACE_MEMBERS.ID)
                        .from(NAMESPACE_MEMBERS)
                        .where(DSL.and(
                                NAMESPACE_MEMBERS.NAMESPACE_ID.eq(NAMESPACES.ID),
                                NAMESPACE_MEMBERS.ROLE.eq("ADMIN")
                        ))
        ))
        .fetch(DefaultNamespaceManager::toNamespace);
```

### Pagination with DSL

```java
Page<Namespace> results = context.selectFrom(NAMESPACES)
        .where(conditions)
        .orderBy(NAMESPACES.CREATED_AT.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch(DefaultNamespaceManager::toNamespace);

long total = context.fetchCount(
        DSL.selectFrom(NAMESPACES).where(conditions)
);

return new PageImpl<>(results, pageable, total);
```

---

## Verification Checklist

- [ ] Aggregate is immutable `record` with inner `Builder`
- [ ] Builder validates invariants in `build()` method
- [ ] Service interface is public with `@Repository`
- [ ] Implementation is package-private, named `Default<Name>`
- [ ] `DSLContext` only used inside `Default<Name>`, never leaked
- [ ] Domain events published via `ApplicationEventPublisher`
- [ ] AutoConfiguration uses `@ConditionalOnMissingBean`
- [ ] Controller is package-private in `controller/` subpackage
- [ ] No Spring annotations on domain objects
- [ ] No jOOQ types in domain layer
- [ ] All transactions labeled
- [ ] Mapper methods are private static

---

## When to Ask for Help

- "Should this be a new aggregate or part of an existing one?"
- "How do I model this relationship without creating circular dependencies?"
- "Is this service interface too broad or too narrow?"
- "Do I need to publish an event here?"
