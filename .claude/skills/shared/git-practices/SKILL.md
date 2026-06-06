---
name: git-practices
description: Git commit message conventions, PR structure, code review practices, and when to ask before changing. Use for all version control and collaboration workflow.
---

# Git Practices

## Commit Messages

**Format:** Clear, imperative, concise

```
# ✓ Good
Add namespace quota validation
Update jOOQ generated code after schema change
Fix N+1 query in vault list endpoint

# ✗ Bad
fixed stuff
WIP
Update
changes
```

**Structure:**
- **First line:** 50 characters max, imperative mood
- **Blank line:** Separate from body
- **Body:** Explain WHY (not what or how)
- **Closes #123:** Link to issue (if applicable)

**Example:**

```
Add namespace quota validation

The namespace module now validates that a namespace 
doesn't exceed the quota set in the feature flag. 
This prevents runaway consumption in multi-tenant deployments.

Closes #456
```

## Commit Scope

**One logical change per commit.** Not one change per file, one per line.

```
# ✓ Good: Logical units
1st commit: Add namespace quota field to database
2nd commit: Implement quota validation in NamespaceManager
3rd commit: Add REST endpoint to update quota

# ✗ Bad: Too many unrelated changes
1st commit: Add field, validation, endpoint, AND audit logging
```

## Squashing Commits

Before merging, squash related commits:

```bash
# Before PR: 5 commits for one feature
git log --oneline
abc1234 Fix typo in constant
abc1235 Rename variable
abc1236 Add namespace validation
abc1237 Update tests
abc1238 Add OpenAPI docs

# After squashing: 1 logical commit
git rebase -i HEAD~5
# Mark all but first as 'squash'
# Result: 1 commit "Add namespace validation"
```

## Pull Request Structure

### Title
Clear summary, not a question:

```
# ✓ Good
Add namespace quota validation
Fix N+1 query in vault endpoint
Update Spring Boot to 3.5.1

# ✗ Bad
Fixed issue
Update code
What about namespaces?
```

### Description

Template:

```markdown
## What
Brief description of the change.

## Why
Business reason or technical justification. 

## How
High-level approach (don't explain code).

## Testing
How was this tested? Which scenarios?

## Checklist
- [ ] Code compiles without warnings
- [ ] Tests pass (./gradlew test or npm run test:ci)
- [ ] No unused imports
- [ ] Documentation updated (if API changes)
- [ ] Commit history is clean
```

### Review Checklist

As **author:**
- [ ] PR title is clear and descriptive
- [ ] Description explains the "why"
- [ ] Changes are logically grouped
- [ ] All tests pass
- [ ] No unrelated changes ("cleanup" commits)

As **reviewer:**
- [ ] Understand the business reason
- [ ] Code follows project conventions
- [ ] No hardcoded values
- [ ] Tests cover happy path + errors
- [ ] No over-engineering
- [ ] Performance implications considered

## When to Ask Before Changing

**Always ask upfront; don't implement then ask for permission:**

### Architecture Questions
- "Should this be a new aggregate or part of existing one?"
- "Which module should own this logic?"
- "Is this a breaking change?"

### Design Questions
- "Should this be an event or a direct service call?"
- "What's the scope for this endpoint?"
- "Do we need to add a table or can we reuse existing ones?"

### Code Quality Questions
- "Should I refactor this module?"
- "Is this performance critical?"
- "Should we add a caching layer?"

### Maintenance Questions
- "Should I update the README?"
- "Does this need documentation?"
- "Should we version this endpoint?"

## Merge Strategy

**Squash and merge** for small PRs:
```bash
# One commit per feature
git merge --squash feature/namespace-quota
git commit -m "Add namespace quota validation"
```

**Rebase and merge** for clean history:
```bash
# Keep all commits if they're already clean
git rebase main
git push -f
# Then merge without squash
```

**Don't use merge commits:**
```bash
# ✗ Avoid
git merge feature  # Creates merge commit
```

## Reverting Changes

If you must revert:

```bash
# Revert a commit (create new commit that undoes it)
git revert <commit-hash>

# Revert a PR
git revert -m 1 <merge-commit-hash>

# Don't force push to shared branch:
git push origin main  # ✓ Safe
git push -f origin main  # ✗ Never on shared branches
```

## Branching Convention

```
feature/namespace-quota              Feature
bugfix/vault-encryption              Bug fix
docs/add-architecture-guide          Documentation
chore/update-spring-boot             Maintenance
```

## Local Development

```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes, commit
git add .
git commit -m "First logical chunk"
git commit -m "Second logical chunk"

# Before PR: rebase and squash if needed
git rebase main
# OR
git rebase -i HEAD~3  # Squash last 3 commits

# Push
git push -u origin feature/my-feature

# Create PR in GitHub
# After review: merge with squash
```

## Handling Merge Conflicts

```bash
# Rebase your branch on main
git fetch origin
git rebase origin/main

# If conflicts:
# 1. Edit files to resolve
# 2. Stage resolved files
git add <resolved-files>

# 3. Continue rebase
git rebase --continue

# 4. Force push (safe if feature branch is yours)
git push -f origin feature/my-feature
```

## Code Review Comments

**Be kind and constructive:**

```
# ✓ Good
This N+1 query could be expensive with large datasets.
Consider using an EXISTS subquery: [example]

# ✗ Bad
This is wrong. Use EXISTS.
```

**Accept suggestions gracefully:**
```
# ✓ Good
You're right, that's cleaner. I'll update.

# ✗ Bad
That doesn't matter.
```

## Amending Commits

If you forgot something small:

```bash
# Make change
git add .

# Amend last commit (don't create a new one)
git commit --amend --no-edit

# Force push to your branch
git push -f origin feature/my-feature

# Don't do this on main or after PR is merged!
```

## Verification Checklist

- [ ] Commit messages are clear and imperative
- [ ] PR description explains "why"
- [ ] One logical change per PR (not everything)
- [ ] All tests pass before requesting review
- [ ] No large files or secrets committed
- [ ] Code follows project conventions
- [ ] Documentation updated if needed
- [ ] Commit history is clean (squashed if needed)

