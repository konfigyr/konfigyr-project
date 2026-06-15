---
name: user-story-mapper
description: >
  Decompose a user journey or workflow into a visual story map: a matrix of user stories organized by lifecycle stages. Use this when a user says "map out the entire user journey", "I want to understand all the stories involved in [workflow]", "create a story map for [feature]", or "what does the full experience look like?" This skill reveals gaps, duplicates, and prioritization opportunities before any ticket is written.
---

# User Story Mapper

## When to Use This Skill

Trigger on phrases like:
- "Map out the entire user journey for [feature]"
- "What stories are involved in the [workflow]?"
- "Create a story map for [feature]"
- "Walk me through everything a user does when they [action]"
- "I want to see the big picture before we start writing tickets"

Use this before **epic-planner** when the feature spans multiple stages of a user journey. For a single-screen feature, go directly to epic-planner.

## Workflow

### Step 1: Identify the User and Journey

Ask:
- **Who is the user?** (role, persona, experience level)
- **What is the end goal?** (what are they trying to accomplish overall?)
- **What triggers the journey?** (what starts it? what ends it?)

### Step 2: Define Lifecycle Stages

Break the journey into 4–7 high-level stages. Common patterns:

- **Discovery → Onboarding → Active Use → Advanced Use → Renewal/Churn**
- **Awareness → Evaluation → Purchase → Setup → Daily Use → Support**
- **Request → Review → Approval → Implementation → Verification**

Tailor stages to the specific workflow — don't force a generic template.

### Step 3: Map Stories to Stages

For each stage, identify 2–4 user stories — things the user does, needs, or experiences. Each story is one-liner format:

> "As a [user], I [action] so that [benefit]"

Label stories with IDs: `S1-A`, `S1-B`, `S2-A`, etc. (stage number + letter).

### Step 4: Identify the Backbone vs Walking Skeleton

- **Backbone** — the single most critical story per stage (marked with ★) — the minimum viable path
- **Walking skeleton** — the set of backbone stories stitched together — what v1 must deliver to be usable

### Step 5: Produce the Map

Output both a visual ASCII matrix and a structured story list.

## Output Format

**Format A: Visual Story Map**

```
STAGE 1: [Name]    │  STAGE 2: [Name]  │  STAGE 3: [Name]  │  STAGE 4: [Name]
───────────────────┼───────────────────┼───────────────────┼──────────────────
★ S1-A: [story]   │ ★ S2-A: [story]  │ ★ S3-A: [story]  │ ★ S4-A: [story]
  S1-B: [story]   │   S2-B: [story]  │   S3-B: [story]  │   S4-B: [story]
  S1-C: [story]   │   S2-C: [story]  │   S3-C: [story]  │
```

Walking skeleton (v1): S1-A → S2-A → S3-A → S4-A

**Format B: Structured Story List**

```
## Stage 1: [Name]
- ★ S1-A: As a [user], I [action] so that [benefit]   [BACKBONE]
- S1-B: As a [user], I [action] so that [benefit]
- S1-C: As a [user], I [action] so that [benefit]

## Stage 2: [Name]
- ★ S2-A: ...   [BACKBONE]
- S2-B: ...

[continue for all stages]

## Walking Skeleton (v1)
Stories: S1-A → S2-A → S3-A → S4-A
What ships: [1-2 sentence description of what this delivers]

## Release 2 Stories
Stories: S1-B, S2-B, S3-B, S4-B
What this adds: [1-2 sentence description]
```

## Example

**User**: "Map out the user journey for someone signing up and using our project management tool for the first time."

**Output (abbreviated)**:

```
STAGE 1: Discovery  │ STAGE 2: Signup    │ STAGE 3: Onboarding │ STAGE 4: Daily Use
────────────────────┼────────────────────┼─────────────────────┼───────────────────
★ S1-A: Views      │ ★ S2-A: Creates   │ ★ S3-A: Creates    │ ★ S4-A: Creates
  landing page     │   account with     │   first project     │   and assigns tasks
                   │   email            │                     │
  S1-B: Reads      │  S2-B: Verifies   │  S3-B: Invites     │  S4-B: Views
  case studies     │   email            │   teammates         │   task status
                   │                   │                     │
  S1-C: Starts     │  S2-C: Chooses    │  S3-C: Completes   │  S4-C: Gets
  free trial       │   plan             │   onboarding tour   │   daily digest
```

Walking skeleton: S1-A → S2-A → S3-A → S4-A

## Guidelines

- Happy path first — map the ideal journey, then add alternative paths (error states, cancellations, edge cases) as secondary rows
- Each story must be one independent thing a user does — avoid "user does X and then Y" in a single story
- The walking skeleton is the v1 scope — use it to drive initial epic creation
- Stories that appear in multiple stages are signals of a cross-cutting concern (like auth or permissions) — call those out
- After mapping, offer to run **epic-planner** on the walking skeleton stories to break them into epics
