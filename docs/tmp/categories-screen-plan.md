# Categories & Subcategories Management Screen — Implementation Plan

The feature adds a full-screen Categories management page reachable from the Settings tab. It reuses
the already-existing `CategoryDao`, `SubcategoryDao`, `CategoryEntity`, `SubcategoryEntity`, and the
domain models in `features/onboarding/`. A dedicated `features/categories/` feature module is created
following the exact same structure as `features/home/`.

The Settings tab currently renders a stateless `SettingsScreen` composable with no navigation
capability. The plan wires the "Categories" item tap into a new route pushed onto the tab's inner
back-stack (same pattern as `BottomTabsNavNavigationHost` handles nested routes).

---

## 1. Route definition

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/navigation/routes/CategoriesRoute.kt`

```kotlin
@Serializable
data object CategoriesRoute : NavKey
```

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/navigation/routes/RegisteredRoutes.kt`

Add inside `registerGlobalRoutes()`:
```kotlin
subclass(CategoriesRoute::class)
```

The Settings tab uses an inner `NavDisplay` (same as Home and Analytics). The `CategoriesRoute`
must also be registered in the `AppNavConfiguration` serializers module inside
`BottomTabsNavNavigationHost.kt` — add `subclass(CategoriesRoute::class)` inside
`registerMainBottomNavigation()`.

---

## 2. Settings tab — add navigation

The Settings tab currently provides no `Router` or navigation capability. Convert it to accept a
`router` parameter so the "Categories" item can push onto the inner back-stack.

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/navigation/tabs/SettingsTabHost.kt`

Change `settingsTabEntries()` signature to receive a `Router<NavKey>`:

```kotlin
internal fun EntryProviderScope<NavKey>.settingsTabEntries(router: Router<NavKey>) {
    entry<MainBottomTabs.Settings> {
        SettingsScreen(onNavigateToCategories = { router.push(CategoriesRoute) })
    }
    entry<CategoriesRoute> {
        CategoriesScreen(onGoBack = { router.pop() })
    }
}
```

`SettingsScreen` receives `onNavigateToCategories: () -> Unit` and passes it as the `onClick`
argument to the existing "Categories" `SettingsItem`.

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/navigation/BottomTabsNavNavigationHost.kt`

Pass `router` into `settingsTabEntries(router)` and add `CategoriesRoute` to the bottom-nav
serializers module.

---

## 3. Database migration (7→8) — add `comment` column

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/core/db/AppDatabase.kt`

- Bump `version = 8`.
- Add `MIGRATION_7_8`:

```kotlin
val MIGRATION_7_8 = Migration(7, 8) {
    it.execSQL("ALTER TABLE category ADD COLUMN comment TEXT DEFAULT NULL")
    it.execSQL("ALTER TABLE subcategory ADD COLUMN comment TEXT DEFAULT NULL")
}
```

- Register migration in the builder.

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/core/db/entity/CategoryEntity.kt`

Add field:
```kotlin
@ColumnInfo(name = "comment") val comment: String? = null,
```

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/core/db/entity/SubcategoryEntity.kt`

Add field:
```kotlin
@ColumnInfo(name = "comment") val comment: String? = null,
```

---

## 3b. Domain models

The existing `Category` and `Subcategory` domain models in
`features/onboarding/domain/model/` are updated to include `comment`.

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/onboarding/domain/model/Category.kt`

Add field: `val comment: String? = null`

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/onboarding/domain/model/Subcategory.kt`

Add field: `val comment: String? = null`

**Mapper updates** — all inline mappers that convert between `CategoryEntity`/`SubcategoryEntity`
and domain models must pass through the new `comment` field:
- `OnboardingRepositoryImpl` — `CategoryUiModel.toCategoryEntity()` (default `null`)
- `HomeRepositoryImpl` — `CategoryEntity` → `HomeCategoryItem` mapping
- `SubcategoryRepositoryImpl` — `SubcategoryEntity.toDomain()` and `Subcategory.toEntity()`
- `AddExpenseRepositoryImpl` — `CategoryEntity` → `ExpenseCategory` mapping

---

## 4. Data layer

### 4a. Repository interface

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/domain/repository/CategoriesRepository.kt`

```kotlin
interface CategoriesRepository {
    fun observeCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>>
    suspend fun addCategory(name: String, comment: String?): kotlin.Result<Unit>
    suspend fun addSubcategory(parentCategoryId: Long, name: String, comment: String?): kotlin.Result<Unit>
}
```

**New file (domain model):**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/domain/model/CategoryWithSubcategories.kt`

```kotlin
data class CategoryWithSubcategories(
    val category: Category,
    val subcategories: ImmutableList<Subcategory>,
)
```

Uses the existing `Category` and `Subcategory` from `features/onboarding/domain/model/`.

### 4b. Repository implementation

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/data/repository/CategoriesRepositoryImpl.kt`

- Inject: `CategoryDao`, `SubcategoryDao`, `ioDispatcher: CoroutineDispatcher`
- `observeCategoriesWithSubcategories()`: combine `categoryDao.observeAll()` and
  `subcategoryDao.observeAll()`, group subcategories by `parentCategoryId`, return mapped domain
  list. Use `.flowOn(ioDispatcher)`.
- `addCategory(name, comment)`: `runCatching { val sortOrder = categoryDao.getNextSortOrder(); categoryDao.insert(CategoryEntity(name=name, iconKey="ic_other", isDefault=false, sortOrder=sortOrder, comment=comment)) }`.
- `addSubcategory(parentCategoryId, name, comment)`: `runCatching { val sortOrder = subcategoryDao.getNextSortOrder(parentCategoryId); subcategoryDao.insert(SubcategoryEntity(parentCategoryId=parentCategoryId, name=name, iconKey="ic_other", isDefault=false, sortOrder=sortOrder, comment=comment)) }`.

Both suspend functions return `kotlin.Result<Unit>`.

---

## 5. Domain layer — use cases

### 5a. Observe use case

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/domain/usecase/ObserveCategoriesUseCase.kt`

Pattern mirrors `ObserveHomeDataUseCase`. Returns `Flow<DomainResult>`. No dispatcher injection —
repository handles `flowOn`.

```kotlin
class ObserveCategoriesUseCase(private val repository: CategoriesRepository) {
    operator fun invoke(): Flow<DomainResult> =
        repository.observeCategoriesWithSubcategories()
            .map<List<CategoryWithSubcategories>, DomainResult> { Result.Success(it) }
            .catch { emit(Result.Failure(it.toErrorType())) }

    sealed interface Result : DomainResult {
        data class Success(val data: List<CategoryWithSubcategories>) : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
```

### 5b. Add category use case

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/domain/usecase/AddCategoryUseCase.kt`

Pattern mirrors `features/home/domain/usecase/AddCategoryUseCase`. Inject `CategoriesRepository`
and `ioDispatcher`. `withContext(ioDispatcher)`, call `repository.addCategory(name).fold(...)`.

```kotlin
sealed interface Result : DomainResult {
    data object Success : Result
    data class Failure(override val errorType: ErrorType) : Result, ErrorResult
}
```

### 5c. Add subcategory use case

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/domain/usecase/AddSubcategoryUseCase.kt`

Same shape as `AddCategoryUseCase` but takes `parentCategoryId: Long` and `name: String`.

---

## 6. Presentation layer

### 6a. State

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/CategoriesState.kt`

```kotlin
@Stable
@Serializable
sealed interface CategoriesState {
    val categories: ImmutableList<CategoryRowUiModel>

    @Serializable
    data object Loading : CategoriesState {
        override val categories: ImmutableList<CategoryRowUiModel> = persistentListOf()
    }

    @Serializable
    data class Content(
        override val categories: ImmutableList<CategoryRowUiModel>,
        val showAddCategorySheet: Boolean,
        val addSubcategoryForCategoryId: Long?,   // non-null = show add-subcategory sheet
    ) : CategoriesState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        override val categories: ImmutableList<CategoryRowUiModel>,
        val showAddCategorySheet: Boolean,
        val addSubcategoryForCategoryId: Long?,
    ) : CategoriesState
}

@Stable
@Serializable
data class CategoryRowUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
    val comment: String?,
    @Serializable(with = ImmutableListSerializer::class)
    val subcategories: ImmutableList<SubcategoryChipUiModel>,
)

@Stable
@Serializable
data class SubcategoryChipUiModel(
    val id: Long,
    val name: String,
    val comment: String?,
)
```

`showAddCategorySheet` and `addSubcategoryForCategoryId` default to `false`/`null` in `Loading`
and are preserved on error via `toError()` extension.

### 6b. Event

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/CategoriesEvent.kt`

```kotlin
sealed interface CategoriesEvent {
    data object AddCategoryClicked : CategoriesEvent
    data class ConfirmAddCategory(val name: String, val comment: String?) : CategoriesEvent
    data object DismissAddCategorySheet : CategoriesEvent
    data class AddSubcategoryClicked(val categoryId: Long) : CategoriesEvent
    data class ConfirmAddSubcategory(val categoryId: Long, val name: String, val comment: String?) : CategoriesEvent
    data object DismissAddSubcategorySheet : CategoriesEvent
    data object DismissError : CategoriesEvent
}
```

### 6c. Navigation (private to StateHolder file)

No external navigation needed for this screen beyond the back press handled by the router.

### 6d. StateHolder

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/CategoriesStateHolder.kt`

- Extends `StateHolder<CategoriesState, CategoriesEvent>`.
- Inject: `ObserveCategoriesUseCase`, `AddCategoryUseCase`, `AddSubcategoryUseCase`.
- `bootstrapTiming = BootstrapTiming.DEFERRED`.
- `collectFlowsOnInit()` returns `observeCategoriesUseCase()`.
- `resolveEventResult(event)`:
  - `AddCategoryClicked` → `flowOf(ShowAddCategorySheet)`
  - `ConfirmAddCategory(name)` → `flow { emit(addCategoryUseCase(name.trim())) }`
  - `DismissAddCategorySheet` → `flowOf(HideAddCategorySheet)`
  - `AddSubcategoryClicked(id)` → `flowOf(ShowAddSubcategorySheet(id))`
  - `ConfirmAddSubcategory(id, name)` → `flow { emit(addSubcategoryUseCase(id, name.trim())) }`
  - `DismissAddSubcategorySheet` → `flowOf(HideAddSubcategorySheet)`
  - `DismissError` → `flowOf(ClearError)`
- `getStateByResult()` handles all transitions. On `AddCategoryUseCase.Result.Success` and
  `AddSubcategoryUseCase.Result.Success`, dismiss the relevant sheet and keep categories intact
  (the `observeCategoriesUseCase` flow will emit an updated list automatically).
- `getErrorStateByResult()` returns `state.value.toError(errorType)`.

Private sealed interface `CategoriesResult : Result`:
```kotlin
private sealed interface CategoriesResult : DomainResult {
    data object ShowAddCategorySheet : CategoriesResult
    data object HideAddCategorySheet : CategoriesResult
    data class ShowAddSubcategorySheet(val categoryId: Long) : CategoriesResult
    data object HideAddSubcategorySheet : CategoriesResult
    data object ClearError : CategoriesResult
}
```

`toError()` extension copies `categories`, `showAddCategorySheet`, `addSubcategoryForCategoryId`
from the previous state.

Domain list → UI list mapping via private extension in StateHolder file:

```kotlin
private fun List<CategoryWithSubcategories>.toUiModels(): ImmutableList<CategoryRowUiModel> =
    map { item ->
        CategoryRowUiModel(
            id = item.category.id,
            name = item.category.name,
            iconKey = item.category.iconKey,
            comment = item.category.comment,
            subcategories = item.subcategories.map { sub ->
                SubcategoryChipUiModel(id = sub.id, name = sub.name, comment = sub.comment)
            }.toImmutableList(),
        )
    }.toImmutableList()
```

---

## 7. UI layer

### 7a. Screen entry point

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/ui/CategoriesScreen.kt`

Pattern identical to `HomeScreen.kt`.

```kotlin
@Composable
fun CategoriesScreen(onGoBack: () -> Unit) {
    val stateHolder = koinViewModel<CategoriesStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(CategoriesEvent.DismissError) },
    ) {
        DisplayFullScreenProgress(showProgress = state is CategoriesState.Loading)
        CategoriesContent(state = state, onEvent = stateHolder::processEvent, onGoBack = onGoBack)
    }
}

internal val CategoriesState.asOverlay: ScreenOverlay?
    @Composable get() = when (this) {
        is CategoriesState.Error -> ScreenOverlay.Error(type = errorType)
        else -> null
    }
```

### 7b. Content composable

**New file (or section in CategoriesScreen.kt):**
`CategoriesContent` is a private composable in the same file.

Layout:
- `Scaffold` with a `TopAppBar` showing "Categories" title and an `ArrowBackIconButton`.
- FAB: `+` button calls `onEvent(CategoriesEvent.AddCategoryClicked)`.
- Body: `LazyColumn` with one item per `CategoryRowUiModel`.

### 7c. Category row

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/presentation/ui/CategoryManagementRow.kt`

Each row:
- Category name + emoji (from `categoryEmojiForKey(iconKey)`).
- Below the name: optional comment text in secondary style.
- Below the name/comment: a `FlowRow` (from `androidx.compose.foundation.layout.FlowRow`) of subcategory
  chips. Each chip shows the subcategory name; comment shown as tooltip or secondary text.
- If `subcategories` is empty, show a single `+` `AssistChip` (or `SuggestionChip`) labelled
  "Add subcategory" that fires `AddSubcategoryClicked(categoryId)`.
- If subcategories exist, render one chip per subcategory plus an additional small `+` chip at the
  end to add more.

`FlowRow` is available in `androidx.compose.foundation.layout` — verify the project already imports
`foundation` (it does, via Compose Multiplatform).

### 7d. Bottom sheets

Add-category and add-subcategory sheets are both inline within `CategoriesContent` using
`AppModalBottomSheet` from `uicomponents/sheets/BottomSheets.kt`.

Sheet visibility driven by state:
- `showAddCategorySheet == true` → show add-category sheet.
- `addSubcategoryForCategoryId != null` → show add-subcategory sheet.

Each sheet contains:
- A `TextField` for the name (required).
- A `TextField` for the comment (optional).
- A confirm button (use `ApplicationButton` from `uicomponents/buttons/ApplicationButton.kt`).
- Dismiss fires the relevant dismiss event.

Use `rememberModalBottomSheetState(initialDetent = Hidden)` from `com.composables.core` and
synchronise the detent with state using `LaunchedEffect`.

---

## 8. DI — Koin module

**New file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/features/categories/di/CategoriesModule.kt`

```kotlin
val categoriesModule = module {

    single<CategoriesRepository> {
        CategoriesRepositoryImpl(
            categoryDao = get<AppDatabase>().categoryDao(),
            subcategoryDao = get<AppDatabase>().subcategoryDao(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory { ObserveCategoriesUseCase(repository = get()) }

    factory {
        AddCategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        AddSubcategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel {
        CategoriesStateHolder(
            observeCategoriesUseCase = get(),
            addCategoryUseCase = get(),
            addSubcategoryUseCase = get(),
        )
    }
}
```

**Modified file:**
`composeApp/src/commonMain/kotlin/com/please/stop/app/di/AppModule.kt`

Add `categoriesModule` to the `includes(...)` list.

---

## 9. String resources

**Modified file:**
`composeApp/src/commonMain/composeResources/values/strings.xml`

Add entries following the `snake_case` with feature prefix `categories_*` convention:

```xml
<string name="categories_screen_title">Categories</string>
<string name="categories_add_category">Add Category</string>
<string name="categories_add_subcategory">Add Subcategory</string>
<string name="categories_add_subcategory_chip">+</string>
<string name="categories_name_hint">Name</string>
<string name="categories_confirm">Confirm</string>
<string name="categories_empty_subcategories_hint">No subcategories yet</string>
```

**Modified file:**
`composeApp/src/commonMain/composeResources/values/accessibility_strings.xml`

```xml
<string name="content_desc_categories_back">Navigate back from categories</string>
<string name="content_desc_add_category">Add new category</string>
<string name="content_desc_add_subcategory">Add subcategory to %1$s</string>
```

---

## 10. Implementation order

Follow this order to keep each layer buildable before the next is added:

1. **DB migration** — bump `AppDatabase` to v8, add `comment` column to both tables,
   update `CategoryEntity` and `SubcategoryEntity`.
2. **Domain models** — add `comment` to `Category` and `Subcategory`, update existing mappers.
3. **Route** — `CategoriesRoute.kt` + register in `RegisteredRoutes.kt` and
   `BottomTabsNavNavigationHost.kt`.
4. **Domain model** — `CategoryWithSubcategories.kt`.
5. **Repository interface** — `CategoriesRepository.kt`.
6. **Repository implementation** — `CategoriesRepositoryImpl.kt`.
7. **Use cases** — `ObserveCategoriesUseCase`, `AddCategoryUseCase`, `AddSubcategoryUseCase`.
8. **State + Event** — `CategoriesState.kt`, `CategoriesEvent.kt`.
9. **StateHolder** — `CategoriesStateHolder.kt`.
10. **UI** — `CategoriesScreen.kt`, `CategoryManagementRow.kt`.
11. **DI** — `CategoriesModule.kt` + `AppModule.kt`.
12. **Navigation wiring** — update `SettingsTabHost.kt` to accept `router`, push `CategoriesRoute`,
    register `entry<CategoriesRoute>` inside `settingsTabEntries`.
13. **String resources** — both XML files.

---

## 11. Files summary

### New files

| Path | Type |
|------|------|
| `composeApp/src/commonMain/.../navigation/routes/CategoriesRoute.kt` | Route |
| `composeApp/src/commonMain/.../features/categories/domain/model/CategoryWithSubcategories.kt` | Domain model |
| `composeApp/src/commonMain/.../features/categories/domain/repository/CategoriesRepository.kt` | Repository interface |
| `composeApp/src/commonMain/.../features/categories/data/repository/CategoriesRepositoryImpl.kt` | Repository impl |
| `composeApp/src/commonMain/.../features/categories/domain/usecase/ObserveCategoriesUseCase.kt` | Use case |
| `composeApp/src/commonMain/.../features/categories/domain/usecase/AddCategoryUseCase.kt` | Use case |
| `composeApp/src/commonMain/.../features/categories/domain/usecase/AddSubcategoryUseCase.kt` | Use case |
| `composeApp/src/commonMain/.../features/categories/presentation/CategoriesState.kt` | State |
| `composeApp/src/commonMain/.../features/categories/presentation/CategoriesEvent.kt` | Event |
| `composeApp/src/commonMain/.../features/categories/presentation/CategoriesStateHolder.kt` | StateHolder |
| `composeApp/src/commonMain/.../features/categories/presentation/ui/CategoriesScreen.kt` | Composable |
| `composeApp/src/commonMain/.../features/categories/presentation/ui/CategoryManagementRow.kt` | Composable |
| `composeApp/src/commonMain/.../features/categories/di/CategoriesModule.kt` | Koin module |

### Modified files

| Path | Change |
|------|--------|
| `core/db/AppDatabase.kt` | Bump version to 8, add `MIGRATION_7_8` |
| `core/db/entity/CategoryEntity.kt` | Add `comment: String? = null` column |
| `core/db/entity/SubcategoryEntity.kt` | Add `comment: String? = null` column |
| `features/onboarding/domain/model/Category.kt` | Add `comment: String? = null` field |
| `features/onboarding/domain/model/Subcategory.kt` | Add `comment: String? = null` field |
| `features/onboarding/data/repository/SubcategoryRepositoryImpl.kt` | Pass `comment` in entity↔domain mappers |
| `navigation/routes/RegisteredRoutes.kt` | `subclass(CategoriesRoute::class)` |
| `navigation/BottomTabsNavNavigationHost.kt` | Register `CategoriesRoute` in nav serializers; pass `router` to `settingsTabEntries` |
| `navigation/tabs/SettingsTabHost.kt` | Accept `router`, add `entry<CategoriesRoute>`, wire "Categories" onClick |
| `di/AppModule.kt` | Add `categoriesModule` |
| `composeResources/values/strings.xml` | Add `categories_*` strings |
| `composeResources/values/accessibility_strings.xml` | Add `content_desc_categories_*` strings |

---

## 12. Risks and notes

- **`FlowRow` availability**: `FlowRow` is in `androidx.compose.foundation.layout` (experimental
  since Compose 1.5, stable in 1.7+). Verify the Compose version in `libs.versions.toml`. If
  experimental, add `@OptIn(ExperimentalLayoutApi::class)`.
- **No network layer needed**: categories/subcategories are local-only (Room). No `ApiService` is
  required. The existing remote-config seeding in `SubcategoryRepositoryImpl` is separate and
  owned by the onboarding feature — do not touch it.
- **Migration 7→8 required**: Adding `comment` column to both `category` and `subcategory` tables.
  Simple `ALTER TABLE ADD COLUMN` — no data loss, nullable with `DEFAULT NULL`.
- **`AddCategoryUseCase` naming collision**: `features/home/domain/usecase/AddCategoryUseCase`
  already exists. The new use case lives under `features/categories/` with a distinct package,
  so no conflict at the class level. When importing in the Koin module, use fully qualified names
  or import aliases if needed.
- **Settings tab router**: The Settings tab currently has no inner navigation stack. After this
  change it gains one (via `router.push(CategoriesRoute)`). The `router` instance is already
  provided by the `BottomNavigationNavHost`'s `Nav3Host` — the same `router` passed to
  `homeTabEntries` is passed to `settingsTabEntries`. No additional router setup is required.
