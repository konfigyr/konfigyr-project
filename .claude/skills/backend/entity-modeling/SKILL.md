---
name: entity-modeling
description: Designing and implementing domain objects as immutable records, creating value objects, building aggregates with validation, and understanding aggregate boundaries. Use when designing new domain features or refactoring domain models.
---

# Entity Modeling & Domain Objects

## Core Principles

**Immutability** — All domain objects are immutable. Use `record` types, never getters/setters.

**Validation in Constructor** — Invariants are validated when objects are built, not later.

**No Spring Annotations** — Domain objects don't know about Spring, jOOQ, or HTTP.

**Builder Pattern** — Complex aggregates use inner `Builder` classes for construction.

**Value Objects** — Represent domain concepts with no identity (Email, Slug, Scope). Also records.

---

## Aggregate Root

An aggregate root is the entry point to a cluster of related objects. It enforces invariants on the whole aggregate.

### Aggregate Root Record

```java
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

        // Setters
        public Builder id(EntityId id) { this.id = id; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder avatar(Avatar avatar) { this.avatar = avatar; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        // Build with validation
        public Namespace build() {
            Assert.notNull(id, "Namespace entity identifier can not be null");
            Assert.hasText(slug, "Namespace slug can not be blank");
            Assert.hasText(name, "Namespace name can not be blank");
            
            // Validate invariants
            if (!isValidSlug(slug)) {
                throw new IllegalArgumentException("Invalid slug format: " + slug);
            }
            
            return new Namespace(id, slug, name, description, avatar, createdAt, updatedAt);
        }

        private boolean isValidSlug(String slug) {
            return slug.matches("^[a-z0-9-]+$") && slug.length() > 0 && slug.length() <= 255;
        }
    }
}
```

### Aggregate Root with Behavior

Aggregates can have behavior methods (not just getters):

```java
@AggregateRoot
public record Namespace(
        @NonNull EntityId id,
        @NonNull String slug,
        @NonNull String name,
        @NonNull NamespaceStatus status
) implements Serializable {

    public static Builder builder() { return new Builder(); }

    // Behavior: transition state
    public NamespaceEvent.StatusChanged changeStatus(NamespaceStatus newStatus) {
        if (status == newStatus) {
            throw new InvalidStateTransitionException("Already in " + status);
        }
        
        if (status == ARCHIVED && newStatus != ACTIVE) {
            throw new InvalidStateTransitionException("Cannot change status of archived namespace");
        }

        return new NamespaceEvent.StatusChanged(this, status, newStatus);
    }

    // Behavior: check if namespace is active
    public boolean isActive() {
        return status == NamespaceStatus.ACTIVE;
    }

    // ... rest of builder omitted
}
```

---

## Value Objects

Value objects represent domain concepts without identity. They're compared by value, not identity.

### Simple Value Objects (Single Property)

```java
@ValueObject
public record Slug(String value) implements Serializable {
    
    public Slug {
        Assert.hasText(value, "Slug cannot be blank");
        if (!value.matches("^[a-z0-9-]+$")) {
            throw new IllegalArgumentException("Invalid slug format: " + value);
        }
    }

    public String get() {
        return value;
    }
}

@ValueObject
public record Email(String value) implements Serializable {
    
    public Email {
        Assert.hasText(value, "Email cannot be blank");
        if (!isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}

@ValueObject
public record UserId(Long value) implements Serializable {
    
    public UserId {
        Assert.notNull(value, "UserId cannot be null");
        Assert.state(value > 0, "UserId must be positive");
    }

    public Long get() {
        return value;
    }

    public static UserId from(Long id) {
        return new UserId(id);
    }
}
```

### Complex Value Objects (Multiple Properties)

```java
@ValueObject
public record Avatar(
        @NonNull String url,
        @NonNull String format,
        Long size
) implements Serializable {

    public Avatar {
        Assert.hasText(url, "Avatar URL cannot be blank");
        Assert.hasText(format, "Avatar format cannot be blank");
        
        if (!isValidUrl(url)) {
            throw new IllegalArgumentException("Invalid avatar URL: " + url);
        }
        
        if (!isValidFormat(format)) {
            throw new IllegalArgumentException("Invalid avatar format: " + format);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String url;
        private String format;
        private Long size;

        public Builder url(String url) { this.url = url; return this; }
        public Builder format(String format) { this.format = format; return this; }
        public Builder size(Long size) { this.size = size; return this; }

        public Avatar build() {
            Assert.hasText(url, "URL is required");
            Assert.hasText(format, "Format is required");
            return new Avatar(url, format, size);
        }
    }

    private static boolean isValidUrl(String url) {
        return url.startsWith("https://") || url.startsWith("http://");
    }

    private static boolean isValidFormat(String format) {
        return format.matches("^(png|jpg|jpeg|gif)$");
    }
}
```

---

## Domain Commands (Value Objects as Input)

Commands represent user intent. They're typically value objects passed to service methods.

```java
@ValueObject
public record CreateNamespaceCommand(
        @NonNull EntityId owner,
        @NonNull Slug slug,
        @NonNull String name,
        @Nullable String description
) implements Serializable {

    public CreateNamespaceCommand {
        Assert.notNull(owner, "Owner is required");
        Assert.notNull(slug, "Slug is required");
        Assert.hasText(name, "Name is required");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private EntityId owner;
        private Slug slug;
        private String name;
        private String description;

        public Builder owner(EntityId owner) { this.owner = owner; return this; }
        public Builder slug(Slug slug) { this.slug = slug; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public CreateNamespaceCommand build() {
            return new CreateNamespaceCommand(owner, slug, name, description);
        }
    }
}

// Usage in service
@Override
public Namespace create(CreateNamespaceCommand command) {
    // command is already validated by constructor
    Namespace namespace = new Namespace(
            EntityId.generate(),
            command.slug(),
            command.name(),
            command.description(),
            Avatar.empty(),
            OffsetDateTime.now(),
            OffsetDateTime.now()
    );
    return repository.save(namespace);
}
```

---

## Enums as Value Objects

```java
@ValueObject
public enum NamespaceRole {
    OWNER("Namespace owner with full access"),
    ADMIN("Administrator with most permissions"),
    MEMBER("Regular member with read access"),
    VIEWER("Read-only access");

    private final String description;

    NamespaceRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canDelete() {
        return this == OWNER;
    }

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }
}
```

---

## EntityId (Generic Identifier)

A wrapper for entity identifiers:

```java
@ValueObject
public record EntityId(Long value) implements Serializable {

    public EntityId {
        Assert.notNull(value, "EntityId cannot be null");
    }

    public Long get() {
        return value;
    }

    public static EntityId from(Long id) {
        return new EntityId(id);
    }

    public static EntityId generate() {
        // Generate a new unique ID (UUID → Long, snowflake, etc.)
        return new EntityId(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

---

## Aggregate Boundaries

Aggregates should be:
- **Small**: 1-3 root entities per aggregate
- **Focused**: Represent a single business concept
- **Bounded**: Clear boundaries with other aggregates

### Good Aggregate Design

```java
// Aggregate: Namespace
// Root entity: Namespace
// Child entities: None (members are in separate aggregate)
@AggregateRoot
public record Namespace(...) { }

// Aggregate: NamespaceMember
// Root entity: NamespaceMember
// References: namespace_id (foreign key, not object reference)
@AggregateRoot
public record NamespaceMember(
        @NonNull EntityId id,
        @NonNull EntityId namespaceId,  // Reference, not embedded object
        @NonNull EntityId accountId,
        @NonNull NamespaceRole role
) { }
```

### Bad Aggregate Design

```java
// ✗ Don't do this: Loading entire tree of objects
@AggregateRoot
public record Namespace(
        EntityId id,
        String slug,
        List<NamespaceMember> members,      // ❌ Too much coupling
        List<Vault> vaults,                  // ❌ Too many responsibilities
        List<AuditLog> auditLogs            // ❌ Should be separate aggregate
) { }

// ✓ Do this: Keep aggregates focused
@AggregateRoot
public record Namespace(
        EntityId id,
        String slug
) { }

// Separate aggregate
@AggregateRoot
public record NamespaceMember(
        EntityId id,
        EntityId namespaceId,  // Reference only
        EntityId accountId,
        NamespaceRole role
) { }
```

---

## Validation Strategy

### Constructor-Level Validation

```java
public record Email(String value) {
    public Email {
        Assert.hasText(value, "Email cannot be blank");
        if (!isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}
```

### Builder-Level Validation

```java
public static final class Builder {
    public Namespace build() {
        Assert.notNull(id, "ID required");
        Assert.hasText(slug, "Slug required");
        Assert.hasText(name, "Name required");
        
        // Cross-field validation
        if (name.length() > 255) {
            throw new IllegalArgumentException("Name too long");
        }
        
        return new Namespace(id, slug, name, ...);
    }
}
```

### Domain Service Validation

```java
@Component
class NamespaceValidator {
    public void validateUniqueSlug(Slug slug, NamespaceRepository repo) {
        if (repo.existsBySlug(slug)) {
            throw new NamespaceExistsException("Slug already exists: " + slug);
        }
    }

    public void validateOwnerAccess(EntityId userId, Namespace namespace) {
        if (!namespace.owner().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}

// Usage in service
@Override
public Namespace create(CreateNamespaceCommand command) {
    validator.validateUniqueSlug(command.slug(), repository);
    
    Namespace namespace = Namespace.builder()
            .id(EntityId.generate())
            .slug(command.slug().get())
            .name(command.name())
            .owner(command.owner())
            .build();
    
    return repository.save(namespace);
}
```

---

## Comparison and Equality

Records automatically provide `equals()` and `hashCode()` based on fields:

```java
Email email1 = new Email("user@example.com");
Email email2 = new Email("user@example.com");

assertThat(email1).isEqualTo(email2);  // ✓ True (same value)
assertThat(email1).isSameAs(email2);   // ✗ False (different objects)
```

Use this for assertions:

```java
@Test
void shouldCreateEmailValueObject() {
    Email email = new Email("user@example.com");
    Email other = new Email("user@example.com");
    
    assertThat(email).isEqualTo(other);  // Records compare by value
}

@Test
void shouldRejectInvalidEmail() {
    assertThatThrownBy(() -> new Email("invalid"))
            .isInstanceOf(IllegalArgumentException.class);
}
```

---

## Serialization

Records are automatically `Serializable` if all fields are serializable:

```java
@AggregateRoot
public record Namespace(
        EntityId id,           // Serializable if EntityId is
        String slug,           // ✓ String is Serializable
        OffsetDateTime createdAt  // ✓ OffsetDateTime is Serializable
) implements Serializable {
    // Automatically serializable
}
```

If you need custom serialization:

```java
@AggregateRoot
public record Namespace(...) implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Serial
    private Object writeReplace() {
        // Custom serialization logic
    }

    @Serial
    private void readObject(ObjectInputStream stream) {
        // Custom deserialization logic
    }
}
```

---

## Verification Checklist

- [ ] All domain objects are `record` types (immutable)
- [ ] No `@Setter` or getter methods on aggregates
- [ ] Validation happens in constructor or `build()`
- [ ] No Spring or jOOQ annotations on domain objects
- [ ] Aggregate roots have `@AggregateRoot` annotation
- [ ] Value objects have `@ValueObject` annotation
- [ ] Builder classes have proper fluent API (`return this`)
- [ ] Cross-field validation in `build()` method
- [ ] Aggregates are small and focused
- [ ] Foreign keys are EntityId values, not object references
- [ ] Invariants enforced (nullability, ranges, formats)
- [ ] Exceptions thrown for invalid state (not validation warnings)

---

## When to Ask for Help

- "Is this a separate aggregate or part of the same one?"
- "Should this be a value object or an aggregate?"
- "How do I model this one-to-many relationship?"
- "What should be validated where (constructor vs builder vs service)?"
- "Is this aggregate too large?"
- "How do I prevent invalid state transitions?"
