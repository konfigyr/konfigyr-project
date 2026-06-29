---
name: roadmap-planner
description: >
  Convert a prioritized list of epics into a phased release roadmap with clear dependencies, confidence levels, and stakeholder-ready communication. Use this when a user asks "create a roadmap for the next two quarters", "how should we sequence these features?", "when can we commit to shipping [epic]?", or "help me build the Q3/Q4 plan." Always run risk-dependency-analyzer first — a roadmap without dependency data is guesswork.
---

# Roadmap Planner

## When to Use This Skill

Trigger on phrases like:
- "Create a roadmap for the next [N] quarters"
- "How should we sequence these epics?"
- "Build me a Q3/Q4 plan"
- "When can we commit to shipping [feature]?"
- "Help me plan what ships when"
- "I need to present our roadmap to leadership"

Run this after **epic-planner** and **risk-dependency-analyzer**. A roadmap built without dependency data will have conflicts. If the user hasn't run risk analysis yet, prompt them to do it first or do a lightweight dependency scan inline.

## Workflow

### Step 1: Gather Inputs

Collect:
- **Epic list** with rough priority order (P1/P2/P3 or Must/Should/Could)
- **Team capacity** — how many devs, designer, QA? (approximate is fine)
- **Time horizon** — how many quarters or sprints to plan?
- **Hard deadlines** — any epics that must ship by a specific date?
- **Known dependencies** — from risk-dependency-analyzer or user input

### Step 2: Assign Epics to Phases

- Respect hard dependencies (dep must complete before successor starts)
- Group related epics where parallelism is possible
- Leave 15–20% buffer per quarter for unplanned work and bugs
- Flag epics with unresolved dependencies as "conditional" for that phase

### Step 3: Assign Confidence Levels

For each epic per phase:
- **High confidence**: dependency-free, well-scoped, team has done similar work
- **Medium confidence**: some external dependency or scope uncertainty
- **Low confidence**: depends on unresolved blocker, or scope is still fuzzy

### Step 4: Generate Stakeholder Summary

For each phase, write a 3–5 sentence stakeholder pitch: what ships, who benefits, what the business impact is. Avoid technical jargon.

## Output Format

**Quarterly Roadmap**

```
## Roadmap: [Time Horizon]

---

### Q[N]: [Theme/Focus for this quarter]

| Epic                  | Confidence | Notes                                      |
|-----------------------|-----------|---------------------------------------------|
| User Auth System      | High       | No blockers; starts week 1                 |
| Admin Dashboard       | High       | Depends on Auth (which ships mid-Q)        |
| Payment Integration   | Medium     | Waiting on payment team API (BLOCKER-1)    |

**Might slip**: Payment Integration — depends on external team delivery
**Capacity note**: ~3 devs, 1 designer, assumes no major incidents

**Stakeholder summary**:
Q1 delivers the foundation: users can register, log in, and access a fully functional
admin panel. The auth system underpins everything that comes after. Payment integration
ships if the external dependency resolves on time — contingency plan is to move it to Q2.

---

### Q[N+1]: [Theme/Focus]

| Epic                  | Confidence | Notes                                      |
|-----------------------|-----------|---------------------------------------------|
| Audit Log             | High       | Auth and Dashboard complete from Q1        |
| CSV Export            | Medium     | Scope needs final sign-off                 |
| API v2                | Low        | Blocked on architecture decision           |

**Might slip**: API v2 — architecture spike not complete
...
```

**Dependencies Reminder**

```
## Dependency Summary

Critical Path: Auth → Dashboard → Audit Log
Q1 risk: Any Auth slip pushes Dashboard into Q2, which cascades to Audit Log
Mitigations documented in risk-dependency-analyzer output
```

**Stakeholder-Ready Pitch (per quarter)**

```
## Executive Summary

**Q1 (Jan–Mar)**: Foundation
We're building the authentication and admin infrastructure that every subsequent
feature depends on. By end of Q1, users can register, log in securely, and admins
have a working dashboard. This is a prerequisite phase — no customer-facing features
ship, but it unblocks Q2 and Q3 delivery.

**Q2 (Apr–Jun)**: Customer Value
With the foundation complete, Q2 ships the features customers have been requesting:
audit logging, CSV export, and the start of our v2 API. Payment integration moves here
if the Q1 dependency resolves.
```

## Example

**Input**: "I have 6 epics: Auth, Dashboard, Audit Log, CSV Export, Payment Integration, API v2. Plan for 2 quarters with a 3-person team."

**Output**: (see format above — assigns Auth + Dashboard to Q1 as the critical path, stacks remaining epics in Q2 by priority and dependency, flags Payment Integration as conditional on external blocker)

## Guidelines

- Never schedule more than 80% of team capacity — 20% buffer is not optional; it's the difference between a realistic roadmap and a wishlist
- Confidence levels are honest signals, not aspirational ones — if it's Low, say so
- Hard deadlines must be flagged explicitly and dependencies resolved or escalated before committing
- The stakeholder pitch hides dependency complexity but a tech-level view must always accompany it
- After presenting the roadmap, offer to run **risk-dependency-analyzer** on the Q1 block if not already done
