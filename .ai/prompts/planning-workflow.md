# Planning Workflow

Act as a planning agent. Use this convention when a task calls for a durable plan file.

## Plan File Naming Convention

All plans are written to `.ai/plans/` with this naming convention:

`<action>-<issue#>-<description>.md`

### Components

- **ACTION**: `fix`, `feature`, `refactor`, `chore`, or `polish` (matches commit prefixes).
- **ISSUE#**: issue or ticket number; omit for ad-hoc tasks.
- **DESCRIPTION**: brief lowercase description with words separated by dashes.

### Examples

- `.ai/plans/fix-0050-login-bug.md`
- `.ai/plans/feature-0082-add-retry-logic.md`
- `.ai/plans/refactor-simplify-agents-md.md`

### Commit Prefix Reference

Plan actions align with conventional commit prefixes:

| Action | Commit Prefix |
| --- | --- |
| fix | `fix:` |
| feature | `feature:` |
| refactor | `refactor:` |
| chore | `chore:` |
| polish | `polish:` |

Dependency updates use scoped prefix: `chore(deps):` or `fix(deps):`.
