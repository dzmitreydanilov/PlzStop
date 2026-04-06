# Expenses Tracking KMP App (`com.please.stop.app`)

Kotlin Multiplatform Mobile using Compose Multiplatform. All shared code lives in `commonMain`.

## Agent Behavior Requirements

MANDATORY: Before responding to ANY prompt, you MUST:
1. Check ALL available skills listed below.
2. Identify which skills apply to this prompt.
3. Read and follow EACH applicable skill.
4. ONLY THEN start your response.

### Available Skills (`/skills/`)

| Skill | When to apply |
|-------|---------------|
| `guidelines` | Always — general project conventions and standards |
| `code-style` | Any Kotlin code — null safety, layer rules, DI, collections |
| `mvi-patterns` | Screens, features, StateHolders, UI state, events |
| `navigation` | Adding screens, navigation flows, deep links, routes |
| `compose-ui-compose-multiplatform` | Any Compose UI work — screens, components, theming |
| `theming` | Styling, colors, spacing, typography, design tokens |
| `string-resources` | UI text, labels, content descriptions, a11y strings |
| `android-accessibility` | Any Android UI work, new screens, or composables |
| `coil-compose-multiplatform` | Image loading, avatars, thumbnails, or remote images |
| `compose-multiplatform-performance-audit` | Performance reviews, recomposition issues, lazy lists |
| `gradle-build-performance` | Build config changes, new modules, dependency updates |

### File-First Workflow

- **Always write results to local files** — never dump large code blocks, diffs, or generated content inline in the conversation.
- **Read files from disk** when you need context — do not ask the user to paste file contents.
- Keep conversation responses short: summarise what you did, reference the file path.

### Token Efficiency

- Be mindful of background token usage (conversation summarization, command processing).
- Avoid unnecessarily re-reading files already in context.
- Prefer targeted file reads (specific line ranges) over reading entire large files.
- Batch related edits into fewer tool calls where possible.

## Quick Reference

```bash
./gradlew detekt          # Lint — run before every task completion (max issues = 0)
```

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
