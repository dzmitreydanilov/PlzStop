# Category Deletion with Expense Reassignment — Implementation Plan

The feature intercepts category deletion, checks whether any active expenses reference the category, and either shows a simple confirmation dialog (no expenses) or a three-option dialog (move / soft-delete all / cancel). For subcategories the existing single-confirmation path is unchanged because the FK is already `SET_NULL`.

---

## 1. DAO Layer

### 1.1 `ExpenseDao` — two new suspend methods

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/core/db/dao/ExpenseDao.kt`

```kotlin
@Query("SELECT COUNT(*) FROM expense WHERE categoryId = :categoryId AND isDeleted = 0")
suspend fun countActiveByCategory(categoryId: Long): Int

@Query("UPDATE expense SET categoryId = :targetCategoryId WHERE categoryId = :sourceCategoryId AND isDeleted = 0")
suspend fun reassignCategory(sourceCategoryId: Long, targetCategoryId: Long)

@Query("UPDATE expense SET isDeleted = 1 WHERE categoryId = :categoryId AND isDeleted = 0")
suspend fun softDeleteByCategory(categoryId: Long)
```

No migration is needed — these are query-only additions on existing columns.

---

## 2. Repository Layer

### 2.1 `CategoriesRepository` interface

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/domain/repository/CategoriesRepository.kt`

Add two new suspending functions:

```kotlin
suspend fun countExpensesForCategory(categoryId: Long): Result<Int>
suspend fun deleteCategoryWithExpenses(categoryId: Long, action: ExpenseDeletionAction): Result<Unit>
```

Where `ExpenseDeletionAction` is a new sealed interface in the domain layer (see §3).

Keep the existing `deleteCategory(id)` — it stays for the no-expenses fast path and will be called internally by `deleteCategoryWithExpenses`.

### 2.2 `CategoriesRepositoryImpl`

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/data/repository/CategoriesRepositoryImpl.kt`

- Inject `ExpenseDao` in the constructor (alongside `CategoryDao`).
- Implement `countExpensesForCategory`: delegates to `expenseDao.countActiveByCategory(categoryId)` inside `runCatching`.
- Implement `deleteCategoryWithExpenses`:
  - `ExpenseDeletionAction.MoveToCategory(targetId)` → call `expenseDao.reassignCategory(categoryId, targetId)` then `categoryDao.deleteById(categoryId)` in a single `runCatching` block (Room handles the transaction implicitly because both queries run on the same connection; if atomicity is required, extract into a `@Transaction` method on a new DAO or use `withTransaction`).
  - `ExpenseDeletionAction.DeleteExpenses` → call `expenseDao.softDeleteByCategory(categoryId)` then `categoryDao.deleteById(categoryId)`.

> Note on atomicity: reassign + delete category should be atomic. Use `database.withTransaction { }` inside the `runCatching` block. `AppDatabase` is already available in the DI graph.

The constructor will change to:

```kotlin
class CategoriesRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val expenseDao: ExpenseDao,         // NEW
    private val featureFlags: FeatureFlags,
    private val ioDispatcher: CoroutineDispatcher,
) : CategoriesRepository
```

---

## 3. Domain Layer

### 3.1 New sealed interface `ExpenseDeletionAction`

**New file:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/domain/model/ExpenseDeletionAction.kt`

```kotlin
sealed interface ExpenseDeletionAction {
    data class MoveToCategory(val targetCategoryId: Long) : ExpenseDeletionAction
    data object DeleteExpenses : ExpenseDeletionAction
}
```

### 3.2 `DeleteCategoryUseCase` — extended

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/domain/usecase/DeleteCategoryUseCase.kt`

Replace the current single `invoke(id)` with two entry points called in sequence by the StateHolder:

```kotlin
suspend fun checkExpenses(categoryId: Long): Result
suspend operator fun invoke(categoryId: Long, action: ExpenseDeletionAction): Result
```

`Result` sealed interface gains:

```kotlin
sealed interface Result : DomainResult {
    data class HasExpenses(val count: Int) : Result          // NEW — triggers dialog
    data object NoExpenses : Result                          // NEW — triggers simple confirm
    data object Success : Result
    data class Failure(override val errorType: ErrorType) : Result, ErrorResult
}
```

`checkExpenses` calls `repository.countExpensesForCategory(categoryId)` and emits `HasExpenses(count)` or `NoExpenses`.

`invoke(categoryId, action)` calls `repository.deleteCategoryWithExpenses(categoryId, action)`.

---

## 4. State Layer

### 4.1 `CategoriesState`

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/CategoriesState.kt`

Add a new property to the sealed interface and all variants:

```kotlin
val deletionDialog: CategoryDeletionDialog?
```

- `Loading` → `null`
- `Content` and `Error` → part of constructor, default `null`

Add `CategoryDeletionDialog` as a `@Serializable` sealed interface in the same file:

```kotlin
@Serializable
sealed interface CategoryDeletionDialog {
    val categoryId: Long

    /** No expenses — show simple "are you sure?" */
    @Serializable
    data class SimpleConfirm(override val categoryId: Long) : CategoryDeletionDialog

    /**
     * Category has expenses — show three-option dialog.
     * [availableTargets] is the list of other categories to move expenses to.
     */
    @Serializable
    data class WithExpenses(
        override val categoryId: Long,
        val expenseCount: Int,
        @Serializable(with = ImmutableListSerializer::class)
        val availableTargets: ImmutableList<CategoryTargetUiModel>,
    ) : CategoryDeletionDialog
}

@Stable
@Serializable
data class CategoryTargetUiModel(val id: Long, val name: String, val iconKey: String)
```

`availableTargets` is derived from `state.categories` (excluding the category being deleted) at the time the dialog is shown — computed in the StateHolder, not fetched from a new use case.

Update `toError()` extension and all state-copying helpers to thread `deletionDialog` through.

### 4.2 `CategoriesEvent`

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/CategoriesEvent.kt`

Replace the current single-step `DeleteCategoryClicked` with a multi-step flow:

```kotlin
// Step 1: user taps delete icon — triggers expense count check
data class DeleteCategoryClicked(val categoryId: Long) : CategoriesEvent   // already exists

// Step 2a: user confirms simple deletion (no expenses)
data class ConfirmDeleteCategory(val categoryId: Long) : CategoriesEvent

// Step 2b: user picks "move expenses" — dialog selection
data class ConfirmDeleteCategoryMoveExpenses(
    val categoryId: Long,
    val targetCategoryId: Long,
) : CategoriesEvent

// Step 2c: user picks "delete expenses too"
data class ConfirmDeleteCategoryDeleteExpenses(val categoryId: Long) : CategoriesEvent

// Step 2d / cancel: dismisses any deletion dialog
data object DismissDeletionDialog : CategoriesEvent
```

---

## 5. StateHolder

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/CategoriesStateHolder.kt`

### 5.1 `resolveEventResult` — new branches

```
DeleteCategoryClicked     → handleDeleteCategoryClicked(categoryId)
ConfirmDeleteCategory     → handleConfirmDeleteNoExpenses(categoryId)
ConfirmDeleteCategoryMoveExpenses → handleConfirmDeleteMoveExpenses(categoryId, targetId)
ConfirmDeleteCategoryDeleteExpenses → handleConfirmDeleteExpenses(categoryId)
DismissDeletionDialog     → flowOf(CategoriesResult.HideDeletionDialog)
```

`handleDeleteCategoryClicked` emits `deleteCategoryUseCase.checkExpenses(categoryId)` which returns `HasExpenses` or `NoExpenses`.

`handleConfirmDelete*` functions each emit `deleteCategoryUseCase(categoryId, action)`.

### 5.2 Internal `CategoriesResult` additions

```kotlin
data object HideDeletionDialog : CategoriesResult
```

No new `ShowDeletionDialog` result is needed — the `HasExpenses` and `NoExpenses` use-case results directly drive state.

### 5.3 `getStateByResult` — new branches

```kotlin
is DeleteCategoryUseCase.Result.HasExpenses -> {
    val targets = previous.categories
        .filter { it.id != /* categoryId needed */ }
        .map { CategoryTargetUiModel(it.id, it.name, it.iconKey) }
        .toImmutableList()
    previous.withDeletionDialog(
        CategoryDeletionDialog.WithExpenses(categoryId, result.count, targets)
    )
}
is DeleteCategoryUseCase.Result.NoExpenses -> {
    previous.withDeletionDialog(CategoryDeletionDialog.SimpleConfirm(categoryId))
}
is DeleteCategoryUseCase.Result.Success -> previous.withDeletionDialog(null)
is DeleteCategoryUseCase.Result.Failure -> previous.withDeletionDialog(null).toError(result.errorType)
is CategoriesResult.HideDeletionDialog -> previous.withDeletionDialog(null)
```

`withDeletionDialog(dialog)` follows the same pattern as existing helpers (`updateSheet`, etc.):

```kotlin
private fun CategoriesState.withDeletionDialog(
    dialog: CategoryDeletionDialog?,
): CategoriesState = when (this) {
    is CategoriesState.Content -> copy(deletionDialog = dialog)
    is CategoriesState.Error -> copy(deletionDialog = dialog)
    CategoriesState.Loading -> this
}
```

**Problem with `categoryId` in `getStateByResult`:** `HasExpenses`/`NoExpenses` results must carry the `categoryId` so the dialog can be populated. Add `val categoryId: Long` to both result types.

### 5.4 Cyclomatic complexity

`resolveEventResult` gains 4 new branches. The existing `@Suppress("CyclomaticComplexity")` already covers it, but split the when block by extracting `handleDeletionEvent(event)` if the suppression limit is exceeded after addition.

---

## 6. UI Layer

### 6.1 `CategoriesScreen.kt`

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/ui/CategoriesScreen.kt`

Inside `CategoriesSheets` (or a new sibling composable `CategoryDeletionDialogs`), add:

```kotlin
when (val dialog = state.deletionDialog) {
    is CategoryDeletionDialog.SimpleConfirm -> SimpleDeleteCategoryDialog(
        onConfirm = { onEvent(CategoriesEvent.ConfirmDeleteCategory(dialog.categoryId)) },
        onDismiss = { onEvent(CategoriesEvent.DismissDeletionDialog) },
    )
    is CategoryDeletionDialog.WithExpenses -> DeleteCategoryWithExpensesDialog(
        dialog = dialog,
        onMoveExpenses = { targetId ->
            onEvent(CategoriesEvent.ConfirmDeleteCategoryMoveExpenses(dialog.categoryId, targetId))
        },
        onDeleteExpenses = {
            onEvent(CategoriesEvent.ConfirmDeleteCategoryDeleteExpenses(dialog.categoryId))
        },
        onDismiss = { onEvent(CategoriesEvent.DismissDeletionDialog) },
    )
    null -> Unit
}
```

### 6.2 New composable `SimpleDeleteCategoryDialog`

Private composable in `CategoriesScreen.kt`. Uses `AlertDialog` from Material 3:

- Title: `categories_delete_confirm_title`
- Body: `categories_delete_confirm_message`
- Confirm button (destructive style): `categories_delete`
- Dismiss button: `add_expense_cancel`

### 6.3 New composable `DeleteCategoryWithExpensesDialog`

Private composable in `CategoriesScreen.kt`. Uses `AlertDialog`:

- Title: `categories_delete_has_expenses_title`
- Body: `categories_delete_has_expenses_message` (with `%1$d` for expense count)
- Three buttons as a `Column` inside `buttons` slot or use custom content area:
  1. "Move to another category" button → opens inline dropdown (or a second dialog state, see below)
  2. "Delete expenses too" button
  3. "Cancel" button
- For the "Move" action: show a `DropdownMenu` or `ExposedDropdownMenuBox` listing `dialog.availableTargets` before the user can confirm. The selection state (`selectedTargetId`) is local `remember` state inside this composable — it does not need to go into `CategoriesState`.

> Keep the dialog self-contained: local `var selectedTargetId by remember { mutableStateOf<Long?>(null) }`. Only emit the event when a target is selected and confirmed.

---

## 7. String Resources

**File:** `composeApp/src/commonMain/composeResources/values/strings.xml`

```xml
<!-- Category deletion — simple confirm -->
<string name="categories_delete_confirm_title">Delete category?</string>
<string name="categories_delete_confirm_message">This category will be permanently removed.</string>

<!-- Category deletion — has expenses -->
<string name="categories_delete_has_expenses_title">This category has expenses</string>
<string name="categories_delete_has_expenses_message">%1$d expense(s) are assigned to this category. What would you like to do?</string>
<string name="categories_delete_move_expenses">Move to another category</string>
<string name="categories_delete_delete_expenses">Delete expenses too</string>
<string name="categories_delete_select_target">Select category</string>
```

No new accessibility strings are needed — the `AlertDialog` handles its own accessibility role, and `IconButton` already has `content_desc_delete_category`.

---

## 8. DI Module

**File:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/di/CategoriesModule.kt`

Update the `CategoriesRepository` single binding to pass `expenseDao`:

```kotlin
single<CategoriesRepository> {
    CategoriesRepositoryImpl(
        categoryDao = get<AppDatabase>().categoryDao(),
        subcategoryDao = get<AppDatabase>().subcategoryDao(),
        expenseDao = get<AppDatabase>().expenseDao(),   // NEW
        featureFlags = get(),
        ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
    )
}
```

No other DI changes — `DeleteCategoryUseCase` factory already injects `CategoriesRepository` and `ioDispatcher`.

---

## 9. Implementation Order

1. **DAO** — add `countActiveByCategory`, `reassignCategory`, `softDeleteByCategory` to `ExpenseDao`.
2. **Domain model** — create `ExpenseDeletionAction`.
3. **Repository interface** — add `countExpensesForCategory` and `deleteCategoryWithExpenses`.
4. **Repository impl** — inject `ExpenseDao`, implement both new methods with `database.withTransaction` for the two-step delete.
5. **Use case** — extend `DeleteCategoryUseCase` with `checkExpenses` and update `Result` sealed interface with `HasExpenses(categoryId, count)`, `NoExpenses(categoryId)`.
6. **State** — add `deletionDialog: CategoryDeletionDialog?` to `CategoriesState`, add `CategoryDeletionDialog` and `CategoryTargetUiModel`.
7. **Events** — add `ConfirmDeleteCategory`, `ConfirmDeleteCategoryMoveExpenses`, `ConfirmDeleteCategoryDeleteExpenses`, `DismissDeletionDialog`.
8. **StateHolder** — wire new events, results, and `withDeletionDialog` helper.
9. **Strings** — add all new string keys.
10. **UI** — add `SimpleDeleteCategoryDialog`, `DeleteCategoryWithExpensesDialog`, wire into `CategoriesScreen`.
11. **DI** — pass `expenseDao` to `CategoriesRepositoryImpl`.

---

## 10. Files Modified / Created

| Action | File |
|--------|------|
| Modified | `core/db/dao/ExpenseDao.kt` |
| Modified | `features/categories/domain/repository/CategoriesRepository.kt` |
| Modified | `features/categories/data/repository/CategoriesRepositoryImpl.kt` |
| Modified | `features/categories/domain/usecase/DeleteCategoryUseCase.kt` |
| Created | `features/categories/domain/model/ExpenseDeletionAction.kt` |
| Modified | `features/categories/presentation/CategoriesState.kt` |
| Modified | `features/categories/presentation/CategoriesEvent.kt` |
| Modified | `features/categories/presentation/CategoriesStateHolder.kt` |
| Modified | `features/categories/presentation/ui/CategoriesScreen.kt` |
| Modified | `features/categories/di/CategoriesModule.kt` |
| Modified | `composeResources/values/strings.xml` |

---

## 11. Risks

- **Atomicity:** `reassignCategory` + `categoryDao.deleteById` must run inside `database.withTransaction {}`. Without it, a crash between the two queries would leave orphaned expenses pointing at the deleted category. Wrap both calls inside `runCatching { database.withTransaction { ... } }` in the repository.
- **Empty target list:** If the category being deleted is the only category in the database, `availableTargets` in `WithExpenses` will be empty. The "Move" option should be disabled or hidden in this edge case — guard it in the dialog composable with `dialog.availableTargets.isEmpty()`.
- **Cyclomatic complexity in `resolveEventResult`:** Adding 4 branches risks breaching the limit of 10. Extract deletion event handling into a private `handleDeletionEvent(event: CategoriesEvent): Flow<DomainResult>?` and call it from the main `when` with an `else` fallback.
