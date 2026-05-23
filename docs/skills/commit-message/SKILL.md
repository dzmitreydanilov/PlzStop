---
name: commit-message
description: Generate a commit message based on staged or unstaged changes without actually committing. Use this skill whenever the user asks to "write a commit message", "generate a commit", "what should I commit", "describe my changes", "prepare a commit", or mentions anything related to git commit messages. Also trigger when the user says "commit" in the context of finishing work on a feature or fix. NEVER run `git commit` — only produce the message.
---

# Commit Message Generator

Generate a conventional commit message from the current git diff. **Never run `git commit`.**

## Workflow

1. **Detect changes** — run the diff commands below and read the output.
2. **Analyse** — identify what changed: new files, modifications, deletions, renames. Group by intent.
3. **Write the message** — follow the format rules below.
4. **Save to file** — write the message to `.commit-msg` in the repo root.
5. **Tell the user** — print a short summary and the file path. The user can then use it with `git commit -F .commit-msg` if they choose.

## Detecting Changes

```bash
# Prefer staged changes; fall back to unstaged
DIFF=$(git diff --cached --stat)
if [ -z "$DIFF" ]; then
  DIFF=$(git diff --stat)
  echo "No staged changes found — showing unstaged changes."
fi

# Full diff for analysis (limit to avoid huge output)
git diff --cached -- . ':(exclude)*.lock' ':(exclude)*.generated.*' | head -500
# If nothing staged:
git diff -- . ':(exclude)*.lock' ':(exclude)*.generated.*' | head -500
```

If both diffs are empty, tell the user there are no changes to describe.

## Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>
```

### Rules

- **type**: `feat`, `fix`, `refactor`, `chore`, `docs`, `style`, `test`, `perf`, `ci`, `build`
- **scope**: the feature or module affected (e.g., `articles`, `navigation`, `di`). Omit if changes span many modules.
- **subject**: imperative mood, lowercase, no period, max 72 chars.
- **body**: optional — include only when the "what" isn't obvious from the subject. Explain *why*, not *what*. Wrap at 72 chars. Use bullet points sparingly.
- **Breaking changes**: add `!` after scope — `feat(api)!: remove legacy endpoint`

### Choosing the type

| Changed | Type |
|---------|------|
| New user-facing behaviour | `feat` |
| Bug fix | `fix` |
| Code restructure, no behaviour change | `refactor` |
| Dependencies, build config, CI | `chore` / `build` / `ci` |
| Tests added or fixed | `test` |
| Performance improvement | `perf` |
| KDocs, README, comments | `docs` |
| Formatting, imports, whitespace | `style` |

### Multi-scope changes

If changes span unrelated areas, prefer a single commit message that captures the primary intent. Don't try to describe every file — focus on the *purpose* of the change.

## Examples

**Simple feature:**
```
feat(expenses): add category filter to expense list
```

**Fix with context:**
```
fix(network): retry failed requests on timeout

The previous implementation silently dropped timed-out requests,
causing missing data in the expense list. Now retries up to 3 times
with exponential backoff.
```

**Refactor:**
```
refactor(di): extract expense module into separate Koin definition
```

**Multiple related changes:**
```
feat(symptoms): add symptom tracking flow

Introduces SymptomRepository, CreateSymptomUseCase, and the
SymptomsListScreen with full MVI wiring. Includes Koin module
registration and navigation integration.
```

## Output

Save the final message to `.commit-msg` in the repo root:

```bash
cat > .commit-msg << 'COMMIT_MSG'
<the generated message here>
COMMIT_MSG
```

Then tell the user:
- The generated message (quoted)
- How to use it: `git commit -F .commit-msg`
- That they can edit it before committing

## Constraints

- **NEVER** run `git commit`, `git push`, or any command that mutates git history.
- **NEVER** stage or unstage files (`git add`, `git reset`).
- Only read git state — `git diff`, `git status`, `git log` are fine.