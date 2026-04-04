# Currency Conversion Implementation Plan

When a user selects a non-default currency for an expense, the app fetches the exchange rate from frankfurter.app, shows a conversion preview above the amount display, allows manual rate override, and stores both the original and converted amounts in the database. All layers follow the existing MVI pattern: repos return `kotlin.Result`, use cases use `result.fold()`, dispatcher is injected per use case.

---

## 1. Database — Extend ExpenseEntity and Add Migration

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/core/db/entity/ExpenseEntity.kt`

Add three nullable columns:

```
originalAmountMinorUnits: Long?     // amount in the selected (non-default) currency
originalCurrencyCode: String?       // e.g. "EUR"
conversionRate: Double?             // rate applied: 1 originalCurrency = X defaultCurrency
```

When all three are null the expense is in the default currency (no conversion). A non-null `originalAmountMinorUnits` means the user entered the amount in a foreign currency.

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/core/db/AppDatabase.kt`

- Bump `version` from `4` to `5`.
- Add `MIGRATION_4_5` that `ALTER TABLE expense ADD COLUMN` for each of the three new columns (all nullable, no default required for `TEXT`/`REAL`/`INTEGER` nullable columns in SQLite).

---

## 2. Data Layer — Exchange Rate API

### 2a. API response model

**File (new):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/data/remote/model/ExchangeRateResponse.kt`

```kotlin
@Serializable
data class ExchangeRateResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
)
```

### 2b. Exchange rate API service

**File (new):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/data/remote/ExchangeRateApiService.kt`

Extends `ApiService`. One method:

```kotlin
suspend fun getRate(from: String, to: String): Result<Double>
```

Calls `GET https://api.frankfurter.app/latest?from={from}&to={to}` using the inherited `get<ExchangeRateResponse>()` helper, then extracts `rates[to]`. Returns `Result.success(rate)` or `Result.failure(...)`.

The base URL for this client must be `https://api.frankfurter.app` — this is separate from the app's primary backend. Create a dedicated `HttpClient` instance with no auth and `api.frankfurter.app` as the `defaultRequest` URL (see §6 DI).

### 2c. Exchange rate repository interface

**File (new):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/domain/repository/ExchangeRateRepository.kt`

```kotlin
interface ExchangeRateRepository {
    suspend fun getRate(from: String, to: String): Result<Double>
}
```

### 2d. Exchange rate repository implementation

**File (new):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/data/repository/ExchangeRateRepositoryImpl.kt`

- Holds an in-memory `Map<Pair<String,String>, Double>` cache (session-scoped, cleared on currency change is not needed — rate per pair is stable for the session).
- Delegates to `ExchangeRateApiService`.
- Returns `kotlin.Result`.

---

## 3. Domain Layer — Use Case

**File (new):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/domain/usecase/FetchExchangeRateUseCase.kt`

```kotlin
class FetchExchangeRateUseCase(
    private val repository: ExchangeRateRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(from: String, to: String): Result =
        withContext(ioDispatcher) {
            repository.getRate(from, to).fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failure(it.toErrorType()) },
            )
        }

    sealed interface Result : DomainResult {
        data class Success(val rate: Double) : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
```

---

## 4. Domain Model Changes

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/domain/model/AddExpenseData.kt`

`ExpenseDetail` needs new fields for pre-populating edit mode with existing conversion data:

```kotlin
data class ExpenseDetail(
    ...existing fields...,
    val originalAmountMinorUnits: Long?,
    val originalCurrencyCode: String?,
    val conversionRate: Double?,
)
```

---

## 5. Presentation Layer — State

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/presentation/AddExpenseState.kt`

Add `ConversionState` data class:

```kotlin
@Stable
@Serializable
data class ConversionState(
    val isLoading: Boolean = false,
    val rate: Double? = null,                   // fetched or user-overridden rate
    val isManualOverride: Boolean = false,
    val showRateEditSheet: Boolean = false,
    val rateEditInput: String = "",             // raw text in the override sheet
    val convertedAmountMinorUnits: Long? = null,// computed from current amount * rate
    val defaultCurrencyCode: String = "",
    val defaultCurrencySymbol: String = "",
)
```

Add `conversionState: ConversionState` to every variant of `AddExpenseState` (Loading default, Content, Error). Loading default uses `ConversionState()`.

The conversion banner is visible when `currency.code != conversionState.defaultCurrencyCode && currency.code.isNotEmpty() && conversionState.defaultCurrencyCode.isNotEmpty()`.

---

## 6. Presentation Layer — Events

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/presentation/AddExpenseEvent.kt`

Add:

```kotlin
data object ShowRateOverrideSheet : AddExpenseEvent
data object DismissRateOverrideSheet : AddExpenseEvent
data class RateOverrideInputChanged(val input: String) : AddExpenseEvent
data object ConfirmRateOverride : AddExpenseEvent
data object ResetToFetchedRate : AddExpenseEvent
```

---

## 7. Presentation Layer — StateHolder Logic

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/presentation/BaseExpenseStateHolder.kt`

### 7a. Inject `FetchExchangeRateUseCase`

Add it as a constructor parameter to `BaseExpenseStateHolder`.

### 7b. Wire new events in `resolveEventResult`

```kotlin
is AddExpenseEvent.ShowRateOverrideSheet -> flowOf(
    updateContent { copy(conversionState = conversionState.copy(showRateEditSheet = true, rateEditInput = conversionState.rate?.toString() ?: "")) }
)
is AddExpenseEvent.DismissRateOverrideSheet -> flowOf(
    updateContent { copy(conversionState = conversionState.copy(showRateEditSheet = false)) }
)
is AddExpenseEvent.RateOverrideInputChanged -> flowOf(
    updateContent { copy(conversionState = conversionState.copy(rateEditInput = event.input)) }
)
is AddExpenseEvent.ConfirmRateOverride -> flowOf(
    updateContent { applyManualRateOverride() }
)
is AddExpenseEvent.ResetToFetchedRate -> handleResetToFetchedRate()
```

### 7c. Trigger rate fetch on currency selection

In `applyCurrencySelection` (currently a pure function), after updating `currency`, return content with `conversionState.copy(isLoading = true, rate = null)`. Then in `resolveEventResult` for `ExpenseCurrencySelected`, chain a `flow { ... }` that emits the content update then calls `fetchExchangeRateUseCase(from = newCurrencyCode, to = defaultCurrencyCode)` and emits a new `ExpenseResult.UpdateContent` applying the rate result. If `from == to`, skip the fetch and emit a content update clearing conversion state.

Add a new internal `ExpenseResult`:

```kotlin
data class ExchangeRateFetched(val rate: Double) : ExpenseResult
data class ExchangeRateFetchFailed(val errorType: ErrorType) : ExpenseResult
```

Handle these in `getStateByResult`:

- `ExchangeRateFetched`: update `conversionState` with `isLoading = false`, `rate = result.rate`, recompute `convertedAmountMinorUnits`.
- `ExchangeRateFetchFailed`: set `isLoading = false`, leave `rate = null` (banner shows "rate unavailable" state).

### 7d. Recompute converted amount on key press

After `applyKeyboardState`, if `conversionState.rate != null`, recompute `convertedAmountMinorUnits = (keyboardCalculator.parseToMinorUnits() * rate).toLong()` and store it in `conversionState`. Extract a private extension `AddExpenseState.Content.withUpdatedConversion(): AddExpenseState.Content` that does this calculation so it can be called after every key press and after rate changes.

### 7e. Manual rate override

`applyManualRateOverride()` — parse `conversionState.rateEditInput` to `Double`, if valid set `rate`, `isManualOverride = true`, `showRateEditSheet = false`, then recompute `convertedAmountMinorUnits`.

### 7f. Apply default currency to ConversionState on form data load

In `applyFormData`, populate `conversionState.defaultCurrencyCode` and `conversionState.defaultCurrencySymbol` from `data.currencyCode` / `data.currencySymbol`. The selected expense currency at this point equals the default, so no fetch yet.

### 7g. Pass conversion data to `handleSave`

In `handleSave`, read `content.conversionState`:

```kotlin
val conversionRate: Double?
val originalAmountMinorUnits: Long?
val originalCurrencyCode: String?

if (content.currency.code != content.conversionState.defaultCurrencyCode && conversionState.rate != null) {
    conversionRate = conversionState.rate
    originalAmountMinorUnits = amountMinorUnits          // amount entered in foreign currency
    originalCurrencyCode = content.currency.code
    // amountMinorUnits to save = convertedAmountMinorUnits (default currency)
    val amountToSave = conversionState.convertedAmountMinorUnits ?: amountMinorUnits
} else {
    conversionRate = null; originalAmountMinorUnits = null; originalCurrencyCode = null
    val amountToSave = amountMinorUnits
}
```

Pass these to `saveExpenseUseCase`.

---

## 8. Domain — Extend SaveExpenseUseCase

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/domain/usecase/SaveExpenseUseCase.kt`

Add parameters to `invoke`:

```kotlin
originalAmountMinorUnits: Long? = null,
originalCurrencyCode: String? = null,
conversionRate: Double? = null,
```

Forward them to `repository.saveExpense` / `repository.updateExpense`.

---

## 9. Data — Extend Repository

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/domain/repository/AddExpenseRepository.kt`

Add the three new nullable parameters to both `saveExpense` and `updateExpense`.

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/data/repository/AddExpenseRepositoryImpl.kt`

- Pass the new columns into `ExpenseEntity` on insert and update.
- In `getExpenseById`, map the new columns into `ExpenseDetail`.

---

## 10. Edit Mode — Pre-populate Conversion State

**File to check:** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/edit/presentation/EditExpenseStateHolder.kt`

In `buildInitialForm` (or wherever `ExpenseDetail` is applied), if `detail.originalCurrencyCode != null`, set the expense currency to `originalCurrencyCode` and pre-populate `conversionState` with the stored `conversionRate`, `originalAmountMinorUnits` as the amount input, and `isManualOverride = true` (stored rate, no need to re-fetch). The `amountMinorUnits` (in default currency) goes into `conversionState.convertedAmountMinorUnits`.

---

## 11. UI — Conversion Banner

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/presentation/ui/AddExpenseScreen.kt`

Between the scrollable content column and `AmountDisplay`, add:

```kotlin
ConversionBanner(
    conversionState = state.conversionState,
    selectedCurrencySymbol = state.currency.symbol,
    onEditRate = { onEvent(AddExpenseEvent.ShowRateOverrideSheet) },
    onResetRate = { onEvent(AddExpenseEvent.ResetToFetchedRate) },
)
```

**New private composable `ConversionBanner`** (same file or extracted to `AddExpenseScreen.kt` private section):

- Hidden when `state.currency.code == state.conversionState.defaultCurrencyCode`.
- Shows a `Surface` / `Card` with `surfaceContainerLow` background.
- States:
  - `isLoading = true`: `CircularProgressIndicator` with "Fetching rate…" text.
  - `rate != null`: Shows "1 {selectedCurrency} = {rate} {defaultCurrency}" and the converted amount "≈ {convertedAmount} {defaultCurrencySymbol}".
  - `isManualOverride = true`: Small badge "Custom rate" with a reset icon button (`onResetRate`).
  - `rate == null && !isLoading`: "Rate unavailable — tap to enter manually" text, tapping calls `onEditRate`.
- Tapping the rate row calls `onEditRate`.
- An info tooltip icon (click reveals an `AlertDialog` or a small inline `Text`) explaining: "Your expense is stored in {defaultCurrency}. The conversion uses the rate shown. You can override it manually."

**Rate override bottom sheet** — add `showRateEditSheet` handling to the existing `if` block area:

```kotlin
if (state.conversionState.showRateEditSheet) {
    RateOverrideSheet(
        input = state.conversionState.rateEditInput,
        fromCode = state.currency.code,
        toCode = state.conversionState.defaultCurrencyCode,
        onInputChanged = { onEvent(AddExpenseEvent.RateOverrideInputChanged(it)) },
        onConfirm = { onEvent(AddExpenseEvent.ConfirmRateOverride) },
        onDismiss = { onEvent(AddExpenseEvent.DismissRateOverrideSheet) },
    )
}
```

`RateOverrideSheet` is a `ModalBottomSheet` containing an `OutlinedTextField` (numeric keyboard type) and a Confirm `TextButton`. Input validates as a positive `Double`.

---

## 12. Strings

**File (modified):** `composeApp/src/commonMain/composeResources/values/strings.xml`

Add:

```xml
<string name="conversion_fetching_rate">Fetching rate…</string>
<string name="conversion_rate_label">1 %1$s = %2$s %3$s</string>
<string name="conversion_converted_amount">≈ %1$s %2$s</string>
<string name="conversion_rate_unavailable">Rate unavailable — enter manually</string>
<string name="conversion_manual_badge">Custom rate</string>
<string name="conversion_reset_rate">Reset to fetched rate</string>
<string name="conversion_info_title">Currency conversion</string>
<string name="conversion_info_body">Your expense is stored in %1$s. The rate shown is used for conversion. You can override it manually.</string>
<string name="conversion_rate_override_title">Enter rate</string>
<string name="conversion_rate_override_hint">1 %1$s = ? %2$s</string>
<string name="conversion_rate_override_confirm">Apply</string>
```

---

## 13. DI

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/di/AddExpenseModule.kt`

Add:

```kotlin
// Frankfurter HTTP client — no auth, separate base URL
single(named("frankfurter")) {
    HttpClient(get<HttpClientEngine>()) {
        expectSuccess = true
        logging()
        contentEncoding()
        configureContent(json = get())
        configureTimeOut(HttpClientDataConfig(
            engine = get(),
            json = get(),
            networkLoggingEnabled = true,
        ))
        defaultRequest { url("https://api.frankfurter.app") }
    }
}

single<ExchangeRateApiService> {
    ExchangeRateApiService(httpClient = get(named("frankfurter")))
}

single<ExchangeRateRepository> {
    ExchangeRateRepositoryImpl(apiService = get())
}

factory {
    FetchExchangeRateUseCase(
        repository = get(),
        ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
    )
}
```

Update `CreateExpenseStateHolder` and `EditExpenseStateHolder` viewModel blocks to inject `fetchExchangeRateUseCase = get()`.

The `HttpClientEngine` is already provided by `platformModule` as an expect/actual (`httpEngine`). Reuse it — do not create a new engine.

---

## 14. StateHolder Constructors

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/presentation/BaseExpenseStateHolder.kt`

Constructor gains: `private val fetchExchangeRateUseCase: FetchExchangeRateUseCase`

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/create/presentation/CreateExpenseStateHolder.kt`

Add `fetchExchangeRateUseCase` param, pass to super.

**File (modified):** `composeApp/src/commonMain/kotlin/com/please/stop/app/features/expenses/edit/presentation/EditExpenseStateHolder.kt`

Add `fetchExchangeRateUseCase` param, pass to super. Also handle pre-population from `ExpenseDetail` conversion fields (step 10).

---

## Implementation Order

1. **Database** — extend `ExpenseEntity`, add `MIGRATION_4_5` to `AppDatabase` (version 5).
2. **Network** — `ExchangeRateResponse`, `ExchangeRateApiService`, `ExchangeRateRepository` interface, `ExchangeRateRepositoryImpl`.
3. **Domain** — `FetchExchangeRateUseCase`. Extend `ExpenseDetail` with conversion fields. Extend `AddExpenseRepository` interface signatures. Extend `SaveExpenseUseCase` parameters.
4. **Data** — Update `AddExpenseRepositoryImpl` (insert/update/read conversion columns).
5. **Presentation state** — Add `ConversionState` to `AddExpenseState`. Add new events. Extend `AddExpenseState` variants.
6. **StateHolder** — Wire all new events, fetch trigger, recompute logic, save changes, edit-mode pre-population.
7. **UI** — `ConversionBanner`, `RateOverrideSheet`, integrate into `AddExpenseContent`.
8. **Strings** — Add all string resources.
9. **DI** — Register new classes, update viewModel factories.

---

## Risk Notes

- The Frankfurter client must not reuse `configureDefaultRequest` (which sets the app's own `BASE_URL`). Construct it directly with `defaultRequest { url("https://api.frankfurter.app") }` rather than calling `createAuthFreeHttpClient`.
- `conversionRate` is a `Double` stored as `REAL` in SQLite — precision is sufficient for display but should not be used for financial recalculation from the stored value. The source of truth for stored money is always `amountMinorUnits` (default currency).
- When `from == to`, bypass the fetch and clear `conversionState` entirely so the banner is never shown.
- The `ResetToFetchedRate` event requires re-fetching because the originally fetched rate is not stored separately from the user-overridden rate. Store `fetchedRate: Double?` alongside `rate: Double?` in `ConversionState` to avoid a network round-trip on reset.
