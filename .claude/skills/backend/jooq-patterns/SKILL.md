---
name: jooq-queries
description: Writing database queries with jOOQ, understanding generated table/column references, common query patterns, and when to run ./gradlew generateJooq. Use when working with database access or understanding how generated code works.
---

# jOOQ Queries

## Generated Code Structure

After running `./gradlew generateJooq`, jOOQ generates classes in `build/generated-sources/jooq/`:

```
build/generated-sources/jooq/com/konfigyr/data/
├── Tables.java              # All table references (NAMESPACES, NAMESPACE_MEMBERS, ACCOUNTS, etc.)
├── Records.java             # Record types for each table
├── Fields.java              # Column field references
└── tables/
    ├── Namespaces.java      # NAMESPACES table class
    ├── NamespaceMembers.java
    └── ...
```

**Always import table references statically in service implementations:**

```java
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
```

---

## Common Query Patterns

### SELECT (Simple)

```java
// Fetch all records
List<Record> all = dsl.selectFrom(NAMESPACES).fetch();

// Fetch one record by ID
Optional<Namespace> namespace = context.selectFrom(NAMESPACES)
        .where(NAMESPACES.ID.eq(id))
        .fetchOptional(DefaultNamespaceManager::toNamespace);

// Fetch with WHERE condition
List<Namespace> active = context.selectFrom(NAMESPACES)
        .where(NAMESPACES.STATUS.eq("ACTIVE"))
        .fetch(DefaultNamespaceManager::toNamespace);
```

### SELECT with Filtering

```java
// Build dynamic conditions
List<Condition> conditions = new ArrayList<>();

if (searchQuery.hasSlug()) {
        conditions.add(NAMESPACES.SLUG.likeIgnoreCase("%" + searchQuery.slug() + "%"));
        }
        if (searchQuery.hasOwner()) {
        conditions.add(NAMESPACES.OWNER_ID.eq(searchQuery.owner().get()));
        }

List<Namespace> results = context.selectFrom(NAMESPACES)
        .where(DSL.and(conditions))
        .fetch(DefaultNamespaceManager::toNamespace);
```

### SELECT with JOIN

```java
// Join to filter or fetch related data
List<Namespace> activeWithMembers = context.select(NAMESPACES.fields())
                .from(NAMESPACES)
                .join(NAMESPACE_MEMBERS).on(NAMESPACES.ID.eq(NAMESPACE_MEMBERS.NAMESPACE_ID))
                .where(DSL.and(
                        NAMESPACES.STATUS.eq("ACTIVE"),
                        NAMESPACE_MEMBERS.ROLE.eq("ADMIN")
                ))
                .fetch(DefaultNamespaceManager::toNamespace);
```

### SELECT with EXISTS Subquery (Safer for N+1 Prevention)

Instead of fetching related entities in a loop, use EXISTS to filter:

```java
// "Find all namespaces that have at least one admin member"
List<Namespace> withAdmins = context.selectFrom(NAMESPACES)
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

### SELECT with Ordering & Pagination

```java
Page<Namespace> page = context.selectFrom(NAMESPACES)
        .where(conditions)
        .orderBy(NAMESPACES.CREATED_AT.desc(), NAMESPACES.NAME.asc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch(DefaultNamespaceManager::toNamespace);

long total = context.fetchCount(
        DSL.selectFrom(NAMESPACES).where(conditions)
);

return new PageImpl<>(page, pageable, total);
```

### SELECT with COUNT

```java
long count = context.selectCount()
        .from(NAMESPACES)
        .where(NAMESPACES.STATUS.eq("ACTIVE"))
        .fetchOne(Record1::value1);

boolean exists = context.fetchExists(
        DSL.selectFrom(NAMESPACES).where(NAMESPACES.SLUG.eq("konfigyr"))
);
```

### INSERT

```java
Namespace created = context.insertInto(NAMESPACES)
        .set(NAMESPACES.ID, EntityId.generate().map(EntityId::get))
        .set(NAMESPACES.SLUG, definition.slug().get())
        .set(NAMESPACES.NAME, definition.name())
        .set(NAMESPACES.CREATED_AT, OffsetDateTime.now())
        .set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
        .returning(NAMESPACES.fields())
        .fetchOne(DefaultNamespaceManager::toNamespace);

Assert.state(created != null, "Failed to insert namespace");
```

### INSERT BATCH

```java
List<InsertValuesStepN> batch = namespaces.stream()
        .map(n -> context.insertInto(NAMESPACES)
                .set(NAMESPACES.ID, n.id().get())
                .set(NAMESPACES.SLUG, n.slug())
                .set(NAMESPACES.NAME, n.name()))
        .collect(Collectors.toList());

context.batch(batch).execute();
```

### UPDATE

```java
int updated = context.update(NAMESPACES)
        .set(NAMESPACES.NAME, newName)
        .set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
        .where(NAMESPACES.ID.eq(id))
        .execute();

Assert.state(updated > 0, "Namespace not found: " + id);
```

### DELETE

```java
int deleted = context.deleteFrom(NAMESPACES)
        .where(NAMESPACES.ID.eq(id))
        .execute();

if (deleted == 0) {
        throw new NamespaceNotFoundException(id);
}
```

---

## Type Mapping

By default, jOOQ maps database types to Java types. Common mappings:

| Database Type | Java Type |
|---------------|-----------|
| `BIGINT` | `Long` |
| `VARCHAR` | `String` |
| `BOOLEAN` | `Boolean` |
| `TIMESTAMP` | `LocalDateTime` |
| `TIMESTAMPTZ` | `OffsetDateTime` |
| `UUID` | `UUID` |
| `JSON` | `String` (usually) |

**Custom type conversion in mapper:**

```java
private static Namespace toNamespace(Record record) {
    return Namespace.builder()
            .id(EntityId.from(record.get(NAMESPACES.ID)))
            .slug(record.get(NAMESPACES.SLUG))
            .name(record.get(NAMESPACES.NAME))
            .createdAt(record.get(NAMESPACES.CREATED_AT))
            .build();
}
```

---

## When to Run ./gradlew generateJooq

**After ANY database schema change:**

1. Create Liquibase changeset (XML file in `konfigyr-data/src/main/resources/migrations/`)
2. Run: `./gradlew generateJooq`
3. Import new generated tables in your service implementation
4. Use the new table references in queries
5. Commit both changeset AND generated code

**Never:**
- Manually edit generated jOOQ classes
- Skip running `generateJooq` after schema changes
- Add business logic to generated classes

---

## Performance Tips

### Avoid N+1 Queries

❌ **Bad:**
```java
List<Namespace> namespaces = context.selectFrom(NAMESPACES).fetch();
namespaces.forEach(n -> {
n.members = context.selectFrom(NAMESPACE_MEMBERS)
            .where(NAMESPACE_MEMBERS.NAMESPACE_ID.eq(n.id()))
        .fetch();  // This loop causes N+1
});
```

✅ **Good:**
```java
// Use EXISTS to filter in a single query
List<Namespace> activeWithMembers = context.selectFrom(NAMESPACES)
                .where(DSL.exists(
                        DSL.select(NAMESPACE_MEMBERS.ID)
                                .from(NAMESPACE_MEMBERS)
                                .where(NAMESPACE_MEMBERS.NAMESPACE_ID.eq(NAMESPACES.ID))
                ))
                .fetch(DefaultNamespaceManager::toNamespace);
```

### Index Frequently Filtered Columns

In Liquibase changesets, create indexes for columns used in WHERE clauses:

```xml
<createIndex tableName="namespaces" indexName="idx_namespace_slug">
    <column name="slug" />
</createIndex>

<createIndex tableName="namespace_members" indexName="idx_member_namespace_id">
<column name="namespace_id" />
</createIndex>
```

### Use LIMIT for List Operations

Always paginate list results to avoid fetching millions of rows:

```java
List<Namespace> recentNamespaces = context.selectFrom(NAMESPACES)
        .orderBy(NAMESPACES.CREATED_AT.desc())
        .limit(100)  // Always set a limit
        .fetch(DefaultNamespaceManager::toNamespace);
```

---

## Debugging Queries

### Print Generated SQL

```java
// Enable debug logging in application.yml
logging:
level:
org.jooq: DEBUG

// Or print directly
System.out.println(context.selectFrom(NAMESPACES).getSQL());
```

### Test Queries in Integration Tests

Use `AbstractIntegrationTest` base class which provides a real database via Testcontainers.

```java
@Test
void shouldFetchNamespaceBySlug() {
    Namespace namespace = context.selectFrom(NAMESPACES)
            .where(NAMESPACES.SLUG.eq("konfigyr"))
            .fetchOne(DefaultNamespaceManager::toNamespace);

    assertThat(namespace).isNotNull().extracting(Namespace::slug)
            .isEqualTo("konfigyr");
}
```

---

## Common Gotchas

### Null Pointer Exception in Mapping

If a record field is null, jOOQ returns null. Always check:

```java
// Safe:
UUID id = record.get(NAMESPACES.ID);
if (id != null) {
        // use it
        }

// Or use Optional:
Optional<String> description = Optional.ofNullable(record.get(NAMESPACES.DESCRIPTION));
```

### String Comparisons Are Case-Sensitive

Use `likeIgnoreCase()` for case-insensitive filtering:

```java
// Case-sensitive (won't match "Konfigyr"):
context.selectFrom(NAMESPACES)
        .where(NAMESPACES.SLUG.like("konfigyr"))

// Case-insensitive (matches "Konfigyr", "KONFIGYR", etc.):
        context.selectFrom(NAMESPACES)
        .where(NAMESPACES.SLUG.likeIgnoreCase("konfigyr"))
```

### Type Mismatches

Always ensure WHERE condition types match column types:

```java
// ✗ Wrong: String compared to Long
context.selectFrom(NAMESPACES)
        .where(NAMESPACES.ID.eq("123"))  // ERROR

// ✓ Correct: Long to Long
        context.selectFrom(NAMESPACES)
        .where(NAMESPACES.ID.eq(123L))
```

---

## Verification Checklist

- [ ] All table references imported statically
- [ ] No raw SQL strings used (always use generated table references)
- [ ] No N+1 queries (use EXISTS for filtering related data)
- [ ] `./gradlew generateJooq` run after schema changes
- [ ] Generated code committed alongside migrations
- [ ] Frequently filtered columns have indexes
- [ ] List queries have LIMIT applied
- [ ] NULL values handled in mapping functions
- [ ] Type conversions correct in mappers
- [ ] Database queries tested in integration tests

---

## When to Ask for Help

- "How do I avoid N+1 queries for this relationship?"
- "Should I use a JOIN or EXISTS subquery?"
- "How do I make this query more efficient?"
- "What's the correct type mapping for this column?"
