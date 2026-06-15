---
name: technical-approach-generator
description: >
  Load the right domain skills and generate a grounded Technical Approach section for a ticket.
  Detects the implementation domain (backend, frontend, or both) from the ticket's description and
  ACs, loads only the relevant skills from the backend/, frontend/, or shared/ folders, then
  produces the Technical Approach. Use this as Step 3 inside ticket-planner, or standalone when
  retrofitting a Technical Approach onto an existing ticket. Never load backend and frontend skills
  simultaneously — context pollution degrades output quality.
---

# Technical Approach Generator

## Position in the Skill Workflow

```
ticket-planner
  └── [YOU ARE HERE] technical-approach-generator   ← Step 3 of ticket-planner
        └── acceptance-criteria-validator
              └── test-planning-generator
```

Can also be invoked standalone:
```
[existing ticket or description] → [YOU ARE HERE] technical-approach-generator
```

---

## Workflow

### Step 1: Determine the Domain

Read the ticket's description and acceptance criteria. Classify as one of:

| Domain | Signals |
|--------|---------|
| **Backend** | Java, Spring Boot, jOOQ, Liquibase, REST controller, domain aggregate, service, database migration, OAuth scope, event, security |
| **Frontend** | React, TanStack, TypeScript, route, component, form, query, Tailwind, OIDC, session |
| **Both** | Ticket explicitly spans backend (new endpoint) AND frontend (new page consuming it) |
| **Infrastructure / Shared** | Git, CI, build, dependency management — no Java or React code |

If the domain is ambiguous, ask before proceeding.

### Step 2: Load Skills — Backend

Load only these skills if the ticket is **backend**. Load only what the ticket actually touches
— do not load all backend skills by default.

| Skill | Load when the ticket involves... |
|-------|----------------------------------|
| `spring-modulith-ddd` | New module, aggregate, service interface, AutoConfiguration |
| `entity-modeling` | New record types, value objects, enums, builder patterns |
| `jooq-patterns` | Database queries, SELECT / INSERT / UPDATE / DELETE, mappers |
| `liquibase-migrations` | Schema changes: new table, column, index, FK, constraint |
| `spring-security-oauth2` | `@RequiresScope`, `OAuthScope`, `@PreAuthorize`, JWT claims, auth flows |
| `spring-testing` | Integration tests, `@SpringBootTest`, `@WebMvcTest`, test fixtures |
| `cross-module-events` | Domain events, `@TransactionalEventListener`, cross-module communication |

**To load a backend skill:** read
`.claude/skills/backend/<skill-name>/SKILL.md`
and apply its patterns, conventions, and code examples when writing the Technical Approach.

### Step 3: Load Skills — Frontend

Load only these skills if the ticket is **frontend**. Do not load backend skills.

| Skill | Load when the ticket involves... |
|-------|----------------------------------|
| `tanstack-routing` | New route, loader, file-based routing, navigation |
| `tanstack-queries` | Data fetching, mutations, cache invalidation, query keys |
| `react-components` | New component, component conventions, props API |
| `tailwind-styling` | Styling, design tokens, responsive layout |
| `form-handling` | TanStack Form, field components, validation, submission |
| `oidc-authentication` | Session management, token handling, protected routes |
| `frontend-testing` | Component tests, MSW mocks, route tests |

**To load a frontend skill:** read
`.claude/skills/frontend/<skill-name>/SKILL.md`
and apply its patterns, conventions, and code examples when writing the Technical Approach.

### Step 4: Load Skills — Shared (always available)

These skills may be consulted for any ticket regardless of domain:

| Skill | Load when the ticket involves... |
|-------|----------------------------------|
| `project-overview` | Needs broad architecture context, cross-domain awareness |
| `git-practices` | Commit strategy, branching, PR structure |

### Step 5: Generate the Technical Approach

Using the loaded skills as grounding, produce the Technical Approach section following the
`ticket-planner` output format. The approach must be:

- **Domain-specific**: reference actual types, annotations, file paths, and patterns from the
  loaded skills — never generic pseudocode that ignores project conventions
- **Concise**: 200–300 words max; key APIs (2–5 entries), one pseudocode sketch, integration points
- **Selective on optional sections**: include Error Handling, Logging, Destructive Operation Safety,
  Alternatives Considered, and Scenario Integration only when genuinely applicable

**Backend Technical Approach must reference:**
- The correct package (`com.konfigyr.<module>`)
- Correct type conventions (`record` + `Builder`, `Default<Name>` implementation, `@Repository`
  service interface, `@AutoConfiguration` wiring)
- Correct jOOQ patterns (static table imports, private static mapper, labeled `@Transactional`)
- Correct Liquibase patterns (XML changesets, `context="api"`, `<preConditions>`, naming conventions)
- Correct security patterns (`@RequiresScope(OAuthScope.X)`, `@PreAuthorize("isAdmin()")`)

**Frontend Technical Approach must reference:**
- The correct file-based route path
- Correct TanStack Query key structure and cache invalidation
- Correct component conventions (props API, Tailwind classes, design tokens)
- Correct form hook pattern (`createFormHook` / `createFormHookContexts`)
- Array types as `Array<Type>`, not `Type[]`

---

## No Context Pollution Rule

**Never load backend and frontend skills in the same invocation** unless the ticket explicitly
spans both layers. When a ticket creates both a backend endpoint AND a frontend page, split the
Technical Approach into two labelled sub-sections: `### Backend` and `### Frontend`, loading
each skill set in turn for its respective sub-section.

---

## Output Format

Produce only the Technical Approach section using the `ticket-planner` template. Do not re-output
the full ticket — just the section so it can be inserted at Step 3 of `ticket-planner`.

```
## Technical Approach

### Recommended Solution
**Library/Framework:** [name] v[version] — grounded in loaded skill, not generic
**Documentation:** [official docs URL if applicable]
**Standards compliance:** [RFC/spec if applicable]

### Key APIs
- `[fully.qualified.Type.method()]` — [purpose]; from [loaded skill name]
- `[annotation or pattern]` — [when and why]

### Implementation Pattern
**Core logic:**
```[language]
[pseudocode or real code sketch — follows project conventions exactly]
```

**Integration points:**
- **Where:** [exact file path in com.konfigyr.* or src/routes/...]
- **How:** [injection / import / annotation / hook pattern]
- **When:** [startup / request handler / event / render]

### Why This Approach
- [reason tied to a convention or constraint from the loaded skill]

[Include optional sections only if applicable: Scenario Integration, Error Handling,
Logging, Destructive Operation Safety, Known Limitations, Alternatives Considered]
```

---

## Skill Selection Examples

**"Add DNS TXT verification strategy"** → backend ticket
→ Load: `spring-modulith-ddd` (new `@Component`), `entity-modeling` (new record types)
→ Skip: `liquibase-migrations` (no schema change), `spring-security-oauth2` (no auth change)

**"Add groupId verification form to settings page"** → frontend ticket
→ Load: `tanstack-routing` (new route), `tanstack-queries` (mutation + cache), `form-handling` (TanStack Form)
→ Skip: all backend skills

**"Add publisher_verified column to artifact_versions"** → backend ticket
→ Load: `liquibase-migrations` (new column), `jooq-patterns` (mapper update)
→ Skip: `spring-modulith-ddd` (no new aggregate)

**"Wire the namespace verification status badge"** → frontend ticket
→ Load: `react-components` (new component), `tailwind-styling` (badge variant), `tanstack-queries` (query)
→ Skip: `form-handling` (no form), all backend skills
```

---

## When to Ask for Help

- "This ticket touches both a new REST endpoint and a new frontend page — split into two sub-sections?"
- "I cannot determine from the description whether this is backend or frontend — clarify before proceeding."
- "The ticket description doesn't mention a specific framework or pattern — which module does this belong to?"
