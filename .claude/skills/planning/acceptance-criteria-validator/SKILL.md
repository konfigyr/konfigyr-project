---
name: acceptance-criteria-validator
description: >
  Quality-check acceptance criteria in tickets to ensure they're testable, specific, behavior-focused, and comprehensive. Use this whenever a user asks "review the acceptance criteria on this ticket", "are these ACs good enough?", "is this ticket ready to hand to dev?", or "check if we're missing edge cases." This skill prevents tickets from entering development with criteria that can't be tested or that will generate endless clarification back-and-forth.
---

# Acceptance Criteria Validator

## When to Use This Skill

Trigger on phrases like:
- "Review the acceptance criteria on this ticket"
- "Are these ACs good enough?"
- "Is this ticket ready for development?"
- "Check if we're missing any edge cases"
- "Can QA actually test this?"
- "Help me improve the ACs on [ticket]"

Run this before handing a ticket to development. It catches problems cheaply before they become expensive mid-sprint clarifications.

## What Makes a Good Acceptance Criterion

Each criterion must pass all four checks:

| Check         | Question to ask                                      | Fail example                         | Pass example                                       |
|---------------|------------------------------------------------------|--------------------------------------|----------------------------------------------------|
| **Testable**  | Can QA independently verify this passes or fails?    | "The form works well"                | "Submitting with empty required fields shows error message below each field" |
| **Specific**  | Is the observable outcome clearly described?         | "Users can see their data"           | "User sees a table with columns: Date, Amount, Status, and a Download button per row" |
| **Behavioral**| Does it describe WHAT, not HOW (no library/impl prescriptions)? | "Use React Query to fetch data" | "Data loads within 3 seconds on a 4G connection" |
| **Complete**  | Does the full AC set cover happy path + key edge cases? | Only happy path described | Happy path + empty state + error state + auth edge case |

## Workflow

### Step 1: Read Each Criterion

Parse the ticket's acceptance criteria one by one.

### Step 2: Apply the Four Checks

For each criterion, check: testable, specific, behavioral, complete.

### Step 3: Generate Per-Criterion Feedback

Mark each as:
- ✓ **Pass** — criterion is solid
- ✗ **Fail** — criterion fails one or more checks; explain why and suggest a rewrite
- ⚠ **Warn** — criterion is acceptable but could be clearer; offer an improvement

### Step 4: Identify Missing Coverage

After reviewing existing criteria, identify gaps:
- Error states not covered
- Authentication / authorization edge cases
- Empty states (no data)
- Mobile / responsive behavior (if UI)
- Performance or load constraints
- Data boundaries (max length, max items, allowed characters)

### Step 5: Provide a Readiness Score

Rate the ticket overall:
- **Ready to build** — all criteria pass, key edge cases covered
- **Needs minor work** — a few criteria need rewrites, no structural gaps
- **Not ready** — criteria are vague or missing; development will stall

## Output Format

```
## Acceptance Criteria Review: [Ticket Title]

### Criterion-by-Criterion Feedback

**AC 1**: "User can submit the contact form"
✗ FAIL — Not testable (what happens after submit? success? error?) and not specific.
→ Rewrite: "User sees a success message 'Your message has been sent' after submitting the form with valid data. The form fields are cleared after successful submission."

**AC 2**: "Form validates required fields"
⚠ WARN — Acceptable but vague. Which fields? What does validation look like?
→ Improve: "Submitting the form with any required field empty highlights that field in red and shows 'This field is required' below it. The form does not submit until all required fields are filled."

**AC 3**: "User receives a confirmation email"
✓ PASS — Observable and testable.

---

### Missing Coverage

The following scenarios are not addressed by any current criterion:

- [ ] **Error state**: What happens if the form submission fails (network error, server error)?
- [ ] **Spam/rate limiting**: What happens if the user submits the form multiple times quickly?
- [ ] **Mobile**: Is the form layout tested on small screens?
- [ ] **Long input**: Is there a character limit on the message field? What happens if exceeded?

---

### Readiness Score

**Needs minor work** — 1 failing criterion and 4 missing edge cases. Fix AC 1, improve AC 2, and add at least the error state coverage before handing to dev.
```

## Example of Common Failures and Fixes

| Original (bad)                              | Problem               | Fixed version                                                                   |
|---------------------------------------------|-----------------------|---------------------------------------------------------------------------------|
| "The dashboard loads fast"                  | Untestable, vague     | "Dashboard loads within 2 seconds on a 10 Mbps connection"                     |
| "Use a modal for the confirmation"          | Implementation, not behavior | "User is asked to confirm before deleting; canceling the confirmation stops the deletion" |
| "Handle errors gracefully"                  | Vague                 | "If the API call fails, user sees 'Something went wrong. Please try again.' with a retry button" |
| "The feature works on mobile"               | Not specific          | "On screens narrower than 768px, the sidebar collapses and is accessible via a hamburger menu" |

## Guidelines

- Distinguish failure reasons: "untestable" (too vague), "prescriptive" (tells devs how, not what), "incomplete" (missing edge cases)
- Always call out missing error states — they're the most commonly forgotten coverage area
- Behavioral criteria are about user-observable outcomes, not code structure
- After reviewing, offer to run **test-planning-generator** on the improved criteria to create QA test cases
