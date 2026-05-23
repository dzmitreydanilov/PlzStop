---
name: mvi-patterns
description: MVI architecture patterns for this project — StateHolder, UI State sealed interface, ScreenOverlayContainer, ActionTracking, feature scaffolding. Use when creating or modifying screens, features, state holders, or event handling.
---

# MVI Patterns

## UI State — sealed interface with shared properties

Every state variant exposes **all** UI properties. Only `Error` adds `errorType`. `Loading` defaults everything to `null`/empty/`false`.

```kotlin
@Serializable
@Stable
sealed interface FooState {
    val title: String?
    val items: ImmutableList<ItemUiModel>
    val isEmpty: Boolean

    data object Loading : FooState {
        override val title: String? = null
        override val items: ImmutableList<ItemUiModel> = persistentListOf()
        override val isEmpty: Boolean = true
    }

    data class Content(
        override val title: String,
        override val items: ImmutableList<ItemUiModel>,
        override val isEmpty: Boolean,
    ) : FooState

    data class Error(
        val errorType: ErrorType,
        override val title: String?,
        override val items: ImmutableList<ItemUiModel>,
        override val isEmpty: Boolean,
    ) : FooState
}
```

Provide a `FooState.toError(errorType)` extension that copies all current props into `Error`, so the UI keeps rendering last known content behind the error overlay.

## ScreenOverlayContainer — required on every screen

Because all UI properties live on the sealed interface, **every state variant (including `Error`) carries the last-known data**. The screen content renders unconditionally — no `when` branching needed inside `ScreenOverlayContainer`. The overlay handles the error display on top.

```kotlin
// 1. Define asOverlay in the screen file
internal val FooState.asOverlay: ScreenOverlay?
    @Composable get() = when (this) {
        is FooState.Error -> ScreenOverlay.Error(type = errorType)
        else -> null
    }

// 2. Wrap content — always render, never branch by state variant
ScreenOverlayContainer(
    overlay = state.asOverlay,
    onDismiss = { stateHolder.processEvent(FooEvent.DismissError) },
    onRetry = { stateHolder.processEvent(FooEvent.Retry) },
) {
    FooContent(
        state = state,         // accepts FooState, NOT FooState.Content
        onEvent = stateHolder::processEvent,
    )
}
```

**Key rule:** `FooContent` must accept `FooState` (the sealed interface), not `FooState.Content`. This way both `Content` and `Error` states render the same UI, with `Error` showing the overlay on top of preserved content. Do NOT add `when`/`is` checks inside `ScreenOverlayContainer` to filter by state variant.

Behaviour: `ErrorType.Network` → full-screen dialog (close + retry). Other errors → error snackbar. `ScreenOverlay.Message` → info snackbar.


# Loading Indicator
Use `DisplayFullScreenProgress` when the screen is in `Loading` state. Because `Loading` is a variant of the same sealed interface, it can be rendered unconditionally inside `ScreenOverlayContainer` without branching.

## StateHolder

Required overrides:
- `resolveEventResult()` — Event → UseCase invocation
- `getStateByResult()` — pure state transformation (preserve previous data on errors)
- `getNavigationByResult()` — Result → navigation side-effect
- `handleRetry()` — retry via `ActionTrackingStateHolder`

Screen init: `bootstrap()` runs once, `collectFlows()` on each collection.

Results implementing `core.models.domain.ErrorResult` are routed through `getErrorStateByResult()` which calls `state.value.toError(errorType)`.

## ActionTrackingStateHolder — automatic retry

Extend `ActionTrackingStateHolder<S, E, T>` instead of `StateHolder` when the screen needs retry for specific user actions.

```kotlin
// 1. Define action types
sealed interface FooActions : LastAction {
    data class Save(val id: Long) : FooActions
    data object Delete : FooActions
}

// 2. Override in StateHolder
override fun mapEventToOperation(event: FooEvent): FooActions? = when (event) {
    is FooEvent.Save -> FooActions.Save(event.id)
    is FooEvent.Delete -> FooActions.Delete
    else -> null
}

override fun handleRetry(operation: FooActions?): Flow<Result> = when (operation) {
    is FooActions.Save -> saveUseCase(operation.id)
    is FooActions.Delete -> deleteUseCase()
    else -> reloadUseCase()
}

// 3. Wire retry event
override fun resolveEventResult(event: FooEvent): Flow<Result> = when (event) {
    is FooEvent.Retry -> handleRetry(lastAction)
    // ...
}
```

## UI Model Mapping

Domain → UI conversion uses private extension functions inside the StateHolder file:

```kotlin
private fun DomainModel.toUiModel(): FooUiModel = FooUiModel(
    id = id,
    displayName = name,
    formattedAmount = formatCurrency(amountMinorUnits, currency),
)
```

Called from `getStateByResult()`. Always end with `.toImmutableList()` for collections.

## Adding a Feature

1. **Data** — `features/[name]/data/` → Repository impl + ApiService
2. **Domain** — `features/[name]/domain/` → UseCase + sealed Result
3. **Presentation** — `features/[name]/presentation/` → StateHolder + State + Event + Screen
4. **DI** — `features/[name]/di/` → Koin module, register in `di/AppModule.kt`

## Network Utilities

- `flowFromSuspend(...)` — convert suspend → `Flow<T>`
- `mapToResult(...)` — convert `RestResponse<T>` → `Flow<Result<T>>`
