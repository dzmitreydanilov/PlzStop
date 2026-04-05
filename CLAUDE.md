# PlzStop — Project Guidelines

## UI State pattern

All screens use a `sealed interface` where **every state variant shares all UI properties**. Only `Error` adds `errorType: ErrorType`. Loading defaults everything to null/empty/false.

```kotlin
@Serializable
@Stable
sealed interface FooState {
    val title: String?
    val items: ImmutableList<ItemUiModel>
    val isEmpty: Boolean
    // ... all other UI props

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

In the state holder, `getErrorStateByResult` uses `state.value.toError(errorType)` with a `FooState.toError()` extension that copies all current props into `Error`. This keeps the UI rendering its last known content behind the error overlay.
