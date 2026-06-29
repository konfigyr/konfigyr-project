---
name: epic-planner
description: >
  Decompose a structured requirement into an Epic with 3–7 child tickets, each with acceptance criteria and dependencies noted. Use this after requirements-analyzer has structured the requirement (or user-story-mapper has produced a walking skeleton). Trigger phrases: "break this into tickets", "decompose this into an epic", "create the epic structure for [feature]", "how should we chunk this work?" Do NOT use this as the first step — run requirements-analyzer first if the requirement is still vague.
---

# Epic Planner

## Position in the Skill Workflow

```
requirements-analyzer       ← run first if requirement is still vague or unstructured
  └── user-story-mapper     ← run first if the feature spans a multi-stage user journey
        └── [YOU ARE HERE] epic-planner
              └── risk-dependency-analyzer   ← run after to check cross-team deps and blockers
                    └── roadmap-planner      ← run after to sequence epics into quarters
                          └── ticket-planner ← run to polish individual child tickets
```

**Prerequisite check**: Before decomposing, confirm you have:
- A clear problem statement (from **requirements-analyzer** or the user)
- A defined scope (what's in and out)
- Known target users
- System architecture context — load `.claude/skills/shared/project-overview/SKILL.md` now if not
  already loaded; module ownership, existing API contracts, and cross-module event patterns are
  required to decompose correctly and sequence tickets in the right dependency order

If the problem statement is still missing → run **requirements-analyzer** first.

## When to Use

- User has a structured requirement and says *"Break this into an epic and tickets"*
- User has a **requirements-analyzer** output and is ready to plan work
- User has a **user-story-mapper** walking skeleton and wants to turn it into epics
- User says *"I want to plan out how to implement [feature]"* and the scope is already clear

Do NOT use if:
- The requirement is still vague → run **requirements-analyzer** first
- The feature spans a complex multi-stage journey → run **user-story-mapper** first to map it
- The user already has well-defined individual tickets → jump to **ticket-planner**

## Workflow

### Step 1: Confirm the Input

Check what the user has provided:

- If they have a **requirements-analyzer** output: extract Problem Statement, Scope, and Target Users directly — do not re-ask for these
- If they have a **user-story-mapper** output: use the walking skeleton stories as the basis for child tickets
- If they have raw input only: ask 2–3 targeted clarifying questions (goal, audience, scope boundaries) — or suggest running **requirements-analyzer** first for better results

### Step 2: Identify Epic Boundaries

Define:
- **Epic title** — concise, high-level (e.g., "User Authentication System")
- **Epic description** — 2–3 sentences: the goal, business value, and who benefits
- **Epic acceptance criteria** — observable outcomes that mark the epic as done

### Step 3: Break into Child Tickets

Decompose into 3–7 child tickets. For each, provide:
- **Title** — action-oriented (e.g., "Implement login form and validation")
- **Description** — what needs to be built, why, and relevant context
- **Acceptance criteria** — specific, observable, testable outcomes (not implementation steps)
- **Dependencies** — which tickets must complete first, or any external dependencies

Decomposition heuristics:
- Each ticket should be completable in a single sprint
- Tickets should be ordered: foundational work first, user-visible features after
- If you reach 8+ tickets, the epic likely contains nested epics — ask the user if it should be split
- If an **open question** from requirements-analyzer is still unresolved, create a spike ticket for it

### Step 4: Output

Present in two formats.

**Format A: Human-Readable**

```
# Epic: [Epic Title]

## Description
[2-3 sentence description of the goal and business value]

## Acceptance Criteria
- [Criterion 1]
- [Criterion 2]
- [Criterion 3]

---

## Child Tickets

### Ticket 1: [Title]
**Description:** [What needs to be built and why]

**Acceptance Criteria:**
- [AC 1]
- [AC 2]
- [AC 3]

**Dependencies:** [None, or list of prerequisite tickets]

---

### Ticket 2: [Title]
[repeat structure]
```

**Format B: Jira Import (JSON)**

```json
{
  "epic": {
    "summary": "Epic Title",
    "description": "Epic description",
    "acceptance_criteria": ["AC 1", "AC 2"]
  },
  "tickets": [
    {
      "summary": "Ticket 1 Title",
      "description": "Ticket description",
      "type": "Story",
      "acceptance_criteria": ["AC 1", "AC 2"],
      "dependencies": []
    }
  ]
}
```

### Step 5: Offer Next Steps

After presenting the output, always offer:
1. **Run risk-dependency-analyzer** — check for cross-team dependencies or blockers before committing to a plan
2. **Run ticket-planner** — polish any individual child ticket into a production-ready Jira ticket
3. **Run roadmap-planner** — if you have multiple epics and need to sequence them into quarters

## Guidelines

- **Load `project-overview` before decomposing** — module ownership, existing API contracts, and
  cross-module event flow are required to sequence tickets correctly and avoid boundary violations;
  this is not optional
- Each ticket must name the module it lives in (`com.konfigyr.<module>` for backend, route path for
  frontend) — ambiguous module ownership is a decomposition smell; resolve it before proceeding
- If a ticket crosses two modules, it is likely two tickets — one per module, with an explicit
  dependency between them; cross-module tickets are hard to review and hard to roll back
- Acceptance criteria on child tickets must be specific and testable — if you can't verify it, rewrite it; run **acceptance-criteria-validator** if the user wants a quality check on the ACs
- Tech choices belong in ticket descriptions as hints, not as decomposition drivers — keep the structure business-focused
- Unresolved **open questions** from the spec become spike tickets, not assumptions
- Dependencies between child tickets must be explicit — they inform the sequencing in **roadmap-planner**
- No estimation — focus on clarity and scope, not time or story points
