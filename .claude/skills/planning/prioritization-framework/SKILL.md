---
name: prioritization-framework
description: >
  Guide PMs through prioritization frameworks (RICE, MoSCoW, Kano, Value-vs-Effort) and apply them to a backlog to produce a ranked list with visible justifications. Use this when a user asks "help me prioritize this backlog", "which features should we build first?", "how do I defend this priority to execs?", or "apply [framework] to rank these." This skill makes the prioritization reasoning explicit so it can be shared and challenged by stakeholders.
---

# Prioritization Framework Helper

## When to Use This Skill

Trigger on phrases like:
- "Help me prioritize this backlog"
- "Which features should we build first?"
- "How do I defend this priority to leadership?"
- "Apply RICE / MoSCoW / Kano to rank these"
- "I have 10 things to do and only capacity for 4 — help"
- "What should we cut?"

Use this whenever the backlog has more items than capacity and a ranking decision must be made and communicated.

## Frameworks Available

### RICE
Best for: Data-informed teams with some usage/reach data available

| Factor | What to estimate | Scale |
|--------|-----------------|-------|
| Reach | Users affected per quarter | Absolute number |
| Impact | Effect on each user | 0.25 (minimal) / 0.5 / 1 (medium) / 2 / 3 (massive) |
| Confidence | How sure are the estimates? | 100% (high) / 80% / 50% (low) |
| Effort | Person-months to build | Absolute number |

**RICE score** = (Reach × Impact × Confidence) / Effort

Higher score = higher priority.

### MoSCoW
Best for: Fixed-scope releases, stakeholder alignment, rapid sorting

- **Must**: Without this, the release fails or is unusable
- **Should**: High value, not strictly required for launch
- **Could**: Nice to have if time permits
- **Won't**: Explicitly not in this release (creates a "later" bucket)

### Kano
Best for: Understanding customer delight vs. baseline satisfaction

- **Basic needs**: Expected features; absence causes dissatisfaction (no delight from presence)
- **Performance**: More is better; users notice improvements linearly
- **Delighters**: Unexpected; creates delight but absence doesn't hurt

### Value vs Effort Matrix (2×2)
Best for: Quick triage, small teams, limited data

Plot items on a 2×2:
```
High Value │ Quick Wins ★  │  Big Bets
           │               │
Low Value  │  Fill-ins     │  Time Sinks
           ├───────────────┼───────────
           Low Effort       High Effort
```
Quick Wins: do first. Big Bets: plan carefully. Fill-ins: if capacity. Time Sinks: cut.

## Workflow

### Step 1: Choose a Framework

If the user hasn't specified one, ask:
- Do you have user reach data? → RICE
- Is this a fixed release with stakeholder sign-off needed? → MoSCoW
- Are you evaluating which features will delight vs. just satisfy? → Kano
- Quick gut-check with limited data? → Value vs Effort

Explain the tradeoff between frameworks briefly and recommend one based on the context.

### Step 2: Gather Estimates

For RICE: walk through each item and estimate Reach, Impact, Confidence, Effort.
For MoSCoW: ask the user to describe each item and categorize collaboratively.
For Kano: ask how users would feel if each feature were absent vs. present.
For Value/Effort: estimate high/medium/low on each axis for each item.

When estimates are uncertain, prompt the user to range-estimate (e.g., "500–2000 users") and use the lower bound for conservative scoring.

### Step 3: Score and Rank

Calculate scores, sort, and display. For tied scores, explain tiebreaker logic (e.g., lower effort wins in RICE ties; Must beats Should in MoSCoW).

### Step 4: Surface Trade-offs

After ranking, call out:
- What gets cut (items that fall below capacity)
- Where estimates were uncertain (affects confidence in rank)
- Interdependencies (lower-ranked item that's a dependency of a higher-ranked one)

### Step 5: Generate Stakeholder-Ready Output

Produce a version the PM can share with leadership or the team — ranked list with brief justifications, not raw scores.

## Output Format

**RICE Scoring Table**

```
## RICE Prioritization

| Feature              | Reach | Impact | Confidence | Effort | RICE Score | Rank |
|----------------------|-------|--------|-----------|--------|------------|------|
| Dark mode            | 5000  | 0.5    | 80%       | 1      | 2000       | #1   |
| CSV export           | 2000  | 1      | 100%      | 2      | 1000       | #2   |
| Webhook integration  | 500   | 2      | 50%       | 3      | 167        | #3   |
| Advanced analytics   | 200   | 3      | 50%       | 5      | 60         | #4   |

### Capacity Decision
Available capacity: 3 person-months → Build #1 and #2. Defer #3 and #4.

### Uncertainty Notes
- Dark mode reach estimate (5000) is based on survey — could be lower
- Webhook confidence is 50% — spike needed before committing

### Trade-off Note
Advanced analytics is the highest impact per user (3), but low reach and high effort
make the RICE score low. Revisit if we get more user evidence.
```

**MoSCoW Output**

```
## MoSCoW Categorization

### Must (ship this or the release fails)
- User auth system
- Core data CRUD

### Should (high value, can ship without)
- Dashboard charts
- Email notifications

### Could (nice to have in this cycle)
- Dark mode
- Bulk operations

### Won't (explicitly deferred)
- Mobile app
- API v2
- Advanced analytics
```

**Stakeholder Summary (any framework)**

```
## Priority Summary for Leadership

We're building [Must/top-RICE items] in Q1. This delivers [core value statement].

[Item 2] and [item 3] are our top priorities because they affect the most users
with the lowest effort. [Item 4] is high-value but requires more investigation before
we commit — treating it as a Q2 candidate.

We're explicitly deferring [Won't items] — they're on the backlog but not in the plan.
```

## Guidelines

- Make the scoring inputs visible — a black-box ranking isn't defensible to stakeholders
- Uncertainty in estimates must be called out; low-confidence scores can be misleading
- Interdependencies can flip the rank: if #4 is a dep of #1, it may need to move up
- MoSCoW "Must" should be strict — if everything is a Must, the framework is being gamed
- After ranking, offer to run **roadmap-planner** to sequence the top items into a delivery plan
