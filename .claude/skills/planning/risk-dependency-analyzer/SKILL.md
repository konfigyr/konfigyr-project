---
name: risk-dependency-analyzer
description: >
  Scan a set of epics or tickets and identify cross-team dependencies, technical risks, blockers, and integration points. Use this whenever a user asks "analyze these epics for dependencies", "what could go wrong with this plan?", "who do we need to coordinate with?", or "what are the blockers?" Run this before committing to a roadmap — missed dependencies are the most common cause of missed deadlines.
---

# Risk & Dependency Analyzer

## When to Use This Skill

Trigger on phrases like:
- "Analyze these epics for dependencies"
- "What could go wrong with this plan?"
- "Who do we need to coordinate with?"
- "What are the blockers before we start?"
- "Can you check if these tickets have any conflicts?"
- "What's our critical path?"

Run this after **epic-planner** and before **roadmap-planner** — it's the gate check between decomposition and sequencing.

## Workflow

### Step 1: Parse the Input

Accept a list of epics, tickets, or a roadmap draft. For each item, extract:
- External systems or APIs it touches
- Other teams it requires input from
- Shared infrastructure or data it depends on
- Known unknowns or technical spikes needed

### Step 2: Build the Dependency Graph

Map what must be completed before what:
- **Hard dependency**: B cannot start until A is done
- **Soft dependency**: B can start but must be integrated with A before release
- **External dependency**: requires a team/system outside this project

Represent as: `A → B` (A must precede B)

### Step 3: Identify the Critical Path

The critical path is the longest chain of hard dependencies. Delays anywhere on it delay the whole project. Flag it explicitly.

### Step 4: Create the Risk Matrix

For each identified risk, score:
- **Likelihood**: High / Medium / Low
- **Impact**: High / Medium / Low
- **Severity**: derived (High×High = Critical, High×Low or Low×High = High, etc.)

Include mitigation for Critical and High risks.

### Step 5: Compile the Blocker List

List specific things that cannot proceed without external action. Format: what is blocked, what it's waiting on, who owns the resolution.

## Output Format

**Dependency Graph**

```
## Dependency Graph

Epic A: [Title]
  └── Hard dep: Epic B (Epic B must complete before A ships)
  └── Soft dep: Epic C (must integrate before release)

Epic D: [Title]
  └── External: Requires Auth team to expose new OAuth scope

Critical Path: Epic B → Epic A → Epic E
(Any slip in B delays A, which delays E)
```

**Risk Matrix**

```
## Risk Matrix

| Risk                              | Likelihood | Impact | Severity | Mitigation                          |
|-----------------------------------|-----------|--------|----------|-------------------------------------|
| Auth API not ready in time        | High      | High   | Critical | Spike in week 1; escalate to infra  |
| Third-party rate limits hit       | Medium    | High   | High     | Implement retry logic + caching     |
| Data migration takes longer       | Medium    | Medium | Medium   | Run parallel dry-run in staging     |
| New hire ramp-up slower than plan | Low       | Medium | Low      | Pair programming plan ready         |
```

**Blocker List**

```
## Blockers

### BLOCKER-1: Payment gateway refund endpoint (Critical)
- Blocked: Epic C (Refund Flow)
- Waiting on: Payment team to expose POST /refunds API
- Owner: [PM name] to raise with Payment team by [date]
- Risk if unresolved: Epic C cannot ship in Q1

### BLOCKER-2: Legal approval for data retention policy (High)
- Blocked: Epic D (Data Export)
- Waiting on: Legal sign-off on 90-day retention rule
- Owner: [PM name] to send spec to Legal this week
```

## Example

**Input**: Three epics: "User Auth System", "Admin Dashboard", "Audit Log"

**Output (abbreviated)**:

```
## Dependency Graph

User Auth System (Epic 1)
  └── No external deps — starts immediately

Admin Dashboard (Epic 2)
  └── Hard dep: User Auth System (requires working auth to protect routes)

Audit Log (Epic 3)
  └── Hard dep: User Auth System (needs user identity for log entries)
  └── Soft dep: Admin Dashboard (log viewer lives in dashboard)

Critical Path: User Auth System → Admin Dashboard → Audit Log viewer

## Risk Matrix

| Risk                          | Likelihood | Impact | Severity |
|-------------------------------|-----------|--------|----------|
| Auth delays cascade to all    | Medium    | High   | High     |
| Dashboard scope expands       | High      | Medium | High     |
| Audit log storage costs spike | Low       | Low    | Low      |
```

## Guidelines

- Call out "this requires X which we don't own" explicitly — silent external dependencies are the most dangerous
- Hard dependencies vs soft dependencies matter: soft deps can be parallelized with a later integration point
- Include infrastructure and scaling risks, not just feature risks
- The critical path is the PM's primary risk-management lever — highlight it prominently
- After analysis, offer to run **roadmap-planner** to sequence the epics respecting these dependencies
