# CLAUDE.md - Konfigyr Development Guidelines

Behavioral guidelines for Claude Code in Konfigyr projects. Extends Andrej Karpathy's methodology.

**Tradeoff:** These guidelines bias toward caution, maintainability, and production-readiness over speed. For trivial tasks, use judgment.

---

## 1. Project Summary

### What is Konfigyr?

Konfigyr is a Spring Boot-native configuration management platform offered as both SaaS and on-premise. The core problem it solves is configuration chaos in teams running microservices: config drift across environments, human errors from manual edits, and lack of auditability or access control over sensitive properties.

Unlike generic secret stores (HashiCorp Vault) or framework-agnostic tools (Infisical), Konfigyr is opinionated about Spring Boot. It reads property metadata generated at build time to power a type-safe UI that validates values before they reach production, surfaces deprecations, and tracks how configuration evolves across artifact versions.

### System Architecture

The system consists of three deployed services that communicate via OAuth2/OIDC:

- **Identity Provider** (`konfigyr-identity`) — Built on Spring Authorization Server. Brokers authentication to external providers (GitHub, GitLab, OIDC), issues PS256/ES256 JWTs, and manages user identity records. Does not store credentials.
- **REST API** (`konfigyr-api`) — Spring Boot OAuth2 Resource Server. Hosts all domain logic: namespaces, vaults, artifactory, KMS. Protected by JWT scopes enforced via `@RequiresScope`.
- **Frontend** (`konfigyr-frontend`) — React 19 / TanStack Start app. Authenticates via Authorization Code + PKCE, holds tokens in session, consumes the REST API.

A Gradle/Maven plugin layer handles build-time metadata ingestion: plugins extract `spring-configuration-metadata.json` from applications and push it to the REST API, populating the Artifactory and enabling the type-safe UI.

### Business Domains

| Domain | Module(s) | Responsibility |
|--------|-----------|----------------|
| **Namespaces & Accounts** | `account`, `namespace`, `membership` | Multi-tenancy: namespaces are the top-level tenant container. Members have roles (`ADMIN`, `USER`). Services (Spring Boot apps) are owned by namespaces. |
| **Vault** | `vault` | Per-service configuration: profiles (dev/staging/prod), change sets, change requests with optional approval workflows, and change history. |
| **Artifactory** | `artifactory` | Metadata registry: indexes artifact versions and their configuration property descriptors (as JSON Schema). Powers type-safe UI, diff/change analysis, and provenance tracking. |
| **KMS** | `kms` | Per-namespace cryptographic keyset management. Each namespace gets an isolated keyset (backed by Google Tink) used to encrypt vault contents. Supports encrypt, key-encapsulation, and signing purposes. |
| **Identity** | `konfigyr-identity` module | User account provisioning from external OAuth providers, JWT token customisation, authorization consent. |
| **Audit** | `audit` | Centralized audit log. Listens to domain events from all other modules via `@TransactionalEventListener` and persists structured records. |
| **Feature flags** | `feature` | Per-namespace feature limits and toggles (e.g. max member count, max services). |

---

## 2. Karpathy's 4 Core Principles

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly
- If multiple approaches exist, present tradeoffs
- If uncertain about architecture or requirements, ask before coding
- If something is unclear, stop and ask for clarification

### 2. Simplicity First

**Minimum code that solves the problem. No speculative features.**

- No features beyond what was asked
- No abstractions for single-use code
- No "flexibility" or "configurability" that wasn't requested
- No error handling for impossible scenarios
- If you write 200 lines and it could be 50, rewrite it

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't refactor unrelated code or reformat files
- Match existing code style exactly
- Remove only what YOUR changes made unused
- Don't delete pre-existing dead code unless asked

**The test:** Every changed line traces directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
```
Goal: "Add OAuth scope validation to endpoint"
↓
Specific:
  1. Create validator for required scope → Test: endpoint returns 403 without scope
  2. Add @RequiresScope annotation → Test: endpoint accepts valid scope
  3. Update OpenAPI docs → Test: docs reflect new scope requirement

Verification:
  - Run: ./gradlew test
  - Check: Auth tests pass
  - Check: No regressions
```

---

## 3. Quick Reference: Which Skill to Load

**You're working on backend (Java, Spring Boot)?**
→ Load backend agent: `/agent backend-engineer`

**You're working on frontend (React, TanStack)?**
→ Load frontend agent: `/agent frontend-engineer`

**You need project architecture context?**
→ Load skill: `/skill project-overview`

---

## 4. Backend Modules & Agents

### Agents

**`backend-engineer`** — Orchestrates backend development
- Loads: Spring Modulith + DDD, jOOQ queries, Liquibase migrations, OAuth2 security, Spring testing, entity modeling, cross-module events
- Use when: Adding features to `konfigyr-api`, creating new modules, writing domain logic

### Backend Skills

| Skill | When to Use |
|-------|-----------|
| `spring-modulith-ddd` | Creating a new module, defining aggregates, service interfaces |
| `jooq-queries` | Writing database queries, understanding generated code |
| `liquibase-migrations` | Modifying database schema, running migrations |
| `spring-security-oauth2` | Protecting endpoints with `@RequiresScope`, auth flows |
| `spring-testing` | Writing integration tests, unit tests, controller tests |
| `entity-modeling` | Designing domain objects, value objects, aggregates |
| `cross-module-events` | Publishing/listening to domain events between modules |

---

## 5. Frontend Routes & Agents

### Agents

**`frontend-engineer`** — Orchestrates frontend development
- Loads: TanStack routing, TanStack queries, React components, Tailwind styling, OIDC authentication, frontend testing, form handling
- Use when: Adding pages/features to `konfigyr-frontend`, fixing UI bugs, improving performance

### Frontend Skills

| Skill | When to Use |
|-------|-----------|
| `tanstack-routing` | Creating new routes, understanding file-based routing, loaders |
| `tanstack-queries` | Fetching/mutating data, cache invalidation, query keys |
| `react-components` | Building new components, component conventions |
| `tailwind-styling` | Styling components, adding new design tokens |
| `oidc-authentication` | Session management, token handling, auth flows |
| `frontend-testing` | Testing components, hooks, routes with MSW |
| `form-handling` | Building forms with TanStack Form |

---

## 6. Shared Principles

### No Context Pollution

- **Backend work:** Frontend skills stay unloaded (no React, TanStack clutter)
- **Frontend work:** Backend skills stay unloaded (no Spring, jOOQ clutter)
- Each skill is self-contained and loads only what's needed

### Verification Checklist (All Work)

- [ ] Code compiles/builds without warnings
- [ ] All tests pass (`./gradlew test` for backend, `npm run test:ci` for frontend)
- [ ] No unused imports or variables
- [ ] No hardcoded values (use configuration)
- [ ] Git history is clean (logical commits)
- [ ] Existing functionality not broken
- [ ] Code is simpler than when I started
- [ ] I can explain the change in one sentence

### Git & Change Management

**Commit Messages:**
- Clear, imperative: "Add namespace quota validation"
- One logical change per commit

**Pull Requests:**
- Title summarizes change
- Description explains WHY
- Link to issues/tickets

**When to Ask Before Changing:**
- Anything affecting public APIs
- Database schema changes
- Dependency upgrades
- Configuration defaults
- Architectural decisions

---

## 7. Skill Maintenance

When making changes that affect documented architecture, update the relevant skill file as part of the same task — not as a separate follow-up.

| If you change...                                                 | Update this skill |
|------------------------------------------------------------------|------------------|
| Auth flow, JWT claims, `OAuthScope`, `@RequiresScope`, token types | `spring-security-oauth2` |
| KMS key purposes, lifecycle states, konfigyr-crypto usage        | `project-overview` |
| Artifactory domain entities, ingestion flow, SDK types           | `project-overview` |
| IdP broker behaviour, OIDC endpoints, JWS algorithm              | `project-overview`, `oidc-authentication`, `spring-security-oauth2` |
| New module, aggregate, or cross-module event                     | `project-overview`, `spring-modulith-ddd` |
| Database schema, Liquibase patterns                              | `liquibase-migrations` |
| API endpoint contracts or scopes                                 | `project-overview`, `spring-security-oauth2` |
| Frontend routes or loaders                                       | `project-overview`, `tanstack-routing` |
| Domain entity design, value objects, aggregates                  | `entity-modeling` |
| Namespace roles, membership model                                | `project-overview` |

---

## 8. Getting Started

**First time here?**

1. Read this file (you're done!)
2. Load the appropriate agent:
    - Backend: `Load agent: backend-engineer`
    - Frontend: `Load agent: frontend-engineer`
3. Each agent will guide you through the relevant skills

**Want to understand the whole system?**
→ Load `/skill project-overview` for architecture deep-dive

---

**Default Behavior:** When in doubt, ask. Better a clarifying question than an unnecessary rewrite.
