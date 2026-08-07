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

### Behavior Methods on Entities

Records can have behavior methods, not just getters. Real example from `com.konfigyr.membership.Member` —
note it's an `@Entity`, not an `@AggregateRoot`: it references its owning `Namespace` aggregate by
`EntityId` rather than embedding it (see "Aggregate Boundaries" below):

```java
@Entity
public record Member(
        @NonNull @Identity EntityId id,
        @NonNull EntityId namespace,
        @NonNull EntityId account,
        @NonNull NamespaceRole role,
        @NonNull String email,
        @Nullable FullName fullName,
        @NonNull Avatar avatar,
        @Nullable OffsetDateTime since
) implements Serializable {

    // Behavior: prefer the full name, fall back to email when the account has none set
    @NonNull
    public String displayName() {
        return fullName == null ? email : fullName.get();
    }

    // Behavior: check membership without a separate repository lookup
    public boolean isMemberOf(@NonNull Namespace namespace) {
        return this.namespace.equals(namespace.id());
    }

    // ... firstName()/lastName() and Builder omitted, see com.konfigyr.membership.Member
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

This is exactly what `com.konfigyr.namespace.NamespaceDefinition` is — the real command type accepted by
`NamespaceManager.create(@NonNull NamespaceDefinition definition)`:

```java
@ValueObject
public record NamespaceDefinition(
        @NonNull EntityId owner,
        @NonNull Slug slug,
        @NonNull String name,
        @Nullable String description
) implements Serializable {

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

        // ... build() with Assert.notNull/hasText validation omitted, see NamespaceDefinition.Builder
    }
}

// Usage in service (see DefaultNamespaceManager.create() for the real, fuller implementation —
// it also inserts the initial administrator Member and publishes NamespaceEvent.Created)
@Override
public Namespace create(NamespaceDefinition definition) {
    // definition is already validated by its Builder
    Namespace namespace = repository.insert(definition);
    return namespace;
}
```

---

## Enums as Value Objects

Not every enum needs behavior — `com.konfigyr.namespace.NamespaceRole` is a plain two-value enum
(`ADMIN`, `USER`) with no methods at all, and that's fine when there's no per-value logic to encapsulate.

When an enum does carry per-value data and behavior, real example from
`com.konfigyr.security.NamespaceClientType`:

```java
@ValueObject
public enum NamespaceClientType {

    SERVICE_ACCOUNT((byte) 0x01, "Service Account"),
    AGENT((byte) 0x02, "AI Agent"),
    WORKLOAD((byte) 0x03, "Workload Identity");

    private final byte code;
    private final String displayName;

    NamespaceClientType(byte code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean requiresSecret() {
        return this == SERVICE_ACCOUNT;
    }

    // ... code()/of(byte) omitted, see com.konfigyr.security.NamespaceClientType
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

This is a real, current example — `Member` isn't just a separate aggregate, it lives in an entirely
separate module (`com.konfigyr.membership`) from `Namespace` (`com.konfigyr.namespace`), referencing it
purely by `EntityId`:

```java
// Aggregate: Namespace (module: com.konfigyr.namespace)
// Root entity: Namespace
// Child entities: None (members live in a separate module's aggregate)
@AggregateRoot
public record Namespace(...) { }

// Entity: Member (module: com.konfigyr.membership)
// References: namespace (EntityId, not an object reference)
@Entity
public record Member(
        @NonNull @Identity EntityId id,
        @NonNull EntityId namespace,   // Reference, not embedded object — different module even
        @NonNull EntityId account,
        @NonNull NamespaceRole role
        // ... email, fullName, avatar, since omitted, see com.konfigyr.membership.Member
) { }
```

### Bad Aggregate Design

```java
// ✗ Don't do this: Loading entire tree of objects
@AggregateRoot
public record Namespace(
        EntityId id,
        String slug,
        List<Member> members,               // ❌ Too much coupling (and crosses a module boundary)
        List<Vault> vaults,                  // ❌ Too many responsibilities
        List<AuditLog> auditLogs            // ❌ Should be separate aggregate
) { }

// ✓ Do this: Keep aggregates focused
@AggregateRoot
public record Namespace(
        EntityId id,
        String slug
) { }

// Separate entity, separate module
@Entity
public record Member(
        EntityId id,
        EntityId namespace,  // Reference only
        EntityId account,
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

Note: `Namespace` itself has no `owner` field — ownership is expressed by the initial `Member` row with
`NamespaceRole.ADMIN`, created alongside the namespace (see `DefaultNamespaceManager.create()`), not as
namespace state. Don't invent an `owner()` accessor on an aggregate that doesn't have one.

```java
@Component
class NamespaceValidator {
    public void validateUniqueSlug(Slug slug, NamespaceRepository repo) {
        if (repo.existsBySlug(slug)) {
            throw new NamespaceExistsException("Slug already exists: " + slug);
        }
    }
}

// Usage in service
@Override
public Namespace create(NamespaceDefinition definition) {
    validator.validateUniqueSlug(definition.slug(), repository);

    Namespace namespace = Namespace.builder()
            .id(EntityId.generate())
            .slug(definition.slug().get())
            .name(definition.name())
            .description(definition.description())
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
