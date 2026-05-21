    q---
name: code-style
description: Code style and layer rules — null safety, DI conventions, repository/use-case/StateHolder patterns, collections, documentation. Use when writing or reviewing any Kotlin code in this project.
---

# Code Style

## Null safety

Never use `!!`. Prefer `?: error("reason")`, `?.let { }`, or `?: return`.

## Always use Named arguments
For functions that have more than 2 parameters, or any parameters of the same type, use named arguments at the call site for clarity.

## Layer rules

### DI modules
Wiring only — no conditional or business logic. Push decisions into the classes themselves.

### Repositories
- Return `kotlin.Result<T>` for all suspend functions.
- Handle errors internally (wrap in `runCatching` or try/catch).

### Use cases (suspend)
- Inject `CoroutineDispatcher` (IO) via constructor.
- Wrap body in `withContext(ioDispatcher)`.
- Use `result.fold(onSuccess = {}, onFailure = {})` to map repository results to domain results.

### Use cases (Flow-based observe)
- Use `.catch` for error handling.
- No dispatcher injection needed — repository handles `flowOn`.

### StateHolders
- Main thread only — never inject a dispatcher.
- Inject with `koinViewModel()`.

## Error types

5 sealed variants in `ErrorType`: `Authentication`, `Server(statusCode)`, `Request(statusCode)`, `Network`, `Unknown`.

Convert from exceptions via `Throwable.toErrorType()`.

## Collections

Use `kotlinx.collections.immutable` (`ImmutableList`, `persistentListOf()`) in all state and UI models.

## Documentation

- KDocs on all public APIs.
- Do **not** add obvious comments (`// Create repository`). Only comment complex logic, non-obvious decisions, or workarounds.

## Other

- Deprecate before removing public APIs.
- Use granular Gradle tasks (never `assemble` on full project).
- Platform code: `nativeMain` for native, `appleMain` for Apple APIs.
- Max line length: 120 chars.
- Max cyclomatic complexity: 10.
