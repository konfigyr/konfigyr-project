---
name: requirements-analyzer
description: >
  Transform vague, scattered, or informal requirements into a structured specification document with a clear problem statement, success metrics, and explicit scope boundaries. Use this whenever a user says "we have this feature idea", "got a request from the customer", "here's what we need to build", or "I have a requirements document but it's all over the place." This is the right starting point before writing any epic or ticket.
---

# Requirements Analyzer

## When to Use This Skill

Trigger on phrases like:
- "We have this feature idea I need to structure"
- "Got a request from the customer, here's the email..."
- "Here's a doc with what we need to build"
- "Can you help me make sense of these requirements?"
- "I need to write a spec for [feature]"

Use this skill before **epic-planner** when the requirement is still fuzzy. If the user already has a clear, scoped request, skip directly to epic-planner or ticket-planner.

## Workflow

### Step 0: Load Project Context (conditional)

Before structuring the requirement, check whether you need system-wide context. Load
`.claude/skills/shared/project-overview/SKILL.md` if **any** of the following are true:

- The requirement mentions more than one module (namespace, vault, artifactory, kms, audit, account, feature)
- The requirement involves a cross-cutting concern (auth, events, encryption, API contracts)
- You cannot yet identify which module the requirement belongs to
- The requirement creates a dependency between services (frontend ↔ API, API ↔ identity)

Skip it for clearly scoped requirements that name a specific module and don't cross boundaries
("fix the DNS timeout in the verification strategy" — no cross-module context needed).

### Step 1: Gather Raw Input

Accept whatever the user provides: a paragraph, a Slack message, a bullet list, a customer email, or a rough doc. Extract as much as possible before asking questions.

### Step 2: Ask Targeted Clarifying Questions

Ask only for what's missing. Key dimensions:

- **Problem**: What pain is being solved? What happens today without this feature?
- **Audience**: Who specifically uses this? Internal team, paying customers, admins?
- **Context**: New capability, improvement to existing, or replacing something?
- **Success criteria**: How do we measure that this worked? (e.g., "checkout completion rate increases", "support tickets drop")
- **Constraints**: Timeline, tech stack, integration requirements, compliance?
- **Out of scope**: What is explicitly NOT included in this version?

Limit to 3–4 targeted questions. Don't interrogate — extract what's already there and ask only for what's missing.

### Step 3: Produce the Structured Specification

Organize findings into these sections:

1. **Problem Statement** — one paragraph: what is broken or missing, for whom, and why it matters now
2. **Target Users** — who benefits, any persona or role details
3. **Success Criteria** — measurable outcomes; at least one quantitative metric if possible
4. **Scope: In** — explicit list of what this requirement covers
5. **Scope: Out** — what is explicitly excluded from this effort
6. **Assumptions** — things taken as true that haven't been confirmed
7. **Open Questions** — unresolved decisions that someone needs to answer before development starts
8. **Dependencies** — systems, teams, or features this depends on

### Step 4: Confirm and Hand Off

After presenting the spec, ask: "Does this capture the requirement accurately? Any corrections or additions?" Once confirmed, offer to run **epic-planner** to break it into an epic and tickets.

## Output Format

```
# Requirements Specification: [Feature Name]

## Problem Statement
[1-2 paragraph description of the problem, who it affects, and the business impact]

## Target Users
- [User type 1]: [brief description of their need]
- [User type 2]: [brief description of their need]

## Success Criteria
- [Metric 1]: e.g., "Checkout abandonment rate drops below 15%"
- [Metric 2]: e.g., "Support tickets related to billing decrease by 30%"

## Scope

### In Scope
- [Feature or capability 1]
- [Feature or capability 2]

### Out of Scope
- [Explicitly excluded 1]
- [Explicitly excluded 2]

## Assumptions
- [Assumption 1]: e.g., "Users are already authenticated when they reach this flow"
- [Assumption 2]

## Open Questions
- [ ] [Question 1 — who needs to answer this?]
- [ ] [Question 2]

## Dependencies
- [Dependency 1]: e.g., "Requires payment gateway API to support refunds endpoint"
- [Dependency 2]
```

## Example

**Input**: "Our customers keep complaining they can't see their invoice history. We want to add a billing page."

**Output**:

```
# Requirements Specification: Customer Billing History Page

## Problem Statement
Customers currently have no self-serve way to view or download their invoices.
This forces them to contact support for every billing inquiry, creating support
overhead and reducing customer trust around billing transparency.

## Target Users
- Paying customers (any plan): need to download invoices for accounting
- Account admins: need to view full billing history for their organization

## Success Criteria
- Support tickets tagged "billing-invoice" decrease by 50% within 60 days
- At least 40% of active accounts visit the billing page within 90 days of launch

## Scope

### In Scope
- List of past invoices with date, amount, and status
- PDF download per invoice
- Filter by date range

### Out of Scope
- Editing or canceling invoices
- Payment method management (separate feature)
- Multi-currency display (v2)

## Assumptions
- Invoice data is already stored and retrievable from the billing service
- Users are authenticated before reaching the billing page

## Open Questions
- [ ] Should invoices older than 24 months be shown? (ask Finance)
- [ ] Do we need role-based access (only admins see billing)?

## Dependencies
- Billing service API must expose invoice history endpoint
```

## Guidelines

- Surface assumptions explicitly — hidden assumptions are the #1 cause of scope creep
- Out-of-scope is as important as in-scope; explicitly name what's excluded
- Success criteria should be measurable, not aspirational ("users can access billing" is not a metric)
- Open questions must be answered before dev starts — flag who owns each one
- Don't add tech stack recommendations unless the user asked; keep it business-focused
