---
name: test-planning-generator
description: >
  Take a ticket or epic and generate a structured test plan with test cases, scenarios, coverage areas, and QA strategy. Use this when a user asks "create a test plan for this epic", "what should QA test?", "generate test cases for [feature]", or "help me plan QA coverage." This skill bridges the gap between acceptance criteria and an executable QA plan — run it after acceptance-criteria-validator confirms the ticket is ready.
---

# Test Planning Generator

## When to Use This Skill

Trigger on phrases like:
- "Create a test plan for this epic/ticket"
- "What should QA test?"
- "Generate test cases for [feature]"
- "Help me plan QA coverage"
- "Is this ready for QA?"
- "What are the edge cases we should test?"

Run this after **acceptance-criteria-validator** confirms the ticket is ready. Test plans built on vague ACs will miss coverage; validate first, then plan.

## Workflow

### Step 1: Parse the Ticket

Extract from the ticket's description and acceptance criteria:
- The feature boundary (what's in scope for testing)
- User roles involved
- System integrations touched
- Any performance or load requirements
- Platform/device scope (web only? mobile? specific browsers?)

### Step 2: Define Test Strategy

Choose the testing approach:
- **Manual only**: exploratory flows, visual checks, one-off releases
- **Automated only**: pure logic, regression-safe, stable UI
- **Both** (default): automated for regression, manual for exploratory and edge cases

Flag any ACs that are hard to automate (visual checks, email delivery, async workflows).

### Step 3: Identify Test Scenarios

Group test cases into scenarios:
- **Happy path** — everything works as expected
- **Alternative paths** — valid but non-default flows
- **Error cases** — invalid input, failed API calls, missing permissions
- **Edge cases** — boundary values, empty states, concurrent actions
- **Regression areas** — existing features that this change might break

### Step 4: Write Test Cases

For each scenario, write 2–4 test cases in Given/When/Then format:

```
Given [initial state/context]
When [user action or system event]
Then [expected observable outcome]
```

### Step 5: Note Regression Risk

List related features or flows that should be re-tested after this change ships.

## Output Format

```
## Test Plan: [Ticket/Epic Title]

### Test Strategy
- Approach: [Manual / Automated / Both]
- Priority: [P1 smoke tests, P2 regression, P3 exploratory]
- Out of scope: [what is not being tested and why]
- Hard-to-automate items: [anything requiring manual verification]

---

### Test Scenarios

#### Scenario 1: Happy Path — [name]
Goal: Verify the expected flow works end-to-end

| # | Given | When | Then | Type |
|---|-------|------|------|------|
| TC-01 | User is logged in and on settings page | User clicks "Export Data" | Export confirmation dialog appears | Manual |
| TC-02 | User confirms export in dialog | System processes export | User sees "Your export is ready" and a download button | Auto |
| TC-03 | User clicks download button | Browser receives file | CSV file downloads with correct filename format: `export_[userId]_[date].csv` | Manual |

#### Scenario 2: Error Cases — [name]
Goal: Verify the system handles failures gracefully

| # | Given | When | Then | Type |
|---|-------|------|------|------|
| TC-04 | Export service is unavailable | User requests export | User sees "Export unavailable. Try again later." — no partial download | Auto |
| TC-05 | User is not authenticated | User hits export endpoint directly | User is redirected to login page | Auto |

#### Scenario 3: Edge Cases — [name]

| # | Given | When | Then | Type |
|---|-------|------|------|------|
| TC-06 | User has 0 records | User requests export | User sees "No data to export" message — no empty CSV downloads | Manual |
| TC-07 | User has 100,000+ records | User requests export | Export completes within 30 seconds; progress indicator shows during processing | Manual |

---

### Regression Areas

The following existing features should be spot-checked after this change:
- [ ] User profile page — ensure settings navigation is unaffected
- [ ] Admin data view — confirm admin can still access user data directly
- [ ] Email notifications — verify export completion email is not triggered unexpectedly

---

### Coverage Summary

| Area              | Covered? | Notes                            |
|-------------------|----------|----------------------------------|
| Happy path        | ✓        | TC-01 through TC-03              |
| Auth edge case    | ✓        | TC-05                            |
| Error state       | ✓        | TC-04                            |
| Empty state       | ✓        | TC-06                            |
| Performance       | Partial  | TC-07 manual only; no load test  |
| Mobile            | ✗        | Not in scope — web only          |
```

## Guidelines

- Happy path alone is not a test plan — edge cases and error states are where bugs hide
- Given/When/Then keeps test cases unambiguous and independently verifiable
- Flag hard-to-automate cases explicitly (email delivery, async jobs, visual rendering) so QA knows they're manual
- Regression areas are not optional — they prevent "this worked before you changed it" surprises
- Performance test cases should state concrete thresholds, not "it should be fast"
- After generating the test plan, offer to run **acceptance-criteria-validator** if any test case reveals an AC gap
