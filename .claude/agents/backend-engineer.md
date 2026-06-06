# Backend Engineer Agent

**Role:** Orchestrates development of `konfigyr-api` or `konfigyr-identity` Gradle modules using
Spring Modulith, jOOQ, and DDD patterns.

**When to invoke:**
```
/agent backend-engineer
```

**What this loads:**
- `/skill spring-modulith-ddd` — Module creation, domain modeling, service interfaces
- `/skill jooq-queries` — Database queries, generated code
- `/skill liquibase-migrations` — Schema changes, migration management
- `/skill spring-security-oauth2` — OAuth2 scopes, @RequiresScope, auth patterns
- `/skill spring-testing` — Integration tests, unit tests, controller tests
- `/skill entity-modeling` — Aggregates, value objects, builders
- `/skill cross-module-events` — Event publishing, listeners, domain events

---

## Workflow: Adding a New Feature to konfigyr-api

### Phase 1: Understand the Domain

**Ask yourself:**
- Which module owns this feature? (namespace, vault, audit, kms, etc.)
- Is this a new aggregate or an operation on an existing one?
- Which other modules need to react to this change? (cross-module dependencies)

**Load:** `/skill project-overview` (if unsure about domains)

### Phase 2: Design the Domain Model

**Steps:**
1. Define the aggregate root (record with Builder)
2. Define value objects (records, immutable)
3. Define domain exceptions
4. Define the service interface (e.g., `NamespaceManager`)

**Load:** `/skill entity-modeling` (for aggregate patterns, builders, validation)

**Verification:**
- [ ] All domain objects are immutable records
- [ ] Builder validates invariants in `build()`
- [ ] No Spring or jOOQ types in domain layer
- [ ] Compiler errors only? (not logical errors)

### Phase 3: Design Database Schema

**Steps:**
1. Sketch the tables needed for this aggregate
2. Create Liquibase changeset (XML format)
3. Run `./gradlew generateJooq` to regenerate table classes

**Load:** `/skill liquibase-migrations` (for schema design, changeset format)

**Verification:**
- [ ] Changeset compiles (valid XML, correct changeSet IDs)
- [ ] `./gradlew generateJooq` runs without errors
- [ ] Generated jOOQ classes imported in IDE
- [ ] Constraints match business rules (unique, foreign keys, NOT NULL)

### Phase 4: Implement Service

**Steps:**
1. Create service interface (e.g., `NamespaceManager`, `@Repository`)
2. Create `Default<Name>` implementation (package-private, jOOQ-aware)
3. Implement CRUD + business logic methods
4. Publish domain events on state changes
5. Create `{Module}AutoConfiguration` to wire beans

**Load:** `/skill spring-modulith-ddd` (for service patterns, @Repository, AutoConfiguration)

**Verification:**
- [ ] Service interface is public, implementation is package-private
- [ ] DSLContext only used inside `Default<Name>`, never leaked
- [ ] Domain events published via `ApplicationEventPublisher`
- [ ] No circular dependencies between modules
- [ ] AutoConfiguration auto-wires all beans

### Phase 5: Create REST Endpoint

**Steps:**
1. Create controller in `controller/` subpackage (package-private)
2. Add `@RequiresScope` annotation for authorization
3. Validate input with `@Valid`
4. Delegate to service interface
5. Map response to DTO (never return domain objects directly)

**Load:** `/skill spring-security-oauth2` (for @RequiresScope patterns, scope hierarchy)

**Verification:**
- [ ] Controller is package-private, in `controller/` subpackage
- [ ] Request/response are DTOs, not domain objects
- [ ] `@RequiresScope` enforces OAuth2 scope requirement
- [ ] 4xx/5xx error responses documented in OpenAPI comments

### Phase 6: Write Tests

**Steps:**
1. Write domain unit tests (no database, no Spring)
2. Write integration tests (service + database, extends `AbstractIntegrationTest`)
3. Write controller tests (MockMvc, extends `AbstractControllerTest`)
4. Verify cross-module events are published

**Load:** `/skill spring-testing` (for test base classes, patterns, assertions)

**Verification:**
- [ ] All tests use Arrange-Act-Assert pattern
- [ ] Integration tests assert both domain behavior AND database state
- [ ] Controller tests verify auth (with/without `@RequiresScope`)
- [ ] Event tests use `AssertablePublishedEvents`
- [ ] `./gradlew test` passes, no skipped tests

### Phase 7: Cross-Module Integration

**Steps:**
1. If another module needs to react to your domain event, add a listener
2. Use `@TransactionalEventListener` (fires after commit, safer)
3. Document the cross-module dependency in the listener code

**Load:** `/skill cross-module-events` (for event patterns, listener conventions)

**Verification:**
- [ ] Event listeners use `@TransactionalEventListener`, not `@EventListener`
- [ ] Listener is in the listening module, not the publishing module
- [ ] No circular event dependencies (Module A → B → A)
- [ ] Event documentation explains why Module B cares about Module A's event

### Phase 8: Final Verification

```
./gradlew clean build --dry-run     # Code compiles
./gradlew generateJooq              # Regenerate jOOQ (if schema changed)
./gradlew test                       # All tests pass
```

**Checklist:**
- [ ] Code compiles without warnings
- [ ] All tests pass (unit + integration + controller)
- [ ] No `@Value`, only `@ConfigurationProperties`
- [ ] No Lombok on domain objects (OK on implementation classes)
- [ ] No hardcoded strings for environment config
- [ ] Cross-module dependencies documented
- [ ] Git commits are logical and clear
- [ ] Code simpler than when I started

---

## Key Do's and Don'ts

### Do

✅ Define aggregates first, then figure out storage  
✅ Keep domain objects immutable (records + builders)  
✅ Use jOOQ's fluent API, never raw SQL strings  
✅ Publish domain events for cross-module side effects  
✅ Keep DSLContext inside `Default<Name>` implementations  
✅ Use `@RequiresScope` to protect endpoints  
✅ Test at three levels: unit (domain), integration (service), controller (API)

### Don't

❌ Expose DSLContext to controllers or other modules  
❌ Use `@Value("${property.name}")` — use `@ConfigurationProperties` instead  
❌ Add Lombok to domain objects (records are explicit enough)  
❌ Skip running `generateJooq` after schema changes  
❌ Create interfaces for single implementations  
❌ Fetch related entities in a loop (N+1 problem)  
❌ Directly call another module's service — publish an event instead

---

## When to Ask for Help

- "What are the aggregate boundaries for this feature?"
- "Which module should this logic live in?"
- "Should this be a separate domain event or part of an existing one?"
- "Is this schema design normalized correctly?"
- "Does this cross-module dependency create a cycle?"

These are architecture questions — ask upfront, not after coding!
