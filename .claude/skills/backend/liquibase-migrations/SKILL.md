---
name: liquibase-migrations
description: Creating and managing database schema with Liquibase, running migration tasks, and regenerating jOOQ code after schema changes. Use when modifying database structure, adding new tables, or changing columns.
---

# Liquibase Migrations

## Overview

Liquibase manages all schema changes in changesets stored in `konfigyr-data/src/main/resources/migrations/`. After each schema change:

1. Create/update XML changeset file
2. Run `./gradlew generateJooq` to regenerate jOOQ table classes
3. Import generated table references in service implementations
4. Commit both changeset AND generated code

---

## Changeset Format (XML)

Changesets are **XML** (not YAML or SQL). Each changeset has a unique `id` and `author`.

### Basic Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet author="vspasic" id="1.0.0-create-namespaces-table" context="identity or api">
        <preConditions onFail="MARK_RAN">
            <not><tableExists tableName="namespaces" /></not>
        </preConditions>

        <comment>Initial namespaces table migration</comment>

        <createTable tableName="namespaces"
                     remarks="Top-level tenant container for Konfigyr.">
            <column name="id" type="bigint" autoIncrement="true">
                <constraints primaryKey="true" nullable="false" />
            </column>
            <column name="slug" type="varchar(255)">
                <constraints nullable="false" />
            </column>
            <column name="name" type="varchar(255)">
                <constraints nullable="false" />
            </column>
            <column name="description" type="text">
                <constraints nullable="true" />
            </column>
            <column name="created_at" type="timestamptz" defaultValue="NOW()">
                <constraints nullable="false" />
            </column>
            <column name="updated_at" type="timestamptz" defaultValue="NOW()">
                <constraints nullable="false" />
            </column>
        </createTable>

        <addUniqueConstraint
                tableName="namespaces"
                columnNames="slug"
                constraintName="unique_namespace_slug" />

        <createIndex tableName="namespaces" indexName="idx_namespace_slug">
            <column name="slug" />
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

### Key Attributes

| Attribute | Purpose |
|-----------|---------|
| `id` | Unique identifier for this changeset (format: `VERSION-description`, e.g., `1.0.0-create-table`) |
| `author` | Person who created the changeset (your name or initials) |
| `context` | When to apply: `identity`, `api`, or `identity or api` |
| `preConditions` | Check before running (e.g., `not<tableExists>` to avoid duplicate creates) |
| `onFail` | If precondition fails: `MARK_RAN` (continue) or `FAIL` (error) |

---

## Common Operations

### Create Table

```xml
<changeSet author="vspasic" id="1.0.0-create-accounts-table" context="identity or api">
    <preConditions onFail="MARK_RAN">
        <not><tableExists tableName="accounts" /></not>
    </preConditions>

    <createTable tableName="accounts" remarks="User accounts from external OAuth providers.">
        <column name="id" type="bigint" autoIncrement="true">
            <constraints primaryKey="true" nullable="false" />
        </column>
        <column name="email" type="varchar(255)">
            <constraints nullable="false" unique="true" />
        </column>
        <column name="first_name" type="varchar(255)">
            <constraints nullable="true" />
        </column>
        <column name="last_name" type="varchar(255)">
            <constraints nullable="true" />
        </column>
        <column name="avatar_url" type="text">
            <constraints nullable="true" />
        </column>
        <column name="created_at" type="timestamptz" defaultValue="NOW()">
            <constraints nullable="false" />
        </column>
    </createTable>

    <createIndex tableName="accounts" indexName="idx_account_email">
        <column name="email" />
    </createIndex>
</changeSet>
```

### Add Column

```xml
<changeSet author="vspasic" id="1.0.1-add-status-to-namespaces" context="api">
    <preConditions onFail="MARK_RAN">
        <not><columnExists tableName="namespaces" columnName="status" /></not>
    </preConditions>

    <addColumn tableName="namespaces">
        <column name="status" type="varchar(20)" defaultValue="ACTIVE">
            <constraints nullable="false" />
        </column>
    </addColumn>
</changeSet>
```

### Drop Column

```xml
<changeSet author="vspasic" id="1.0.2-remove-legacy-field" context="api">
    <preConditions onFail="MARK_RAN">
        <columnExists tableName="namespaces" columnName="deprecated_field" />
    </preConditions>

    <dropColumn tableName="namespaces" columnName="deprecated_field" />
</changeSet>
```

### Add Foreign Key

```xml
<changeSet author="vspasic" id="1.0.0-create-namespace-members-table" context="api">
    <createTable tableName="namespace_members">
        <column name="id" type="bigint" autoIncrement="true">
            <constraints primaryKey="true" nullable="false" />
        </column>
        <column name="namespace_id" type="bigint">
            <constraints nullable="false" />
        </column>
        <column name="account_id" type="bigint">
            <constraints nullable="false" />
        </column>
        <column name="role" type="varchar(20)">
            <constraints nullable="false" />
        </column>
    </createTable>

    <addForeignKeyConstraint
            baseTableName="namespace_members"
            baseColumnNames="namespace_id"
            constraintName="fk_namespace_member_namespace"
            referencedTableName="namespaces"
            referencedColumnNames="id"
            onDelete="CASCADE" />

    <addForeignKeyConstraint
            baseTableName="namespace_members"
            baseColumnNames="account_id"
            constraintName="fk_namespace_member_account"
            referencedTableName="accounts"
            referencedColumnNames="id"
            onDelete="CASCADE" />
</changeSet>
```

### Create Unique Constraint

```xml
<changeSet author="vspasic" id="1.0.1-add-unique-member-constraint" context="api">
    <preConditions onFail="MARK_RAN">
        <not>
            <uniqueConstraintExists tableName="namespace_members" columnNames="namespace_id,account_id" />
        </not>
    </preConditions>

    <addUniqueConstraint
            tableName="namespace_members"
            columnNames="namespace_id,account_id"
            constraintName="unique_namespace_member" />
</changeSet>
```

### Create Index

```xml
<changeSet author="vspasic" id="1.0.0-create-indices" context="api">
    <createIndex tableName="namespace_members" indexName="idx_member_namespace_id">
        <column name="namespace_id" />
    </createIndex>

    <createIndex tableName="namespace_members" indexName="idx_member_account_id">
        <column name="account_id" />
    </createIndex>
</changeSet>
```

---

## File Organization

Changesets are organized by module in separate directories:

```
konfigyr-data/src/main/resources/migrations/
├── changelog.xml               # Master file that includes all modules
├── namespace/
│   ├── namespaces-1.0.0.xml   # Create namespaces table
│   └── namespaces-1.0.1.xml   # Add status column
├── account/
│   └── accounts-1.0.0.xml     # Create accounts table
├── vault/
│   └── vault-1.0.0.xml
├── kms/
│   └── kms-1.0.0.xml
└── audit/
    └── audit-1.0.0.xml
```

**master changelog.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <includeAll path="namespace" relativeToChangelogFile="true" />
    <includeAll path="account" relativeToChangelogFile="true" />
    <includeAll path="vault" relativeToChangelogFile="true" />
    <includeAll path="kms" relativeToChangelogFile="true" />
    <includeAll path="audit" relativeToChangelogFile="true" />

</databaseChangeLog>
```

---

## Workflow: Adding a New Table

### Step 1: Create Changeset File

Create `konfigyr-data/src/main/resources/migrations/<module>/<module>-1.0.0.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog ...>
    <changeSet author="yourname" id="1.0.0-create-my-table" context="identity or api">
        <preConditions onFail="MARK_RAN">
            <not><tableExists tableName="my_table" /></not>
        </preConditions>

        <createTable tableName="my_table">
            <!-- columns -->
        </createTable>
    </changeSet>
</databaseChangeLog>
```

### Step 2: Regenerate jOOQ Code

```bash
./gradlew generateJooq
```

This:
1. Starts a Testcontainer with PostgreSQL
2. Runs all Liquibase changesets
3. Generates jOOQ table classes based on the schema
4. Cleans up the container

### Step 3: Import Generated Tables

In your service implementation:
```java
import static com.konfigyr.data.tables.MyTable.MY_TABLE;

// Now use MY_TABLE in queries
```

### Step 4: Commit Both Files

Commit both:
- The changeset XML file
- The generated jOOQ code in `build/generated-sources/jooq/`

---

## Naming Conventions

### Changeset IDs

Format: `<VERSION>-<description>`

```
1.0.0-create-namespaces-table
1.0.1-add-status-column
2.0.0-rename-field-to-new-name
```

**Increment version when:**
- Major schema redesign: 2.0.0
- Adding new table: 1.0.1
- Modifying existing table: 1.0.2

### Table Names

- Use snake_case: `namespace_members`
- Pluralize when representing a collection: `namespaces`, `accounts`
- Avoid abbreviations: `account_namespace_members` (not `acc_ns_members`)

### Column Names

- Use snake_case: `created_at`, `namespace_id`
- Use `_at` suffix for timestamps: `created_at`, `updated_at`
- Use `_id` suffix for foreign keys: `namespace_id`, `account_id`

### Constraint Names

- Foreign key: `fk_<table>_<referenced_table>`
    - Example: `fk_namespace_member_account`
- Unique: `unique_<table>_<columns>`
    - Example: `unique_namespace_slug` or `unique_namespace_member`
- Index: `idx_<table>_<columns>`
    - Example: `idx_namespace_slug`, `idx_member_namespace_id`

---

## Context Attribute

Use `context` to control which deployment gets this changeset:

```
context="identity"          # Only apply to identity provider
context="api"               # Only apply to REST API
context="identity or api"   # Apply to both
```

**Typical split:**
- `identity`: User account tables, auth tokens
- `api`: Namespace, vault, kms, audit
- `identity or api`: Shared reference data

---

## Safety Checks (Preconditions)

Always use preconditions to prevent errors on re-runs or deployments:

```xml
<!-- Check table doesn't exist before creating -->
<preConditions onFail="MARK_RAN">
    <not><tableExists tableName="namespaces" /></not>
</preConditions>

<!-- Check column doesn't exist before adding -->
<preConditions onFail="MARK_RAN">
    <not><columnExists tableName="namespaces" columnName="status" /></not>
</preConditions>

<!-- Check constraint doesn't exist before adding -->
<preConditions onFail="MARK_RAN">
    <not>
        <uniqueConstraintExists tableName="namespaces" columnNames="slug" />
    </not>
</preConditions>
```

---

## Data Types

| Liquibase Type | PostgreSQL Type | Java Type |
|---|---|---|
| `bigint` | `BIGINT` | `Long` |
| `varchar(255)` | `VARCHAR(255)` | `String` |
| `text` | `TEXT` | `String` |
| `boolean` | `BOOLEAN` | `Boolean` |
| `int` | `INTEGER` | `Integer` |
| `decimal(10,2)` | `DECIMAL(10,2)` | `BigDecimal` |
| `timestamp` | `TIMESTAMP` | `LocalDateTime` |
| `timestamptz` | `TIMESTAMP WITH TIME ZONE` | `OffsetDateTime` |
| `uuid` | `UUID` | `UUID` |
| `json` | `JSON` | `String` (usually) |

---

## Common Mistakes

### ❌ Creating Same Table Twice

```xml
<!-- First deployment -->
<changeSet id="1.0.0-create-table">
    <createTable tableName="users"> ... </createTable>
</changeSet>

<!-- Second deployment (without precondition) - FAILS! -->
<changeSet id="1.0.0-create-table">
    <createTable tableName="users"> ... </createTable>
</changeSet>
```

**Fix:** Always add preconditions:
```xml
<preConditions onFail="MARK_RAN">
    <not><tableExists tableName="users" /></not>
</preConditions>
```

### ❌ Missing Foreign Key Constraints

Forgetting to add constraints can lead to orphaned data.

```xml
<!-- ✗ Bad: No foreign key -->
<createTable tableName="namespace_members">
    <column name="namespace_id" type="bigint" />
</createTable>

<!-- ✓ Good: Foreign key with cascade delete -->
<addForeignKeyConstraint
        baseTableName="namespace_members"
        baseColumnNames="namespace_id"
        referencedTableName="namespaces"
        referencedColumnNames="id"
        onDelete="CASCADE" />
```

### ❌ Forgetting to Run generateJooq

After schema changes, you MUST run `./gradlew generateJooq` to regenerate jOOQ table classes.

```bash
# ✗ Wrong: Changeset created but jOOQ code not regenerated
git add migrations/namespace/namespace-1.0.1.xml
git commit -m "Add status column"

# ✓ Correct: Both changeset and generated code committed
./gradlew generateJooq
git add migrations/namespace/namespace-1.0.1.xml
git add build/generated-sources/jooq/...
git commit -m "Add status column to namespace"
```

---

## Verification Checklist

- [ ] Changeset has unique `id` and `author`
- [ ] Preconditions prevent re-run failures
- [ ] File in correct module directory (`migrations/<module>/`)
- [ ] All table/column names follow snake_case convention
- [ ] Foreign keys use `onDelete="CASCADE"` where appropriate
- [ ] Indexes created for frequently filtered columns
- [ ] `./gradlew generateJooq` runs successfully
- [ ] Generated jOOQ code committed with changeset
- [ ] Both files (changeset + generated code) in same commit
- [ ] No hardcoded data (use `defaultValue` only for technical defaults)

---

## When to Ask for Help

- "Should I create an index for this column?"
- "What's the correct way to model this relationship?"
- "How do I safely rename a column with existing data?"
- "Do I need a cascade delete for this foreign key?"
- "generateJooq is failing—how do I debug?"
