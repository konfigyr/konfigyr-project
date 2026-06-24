---
name: ticket-planner
description: >
  Create a well-structured, individual Jira ticket with a clear description, acceptance criteria, technical approach, and test strategies. Use this in two situations: (1) refining a child ticket that came out of epic-planner, or (2) writing a standalone ticket for a well-scoped piece of work. Trigger phrases: "write up this ticket", "polish ticket [N] from the epic", "create a ticket for [specific task]", "is this ticket ready for dev?" Always run acceptance-criteria-validator after to catch gaps before handing to development.
---

# Ticket Planner

## Position in the Skill Workflow

**Path A — Refining an epic child ticket** (most common):
```
requirements-analyzer → user-story-mapper → epic-planner → [YOU ARE HERE] ticket-planner
                                                                  ├── technical-approach-generator  ← Step 3
                                                                  └── acceptance-criteria-validator
```

**Path B — Standalone ticket** (known, scoped work):
```
requirements-analyzer   ← run first if the task is still vague
  └── [YOU ARE HERE] ticket-planner
        ├── technical-approach-generator  ← Step 3
        └── acceptance-criteria-validator
```

**Prerequisite check**: Before writing the ticket, confirm you have:
- A clear, specific piece of work (not something that should be broken into multiple tickets)
- A known target user or beneficiary
- At least a rough idea of what "done" looks like

If the scope is fuzzy → run **requirements-analyzer** first.
If this is one of several related pieces of work → run **epic-planner** first to establish the full context.

## When to Use

- Refining a child ticket from **epic-planner** output
- User has a specific, scoped task: *"Create a settings page for user preferences"*
- User describes a bug: *"The checkout button is broken on mobile"*
- User says: *"Write up this ticket for our backlog"*
- User wants to polish a rough ticket before it enters a sprint

Do NOT use for large features that haven't been decomposed yet — run **epic-planner** first.
Do NOT use if the requirement is still vague — run **requirements-analyzer** first.

## Workflow

### Step 1: Confirm the Input

Check what the user has provided:

- If they're refining a child ticket from **epic-planner**: pull the title, description, ACs, and dependencies from that output — do not re-ask for context that's already been established
- If they have a **requirements-analyzer** spec: use the Problem Statement and Success Criteria directly
- If they have only a rough description: extract what you can, then ask only for what's missing (scope, target user, definition of done)

### Step 2: Write the Ticket

**Part A: Title**
- Action-oriented, 4–10 words
- ✅ "Add dark mode toggle to user settings"
- ❌ "Settings improvement"

**Part B: Description**
- Open with a user story: *"As a [user role], I want [action] so that [benefit]"*
- 2–3 sentences on what needs to happen and why
- Reference related tickets, epic, spec, or design mockup if available
- Note any constraints (tech stack, accessibility, design system)

**Part C: Acceptance Criteria**
- 3–7 specific, testable conditions verifiable by QA
- Cover: happy path, at least one error state, at least one edge case
- Format: `- [ ] **Given** [context] **When** [action] **Then** [observable result]`
- ✅ `- [ ] **Given** user is on the settings page **When** user toggles dark mode **Then** the UI switches to dark theme immediately`
- ✅ `- [ ] **Given** user logs out **When** user logs back in **Then** their dark mode preference is restored`
- ✅ `- [ ] **Given** user has no saved preference **When** user first logs in **Then** dark mode follows the OS system preference`
- ❌ "Dark mode works"

### Step 3: Technical Approach

Run **technical-approach-generator** to produce this section. Do not write the Technical Approach
without first loading the appropriate domain skills — a generic approach that ignores project
conventions is worse than no approach at all.

`technical-approach-generator` will:
1. Classify the ticket as backend, frontend, or both
2. Load only the relevant skills from `.claude/skills/backend/`, `.claude/skills/frontend/`,
   or `.claude/skills/shared/` — never both backend and frontend simultaneously
3. Generate a grounded Technical Approach using actual project types, annotations, file paths,
   and patterns from those skills

If the ticket is a spike, research task, or purely organisational (no code), skip this step and
write a plain "Research approach" note instead.

### Step 4: Test Strategies

After the Technical Approach, run **test-planning-generator** on this ticket to produce the test scenarios. Embed the resulting test plan inline in the ticket — do not leave it as a separate artifact. The test plan must cover:

- Happy path scenarios (Given/When/Then)
- Error cases (invalid input, missing permissions, failed external calls)
- Edge cases (boundary values, concurrent actions, empty states)
- Regression areas (existing features this ticket might break)

Flag any scenarios that are hard to automate (DNS propagation, OAuth flows, async jobs) and mark them Manual.

### Step 5: Output

Present in two formats.

**Format A: Human-Readable Ticket**

```
# [Ticket Title]

## Description

As a [user role], I want [action] so that [benefit].

[Context: what problem this solves, background, or why it matters now]

[References: related epic, ticket, spec, or design link]

---
## Technical Approach

### Recommended Solution
**Library/Framework:** [name] v[version] ([stability: LTS/stable/beta])
**Documentation:** [official docs URL]

**Standards compliance:** [RFC/spec if applicable]

### Key APIs
**Primary methods:**
- `[method_signature]` - [purpose and when to use]
- `[method_signature]` - [purpose and when to use]

**Configuration:**
- `[parameter]`: [value/type] - [purpose and impact]

### Implementation Pattern
**Core logic:**
```pseudocode
[High-level pseudocode showing main integration flow]
[5-10 lines maximum — this is a guide, not implementation]

---
## Acceptance Criteria

- [ ] **Given** [context] **When** [action] **Then** [observable result]
- [ ] **Given** [context] **When** [action] **Then** [observable result]
- [ ] **Given** [context] **When** [action] **Then** [observable result]
```

**Integration points:**
- **Where:** [file/module path]
- **How:** [dependency injection / direct import / middleware / decorator]
- **When:** [startup / request handler / background task]

<!-- Optional: include when an actor must invoke or consume a mechanism this ticket defines -->
### Scenario Integration

| AC | Actor Trigger | Entry Point | Discovery | Usage Context | Observable Outcome |
|----|--------------|-------------|-----------|---------------|-------------------|
| [AC ref] | [What initiates] | [Named mechanism] | [How found] | [What actor needs to invoke it] | [Verifiable result] |

### Why This Approach
- [Reason 1: standards compliance or industry best practice]
- [Reason 2: performance/security/maintainability benefit]

### Patterns Used
- [Pattern 1] — [purpose in this context]

<!-- Optional: include when the chosen approach has meaningful constraints -->
### Known Limitations
- [Limitation] — [workaround or impact]

<!-- Optional: include when this ticket introduces new error states -->
### Error Handling Strategy

**Expected errors:**
| Error Type | HTTP Status | When Occurs | User Message |
|------------|-------------|-------------|--------------|
| [ErrorType] | [4xx/5xx] | [condition] | [message] |

**Retry logic:**
- Retryable: [list transient errors]
- Backoff: [strategy, max retries, initial delay]

<!-- Optional: include when the ticket touches auditable or sensitive operations -->
### Logging Requirements

| Event | Level | Data Fields | Purpose |
|-------|-------|-------------|---------|
| [event] | INFO/WARN/ERROR | [fields] | [purpose] |

<!-- Optional: include when the ticket includes destructive operations -->
### Destructive Operation Safety
**Operations:** [list each destructive operation]
**Severity:** [CRITICAL / HIGH / MEDIUM]
**Backup plan:** [what and how to verify]
**Rollback plan:** [undo procedure]
**Blast radius:** [resources, scope, downtime]

### Alternatives Considered
- **[Alternative]:** [why rejected]

---

## Test Strategies

### Test Strategy
- **Approach:** [Manual / Automated / Both]
- **Hard-to-automate items:** [anything requiring manual verification]

### Scenario 1: Happy Path — [name]

| # | Given | When | Then | Type |
|---|-------|------|------|------|
| TC-01 | [initial state] | [action] | [result] | Auto/Manual |

### Scenario 2: Error Cases — [name]

| # | Given | When | Then | Type |
|---|-------|------|------|------|
| TC-02 | [initial state] | [action] | [result] | Auto/Manual |

### Scenario 3: Edge Cases — [name]

| # | Given | When | Then | Type |
|---|-------|------|------|------|
| TC-03 | [initial state] | [action] | [result] | Auto/Manual |

### Regression Areas
- [ ] [Existing feature to spot-check after this change]
- [ ] [Existing feature to spot-check after this change]

## Notes
[Open questions, constraints, or hints that don't fit elsewhere]
```

**Format B: Jira Import (JSON)**

```json
{
  "ticket": {
    "summary": "Ticket Title",
    "description": "As a [role], I want [action] so that [benefit].\n\n[Context]\n\n[References]",
    "type": "Story",
    "acceptance_criteria": [
      "Given [context] When [action] Then [result]",
      "Given [context] When [action] Then [result]"
    ],
    "technical_approach": "[summary of recommended solution and integration points]",
    "test_scenarios": [
      "TC-01: Given [state] When [action] Then [result]"
    ],
    "labels": [],
    "custom_fields": {}
  }
}
```

### Step 6: Offer Next Steps

After presenting the ticket, always offer:
1. **Run acceptance-criteria-validator** — quality-check the ACs before handing to dev; catches untestable, vague, or prescriptive criteria and missing edge cases
2. **Re-run technical-approach-generator** — if the domain or tech stack changes, or if the first pass was written without the domain skills loaded
3. **Return to epic-planner** — if there are more child tickets to refine from the same epic

## Guidelines

- Acceptance criteria are observable outcomes, not implementation steps — "use a modal" is not an AC; "user is asked to confirm before deleting" is
- Always use Given/When/Then format for acceptance criteria — it forces the author to specify context, action, and observable outcome explicitly
- Include at least one error state in the ACs — it is the most commonly skipped coverage area
- The Technical Approach is a guide, not a spec — 200–300 words max; leave implementation details to the executor
- The Technical Approach must be grounded in domain skills — always run `technical-approach-generator` rather than writing it from general knowledge; a generic approach that ignores project conventions actively misleads the implementor
- Never load backend and frontend skills simultaneously — if the ticket spans both, split the Technical Approach into `### Backend` and `### Frontend` sub-sections, each grounded in its own skill set
- Omit Technical Approach sub-sections that do not apply — a ticket with no destructive operations needs no Destructive Operation Safety section
- Test scenarios are part of the ticket, not a separate artifact — embed them inline so the dev and QA work from the same document
- Flag hard-to-automate test scenarios explicitly (email delivery, OAuth flows, async jobs, DNS propagation) so QA knows they require manual verification
- Avoid prescriptive tech in the description unless the user explicitly asked for it — tech choices belong in the Technical Approach, not the Description
- If a child ticket references context from the epic, include a brief summary in the ticket description — it should not require reading the epic to understand the ticket
- No estimation — focus on clarity and scope, not time or story points
