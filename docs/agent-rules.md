# Expenses Tracking KMP App (`com.please.stop.app`)

Kotlin Multiplatform Mobile using Compose Multiplatform. All shared code lives in `commonMain`.

This file is the canonical source of truth for repo-specific agent behavior.
Skill content lives in `docs/skills/`.
Generated wrappers mirror this file to `AGENTS.md`, `.claude/CLAUDE.md`, and `.codex/CODEX.md`.
Generated Claude Code and Codex skill access points use symlinks from `.claude/skills/` and `.codex/skills/`
to `docs/skills/`.

## Agent Behavior Requirements

MANDATORY: Before responding to ANY prompt, you MUST:
1. Check ALL available skills listed below.
2. Identify which skills apply to this prompt.
3. Read and follow EACH applicable skill.
4. ONLY THEN start your response.

### Available Skills (`docs/skills/`)

| Skill | When to apply |
|-------|---------------|
| `guidelines` | Always — general project conventions and standards |
| `code-style` | Any Kotlin code — null safety, layer rules, DI, collections |
| `kotlin-types-value-class` | Kotlin type declarations — choose `@JvmInline value class` vs `data class` |
| `kotlin-coroutines-structured-concurrency` | Coroutine scopes, launches, `runBlocking`, and suspend-call error handling |
| `kotlin-multiplatform-expect-actual` | Kotlin Multiplatform platform boundaries, `expect/actual`, and native interop |
| `mvi-patterns` | Screens, features, StateHolders, UI state, events |
| `navigation` | Adding screens, navigation flows, deep links, routes |
| `compose-state-authoring` | Local Compose state, `remember`, mutable state containers, and `@ReadOnlyComposable` |
| `compose-state-hoisting` | Where Compose UI state or UI logic should live |
| `compose-side-effects` | Compose side effects, flows, navigation, focus, analytics, and snackbar events |
| `compose-slot-api-pattern` | Reusable Compose components with caller-defined visual regions |
| `compose-modifier-and-layout-style` | Compose layout APIs, modifier parameters, and root layout wrappers |
| `compose-recomposition-performance` | Compose recomposition performance, compiler reports, and frame-rate state reads |
| `theming` | Styling, colors, spacing, typography, design tokens |
| `string-resources` | UI text, labels, content descriptions, a11y strings |
| `android-accessibility` | Any Android UI work, new screens, or composables |
| `gradle-build-performance` | Build config changes, new modules, dependency updates |
| `commit-message` | commit messages, finishing a task, describing changes |

### File-First Workflow

- **Always write results to local files** — never dump large code blocks, diffs, or generated content inline in the conversation.
- **Read files from disk** when you need context — do not ask the user to paste file contents.
- Keep conversation responses short: summarise what you did, reference the file path.

### Token Efficiency

- Be mindful of background token usage (conversation summarization, command processing).
- Avoid unnecessarily re-reading files already in context.
- Prefer targeted file reads (specific line ranges) over reading entire large files.
- Batch related edits into fewer tool calls where possible.
- Build artifacts, iOS SPM checkouts, node_modules, IDE files, and binaries are excluded via `.claudeignore`
  and `.codexignore` — do not read or reference those paths.
- **Always limit command output** — pipe through `tail -20`, `grep`, or use `--quiet`/`-q` flags. Never let raw Gradle or build output flood the context.

## Quick Reference

```bash
./gradlew detekt -q 2>&1 | tail -20   # Lint — run before every task completion (max issues = 0)
```

> Always use `-q` and pipe to `tail -N` or `grep` to limit output and save context tokens.

- **Branch:** `master` = production
- **Max line length:** 120 chars
- **Max cyclomatic complexity:** 10

## Architecture: MVI

```
User Action → Event
  → StateHolder.resolveEventResult()
    → UseCase → Repository
      → Result
  → transformIntoState()
    ├─ getStateByResult()   → new State
    └─ getNavigationByResult() → navigation side-effect
```

Screen initialisation: `bootstrap()` runs once, `collectFlows()` on each collection.

### Layers

| Layer        | Path                       | Returns              | Key types                          |
|--------------|----------------------------|----------------------|------------------------------------|
| Presentation | `features/*/presentation/` | Composables, State   | `*StateHolder`, `*State`, `*Event` |
| Domain       | `features/*/domain/`       | `Flow<Result>`       | `*UseCase`                         |
| Data         | `features/*/data/`         | `Flow<kotlin.Result>` | `*Repository`, `*ApiService`      |

### Key directories

- `/core/stateholder/` — StateHolder base classes, StateSaver
- `/core/models/` — Shared domain & UI models
- `/navigation/` — Navigation 3
- `/network/` — `flowFromSuspend`, `mapToResult`
- `/features/[name]/` — Feature modules (`data/`, `domain/`, `presentation/`)
- `/di/` — Koin root; each feature also has `/features/[name]/di/`
