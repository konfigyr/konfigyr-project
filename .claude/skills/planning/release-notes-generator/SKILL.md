---
name: release-notes-generator
description: >
  Transform completed epics, tickets, and features into customer-facing or internal release notes that highlight user value and surface known limitations. Use this when a user asks "write release notes for [epic]", "generate release notes for v2.1", "create customer comms for the new features we shipped", or "draft the changelog." This skill produces three audience-appropriate formats in one pass: customer-facing notes, internal changelog, and a social media summary.
---

# Release Notes Generator

## When to Use This Skill

Trigger on phrases like:
- "Write release notes for [epic or version]"
- "Generate the changelog for this release"
- "Create customer comms for what we shipped"
- "Draft the v[X.Y] release notes"
- "Summarize what's new in this sprint"
- "What do we tell customers about this release?"

Use this after features have shipped or are in final QA. It's most effective when working from completed tickets or epics with accepted acceptance criteria.

## Workflow

### Step 1: Gather Shipped Work

Accept a list of completed epics, tickets, or a sprint summary. For each item, extract:
- What changed (feature, fix, improvement, removal)
- Who benefits (which user type, which workflow)
- User-facing impact (what they can now do, faster, or differently)
- Any known limitations or caveats
- Any breaking changes or migration requirements

### Step 2: Classify Each Item

Sort shipped items into categories:

- **New Features** — net new capabilities users didn't have before
- **Improvements** — enhancements to existing functionality
- **Bug Fixes** — notable fixes users experienced as problems
- **Breaking Changes** — anything requiring user action or migration
- **Deprecations** — functionality removed or no longer supported
- **Known Issues** — bugs or limitations shipping with the release

Breaking changes and known issues must always appear at the top of customer-facing notes.

### Step 3: Write for Each Audience

Three audiences require different language and depth:

- **Customer-facing**: business value, plain language, no implementation details, 1–2 sentences per item
- **Internal changelog**: technical detail, version numbers, PR references, implementation notes
- **Social media / announcement**: punchy, benefit-led, top 2–3 highlights only

### Step 4: Review for Tone

Customer-facing notes should:
- Lead with what users can DO now, not what was built
- Use plain language (no "we refactored", "we migrated", "we abstracted")
- Always surface breaking changes prominently and explain what users need to do
- Be honest about known issues — hiding them erodes trust

## Output Format

**Customer-Facing Release Notes**

```
# Release Notes — [Product Name] v[X.Y] — [Date]

> **Breaking changes in this release**: [brief callout if any — link to migration guide]

## What's New

### [Feature Name]
[1–2 sentences describing what users can now do and why it matters]

### [Feature Name 2]
[1–2 sentences]

---

## Improvements

### [Improvement Name]
[1 sentence: what's better, and how users notice it]

---

## Bug Fixes

- Fixed: [brief description of the user-facing issue that was resolved]
- Fixed: [brief description]

---

## Known Issues

- [Known issue 1]: [brief description + workaround if available]

---

## Breaking Changes

### [Breaking Change Title]
**Who is affected**: [user type / integration type]
**What changed**: [1 sentence]
**What to do**: [step-by-step migration action]
**Migration deadline**: [date if applicable]
```

**Internal Changelog**

```
## Changelog — v[X.Y] — [Date]

### Added
- [Feature]: [technical description] — PR #[XXX], [author]
- [Feature]: [technical description]

### Changed
- [Change]: [what changed and why] — closes #[ticket]

### Fixed
- [Bug]: [root cause and fix] — resolves #[ticket]

### Removed
- [Deprecated feature]: [migration path]

### Internal Notes
- [Migration scripts run]: [details]
- [Config changes required]: [details]
- [Rollback procedure]: [details]
```

**Social Media / Announcement Summary**

```
🚀 [Product] v[X.Y] is here!

✅ [Top benefit 1] — [1-line description]
✅ [Top benefit 2] — [1-line description]
✅ [Top benefit 3] — [1-line description]

[CTA: "Read the full release notes at [link]" or "Update now in [location]"]
```

## Example

**Input**: "We shipped: user CSV export, fixed slow dashboard loading, removed legacy API v1 endpoints (breaking), and dark mode is still in progress (known issue)."

**Output (customer-facing, abbreviated)**:

```
# Release Notes — Konfigyr v2.1 — June 2026

> **Breaking change**: Legacy API v1 endpoints have been removed. See migration guide below.

## What's New

### Export Your Data as CSV
You can now export your full configuration history as a CSV file directly from
Settings → Data → Export. Use it for auditing, reporting, or migrating to another system.

---

## Improvements

### Faster Dashboard Loading
The main dashboard now loads up to 60% faster for accounts with large configuration sets.

---

## Breaking Changes

### Legacy API v1 Removed
**Who is affected**: Integrations using `/api/v1/` endpoints
**What changed**: All v1 endpoints now return 410 Gone
**What to do**: Migrate to v2 API endpoints — see the migration guide at [link]
**Migration deadline**: This change is live in v2.1

---

## Known Issues

- Dark mode: Work in progress, not yet available. Expected in v2.2.
```

## Guidelines

- Breaking changes must be at the top — a user who misses them has a bad day; a user who sees them can prepare
- "We refactored X" is never in customer-facing notes; "X is now faster/more reliable" is
- Known issues are honest signals — hiding them makes support tickets worse
- Each new feature should answer "what can users do now that they couldn't before?"
- Social summaries are for top 2–3 items only — everything is not equally newsworthy
