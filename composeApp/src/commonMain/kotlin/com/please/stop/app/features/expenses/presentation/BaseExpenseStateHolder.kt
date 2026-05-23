package com.please.stop.app.features.expenses.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import com.please.stop.app.features.expenses.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.expenses.domain.usecase.BuildPendingReceiptDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.ClearPendingReceiptDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.FetchAndApplyExchangeRateUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataResult
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.ResolveExpenseSaveAmountsUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.features.expenses.domain.usecase.SetPendingReceiptDataUseCase
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.uicomponents.tagsForCategoryKey
import com.please.stop.app.utils.date.localDateTimeFromMillis
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.math.roundToLong
import kotlin.reflect.KClass
import com.please.stop.app.core.models.domain.Result as DomainResult

@Suppress("TooManyFunctions")
abstract class BaseExpenseStateHolder(
    private val observeFormDataUseCase: ObserveAddExpenseFormDataUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val analyzeReceiptUseCase: AnalyzeReceiptUseCase,
    private val fetchAndApplyExchangeRateUseCase: FetchAndApplyExchangeRateUseCase,
    private val setPendingReceiptDataUseCase: SetPendingReceiptDataUseCase,
    private val clearPendingReceiptDataUseCase: ClearPendingReceiptDataUseCase,
) : StateHolder<AddExpenseState, AddExpenseEvent>() {

    override val bootstrapTiming = BootstrapTiming.DEFERRED

    protected var keyboardCalculator = KeyboardCalculator(decimalPlaces = 0, currencySymbol = "")

    protected abstract val editContext: EditContext
    protected abstract fun buildInitialForm(): ExpenseFormInput
    protected abstract fun hasUnsavedChanges(
        form: ExpenseFormInput,
        editContext: EditContext
    ): Boolean

    override suspend fun bootstrap(emit: suspend (DomainResult) -> Unit) {
        clearPendingReceiptDataUseCase()
    }

    override fun getInitial(): AddExpenseState = AddExpenseState(
        editContext = editContext,
        currency = CurrencyConfig(symbol = "", decimalPlaces = 0),
        form = buildInitialForm(),
        categories = persistentListOf(),
        subcategories = persistentListOf(),
    )

    override fun collectFlowsOnInit(): Flow<DomainResult> = observeFormDataUseCase()

    override fun getNavigationResults(): Set<KClass<out DomainResult>> {
        return setOf(ExpenseResult.NavigateBack::class, ExpenseResult.NavigateToReceiptItems::class)
    }

    override fun getNavigationByResult(result: DomainResult): Navigation? = when (result) {
        is ExpenseResult.NavigateBack -> AddExpenseNavigation.GoBack
        is ExpenseResult.NavigateToReceiptItems -> AddExpenseNavigation.OpenReceiptItems
        else -> null
    }

    override fun getStateByResult(
        previous: AddExpenseState,
        result: DomainResult,
    ): AddExpenseState {
        val newState = when (result) {
            is ObserveAddExpenseFormDataResult.Success -> applyFormData(previous, result.data)
            is ObserveAddExpenseFormDataResult.Failure -> previous.withError(result.errorType)
            is ExpenseResult.UpdateContent -> result.updater(previous)
            is SaveExpenseUseCase.Result.Success -> previous.updateStatus { copy(isSaving = false) }
            is SaveExpenseUseCase.Result.Failure -> {
                previous.updateStatus { copy(isSaving = false) }.withError(result.errorType)
            }
            is AnalyzeReceiptUseCase.Result.Success -> applyReceiptResult(previous, result)
            is ExpenseResult.ClearError -> previous.copy(errorOverlay = null)
            else -> super.getStateByResult(previous, result)
        }
        return newState.withDerivedFields()
    }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType
    ): AddExpenseState {
        val receiptError = (result as? AnalyzeReceiptUseCase.Result.Failure)?.receiptError
        return state.value.withError(errorType, receiptError).copy(receipt = ReceiptState())
    }

    override fun resolveEventResult(event: AddExpenseEvent): Flow<DomainResult> {
        return when (event) {
            is AddExpenseEvent.KeyPressed -> handleKeyPress(event.key)
            is AddExpenseEvent.TitleChanged -> flowOf(
                updateState { copy(form = form.copy(title = event.text.take(MAX_TITLE_LENGTH))) }
            )
            is AddExpenseEvent.CategorySelected -> flowOf(
                updateState {
                    copy(
                        form = form.copy(
                            selectedCategoryId = event.categoryId,
                            selectedSubcategoryId = null
                        )
                    )
                }
            )
            is AddExpenseEvent.SubcategorySelected -> flowOf(
                updateState { copy(form = form.copy(selectedSubcategoryId = event.subcategoryId)) }
            )
            is AddExpenseEvent.DateChanged -> handleDateChange(event.epochMillis)
            is AddExpenseEvent.NotesChanged -> flowOf(
                updateState { copy(form = form.copy(notes = event.text.take(MAX_NOTES_LENGTH))) }
            )
            is AddExpenseEvent.SaveClicked -> handleSave()
            is AddExpenseEvent.DeleteClicked -> handleDeleteClicked()
            is AddExpenseEvent.ConfirmDelete -> handleConfirmDelete()
            is AddExpenseEvent.DismissDeleteDialog -> handleDismissDeleteDialog()
            is AddExpenseEvent.BackClicked -> handleBack()
            is AddExpenseEvent.ConfirmDiscard -> flow {
                emit(updateState { updateStatus { copy(showDiscardDialog = false) } })
                emit(ExpenseResult.NavigateBack)
            }
            is AddExpenseEvent.DismissDiscardDialog -> flowOf(
                updateState { updateStatus { copy(showDiscardDialog = false) } }
            )
            is AddExpenseEvent.DismissError -> flowOf(ExpenseResult.ClearError)
            is AddExpenseEvent.ReceiptScanned -> handleReceiptScanned(event.imageBytes)
            is AddExpenseEvent.DismissDatePicker -> flowOf(
                updateState { updateStatus { copy(showDatePicker = false) } }
            )
            is AddExpenseEvent.DismissCurrencyPicker -> flowOf(
                updateState { copy(showCurrencyPicker = false) }
            )
            is AddExpenseEvent.ExpenseCurrencySelected -> handleCurrencySelection(event.currency)
            is AddExpenseEvent.ShowRateOverrideSheet -> flowOf(
                updateState {
                    copy(
                        conversion = conversion.copy(
                            showRateEditSheet = true,
                            rateEditInput = conversion.rate?.toString() ?: "",
                        )
                    )
                }
            )
            is AddExpenseEvent.DismissRateOverrideSheet -> flowOf(
                updateState { copy(conversion = conversion.copy(showRateEditSheet = false)) }
            )
            is AddExpenseEvent.RateOverrideInputChanged -> flowOf(
                updateState { copy(conversion = conversion.copy(rateEditInput = event.input)) }
            )
            is AddExpenseEvent.ConfirmRateOverride -> flowOf(
                updateState { applyManualRateOverride() }
            )
            is AddExpenseEvent.ResetToFetchedRate -> flowOf(
                updateState { resetToFetchedRate() }
            )
            is AddExpenseEvent.ToggleSaveInOriginalCurrency -> flowOf(
                updateState {
                    copy(conversion = conversion.copy(saveInOriginalCurrency = !conversion.saveInOriginalCurrency))
                }
            )
            is AddExpenseEvent.CreateReceiptClicked -> handleCreateReceiptManually()
        }
    }

    protected open fun handleDeleteClicked(): Flow<DomainResult> =
        flowOf(updateState { this })

    protected open fun handleConfirmDelete(): Flow<DomainResult> =
        flowOf(updateState { this })

    protected open fun handleDismissDeleteDialog(): Flow<DomainResult> =
        flowOf(updateState { this })

    protected fun applyFormData(
        previous: AddExpenseState,
        data: AddExpenseFormData,
    ): AddExpenseState {
        val currency = AddExpenseStateMapper.toCurrencyConfig(data)
        val hasUserSelectedCurrency = previous.currency.code != previous.conversion.defaultCurrencyCode &&
            previous.currency.code.isNotEmpty()

        if (!hasUserSelectedCurrency) {
            keyboardCalculator = KeyboardCalculator(
                decimalPlaces = currency.decimalPlaces,
                currencySymbol = data.currencySymbol,
            )
        }

        val categories = AddExpenseStateMapper.toCategoryUiModels(data)
        val subcategories = AddExpenseStateMapper.toSubcategoryUiModels(data)
        return previous.copy(
            currency = if (hasUserSelectedCurrency) previous.currency else currency,
            categories = categories,
            subcategories = subcategories,
            conversion = previous.conversion.copy(
                defaultCurrencyCode = currency.code,
                defaultCurrencySymbol = currency.symbol,
            ),
            currencyConversionEnabled = data.currencyConversionEnabled,
        )
    }

    private fun applyReceiptResult(
        previous: AddExpenseState,
        result: AnalyzeReceiptUseCase.Result.Success,
    ): AddExpenseState {
        val data = result.data
        val newAmountInput = data.totalAmountMinorUnits?.let {
            keyboardCalculator.formatFromMinorUnits(it)
        } ?: previous.form.amountInput
        if (data.totalAmountMinorUnits != null) {
            keyboardCalculator.setFromAmount(newAmountInput)
        }
        val kbState = keyboardCalculator.getState()
        val newCategoryId = data.categoryId ?: previous.form.selectedCategoryId
        val categoryChanged = newCategoryId != previous.form.selectedCategoryId
        return previous.copy(
            receipt = ReceiptState(),
            form = previous.form.copy(
                title = data.merchantName ?: previous.form.title,
                amountInput = newAmountInput,
                amountDisplayExpression = kbState.displayExpression,
                isInExpressionMode = kbState.isInExpressionMode,
                selectedCategoryId = newCategoryId,
                selectedSubcategoryId = when {
                    categoryChanged -> data.subcategoryId
                    else -> data.subcategoryId ?: previous.form.selectedSubcategoryId
                },
                dateEpochMillis = data.date?.toEpochMillis() ?: previous.form.dateEpochMillis,
            ),
        )
    }

    private fun handleKeyPress(key: NumericKey): Flow<DomainResult> = when (key) {
        is NumericKey.Calendar -> flowOf(
            updateState { updateStatus { copy(showDatePicker = true) } }
        )
        is NumericKey.Notes -> emptyFlow<DomainResult>()
        is NumericKey.Equals -> {
            if (state.value.form.isInExpressionMode) {
                val newState = keyboardCalculator.processKey(key)
                flowOf(updateState { applyKeyboardState(newState) })
            } else {
                handleSave()
            }
        }
        is NumericKey.CurrencySymbol -> flowOf(
            updateState { copy(showCurrencyPicker = true) }
        )
        else -> {
            val newState = keyboardCalculator.processKey(key)
            flowOf(updateState { applyKeyboardState(newState) })
        }
    }

    private fun AddExpenseState.applyKeyboardState(
        kbState: KeyboardState,
    ): AddExpenseState = copy(
        form = form.copy(
            amountInput = kbState.currentValue,
            amountDisplayExpression = kbState.displayExpression,
            isInExpressionMode = kbState.isInExpressionMode,
        ),
    ).withUpdatedConversion()

    private fun handleCurrencySelection(
        selected: Currency,
    ): Flow<DomainResult> = flow {
        val newCurrency = CurrencyConfig(
            code = selected.code,
            symbol = selected.symbol,
            decimalPlaces = selected.decimalPlaces,
        )
        val content = state.value
        val currentAmountInput = content.form.amountInput
        keyboardCalculator = KeyboardCalculator(
            decimalPlaces = selected.decimalPlaces,
            currencySymbol = selected.symbol,
        )
        if (currentAmountInput.isNotEmpty()) {
            keyboardCalculator.setFromAmount(currentAmountInput)
        }
        val kbState = keyboardCalculator.getState()
        val defaultCode = content.conversion.defaultCurrencyCode
        val isSameCurrency = selected.code == defaultCode

        emit(
            updateState {
                copy(
                    currency = newCurrency,
                    showCurrencyPicker = false,
                    form = form.copy(
                        amountInput = currentAmountInput,
                        amountDisplayExpression = kbState.displayExpression,
                        isInExpressionMode = kbState.isInExpressionMode,
                    ),
                    conversion = if (isSameCurrency) {
                        ConversionState(
                            defaultCurrencyCode = defaultCode,
                            defaultCurrencySymbol = conversion.defaultCurrencySymbol,
                        )
                    } else {
                        conversion.copy(
                            isLoading = true,
                            rate = null,
                            fetchedRate = null,
                            isManualOverride = false,
                            convertedAmountMinorUnits = null,
                            hasFetchError = false,
                        )
                    },
                )
            }
        )

        if (isSameCurrency) return@flow

        val dateString = localDateTimeFromMillis(content.form.dateEpochMillis).date.toString()
        emitExchangeRateResult(selected.code, defaultCode, dateString)
    }

    private fun handleDateChange(newDateEpochMillis: Long): Flow<DomainResult> = flow {
        val tz = TimeZone.currentSystemDefault()
        val content = state.value
        val oldDateTime = localDateTimeFromMillis(content.form.dateEpochMillis, tz)
        val newDate = localDateTimeFromMillis(newDateEpochMillis, tz)
        val merged = LocalDateTime(
            year = newDate.year,
            month = newDate.month,
            day = newDate.day,
            hour = oldDateTime.hour,
            minute = oldDateTime.minute,
            second = 0,
            nanosecond = 0,
        )
        val mergedMillis = merged.toInstant(tz).toEpochMilliseconds()

        emit(
            updateState {
                copy(form = form.copy(dateEpochMillis = mergedMillis))
                    .updateStatus { copy(showDatePicker = false) }
            }
        )

        val conv = content.conversion
        val isForeignCurrency = content.currency.code != conv.defaultCurrencyCode &&
            conv.defaultCurrencyCode.isNotEmpty() &&
            content.currency.code.isNotEmpty()
        val dateChanged = oldDateTime.date != newDate.date

        if (isForeignCurrency && dateChanged) {
            emit(updateState { copy(conversion = conversion.resetForFetch()) })
            emitExchangeRateResult(content.currency.code, conv.defaultCurrencyCode, newDate.date.toString())
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<DomainResult>.emitExchangeRateResult(
        from: String,
        to: String,
        date: String,
    ) {
        when (val result = fetchAndApplyExchangeRateUseCase(from = from, to = to, date = date)) {
            is FetchAndApplyExchangeRateUseCase.Result.RateFetched -> emit(
                updateState {
                    copy(
                        conversion = conversion.copy(
                            isLoading = false,
                            rate = result.rate,
                            fetchedRate = result.rate,
                        ),
                    ).withUpdatedConversion()
                }
            )
            is FetchAndApplyExchangeRateUseCase.Result.FetchFailed -> emit(
                updateState {
                    copy(conversion = conversion.copy(isLoading = false, hasFetchError = true))
                }
            )
            is FetchAndApplyExchangeRateUseCase.Result.Disabled -> emit(
                updateState {
                    copy(
                        conversion = ConversionState(
                            defaultCurrencyCode = conversion.defaultCurrencyCode,
                            defaultCurrencySymbol = conversion.defaultCurrencySymbol,
                        ),
                    )
                }
            )
        }
    }

    private fun ConversionState.resetForFetch(): ConversionState = copy(
        isLoading = true,
        rate = null,
        fetchedRate = null,
        isManualOverride = false,
        convertedAmountMinorUnits = null,
        hasFetchError = false,
    )

    private fun AddExpenseState.withUpdatedConversion(): AddExpenseState {
        val rate = conversion.rate ?: return this
        val amountMinorUnits = keyboardCalculator.parseToMinorUnits()
        if (amountMinorUnits <= 0) {
            return copy(conversion = conversion.copy(convertedAmountMinorUnits = null))
        }
        val converted = (amountMinorUnits * rate).roundToLong()
        return copy(conversion = conversion.copy(convertedAmountMinorUnits = converted))
    }

    private fun AddExpenseState.applyManualRateOverride(): AddExpenseState {
        val rate = conversion.rateEditInput.toDoubleOrNull()
        if (rate == null || rate <= 0) return this
        return copy(
            conversion = conversion.copy(
                rate = rate,
                isManualOverride = true,
                showRateEditSheet = false,
            ),
        ).withUpdatedConversion()
    }

    private fun AddExpenseState.resetToFetchedRate(): AddExpenseState {
        val fetched = conversion.fetchedRate ?: return this
        return copy(
            conversion = conversion.copy(
                rate = fetched,
                isManualOverride = false,
            ),
        ).withUpdatedConversion()
    }

    private fun handleSave(): Flow<DomainResult> = flow {
        emit(updateState { updateStatus { copy(isSaving = true) } })
        val content = state.value
        val categoryId = content.form.selectedCategoryId ?: return@flow

        val amounts = ResolveExpenseSaveAmountsUseCase(
            ResolveExpenseSaveAmountsUseCase.Input(
                enteredAmountMinorUnits = keyboardCalculator.parseToMinorUnits(),
                convertedAmountMinorUnits = content.conversion.convertedAmountMinorUnits,
                currencyCode = content.currency.code,
                defaultCurrencyCode = content.conversion.defaultCurrencyCode,
                conversionRate = content.conversion.rate,
                saveInOriginalCurrency = content.conversion.saveInOriginalCurrency,
            )
        )

        val result = saveExpenseUseCase(
            existingId = content.editContext.existingExpenseId,
            amountMinorUnits = amounts.amountToSave,
            title = content.form.title.trim(),
            categoryId = categoryId,
            dateEpochMillis = content.form.dateEpochMillis,
            notes = content.form.notes.trim().takeIf { it.isNotBlank() },
            subcategoryId = content.form.selectedSubcategoryId,
            originalAmountMinorUnits = amounts.originalAmountMinorUnits,
            originalCurrencyCode = amounts.originalCurrencyCode,
            conversionRate = amounts.conversionRate,
        )
        emit(result)
        if (result is SaveExpenseUseCase.Result.Success) {
            emit(ExpenseResult.NavigateBack)
        }
    }

    private fun handleReceiptScanned(imageBytes: ByteArray): Flow<DomainResult> = flow {
        emit(updateState { copy(receipt = ReceiptState(isAnalyzing = true)) })
        val result = analyzeReceiptUseCase(imageBytes)
        if (result is AnalyzeReceiptUseCase.Result.Success && result.data.items.size > 1) {
            val pendingData = BuildPendingReceiptDataUseCase(
                BuildPendingReceiptDataUseCase.Input.FromReceipt(result.data)
            )
            setPendingReceiptDataUseCase(pendingData)
            emit(updateState { copy(receipt = ReceiptState()) })
            emit(ExpenseResult.NavigateToReceiptItems)
        } else {
            emit(result)
        }
    }

    private fun handleCreateReceiptManually(): Flow<DomainResult> = flow {
        val content = state.value
        val pendingData = BuildPendingReceiptDataUseCase(
            BuildPendingReceiptDataUseCase.Input.Manual(
                categoryId = content.form.selectedCategoryId,
                subcategoryId = content.form.selectedSubcategoryId,
            )
        )
        setPendingReceiptDataUseCase(pendingData)
        emit(ExpenseResult.NavigateToReceiptItems)
    }

    private fun handleBack(): Flow<DomainResult> {
        return if (state.value.status.hasUnsavedChanges) {
            flowOf(updateState { updateStatus { copy(showDiscardDialog = true) } })
        } else {
            flowOf(ExpenseResult.NavigateBack)
        }
    }

    private fun AddExpenseState.withDerivedFields(): AddExpenseState {
        val resolvedCategory = categories.firstOrNull { it.id == form.selectedCategoryId }
        val resolvedTags = resolvedCategory?.let {
            tagsForCategoryKey(it.iconKey).toImmutableList()
        } ?: persistentListOf()
        return copy(
            selectedCategory = resolvedCategory,
            titleTags = resolvedTags,
            dateTime = localDateTimeFromMillis(form.dateEpochMillis),
        ).updateStatus {
            copy(
                isFormValid = isFormValid(form),
                hasUnsavedChanges = hasUnsavedChanges(form, editContext),
            )
        }
    }

    private fun isFormValid(form: ExpenseFormInput): Boolean {
        return form.amountInput.isNotEmpty() &&
            form.amountInput.toDoubleOrNull().let { it != null && it > 0 } &&
            form.title.isNotBlank() &&
            form.selectedCategoryId != null
    }

    protected fun AddExpenseState.updateStatus(
        updater: FormStatus.() -> FormStatus,
    ): AddExpenseState = copy(status = status.updater())

    protected fun AddExpenseState.withError(
        errorType: ErrorType,
        receiptError: ReceiptError? = null,
    ): AddExpenseState = copy(errorOverlay = ErrorOverlay(errorType, receiptError))

    protected fun updateState(
        updater: AddExpenseState.() -> AddExpenseState,
    ): DomainResult = ExpenseResult.UpdateContent(updater)

    companion object {
        const val MAX_TITLE_LENGTH = 60
        const val TITLE_COUNTER_THRESHOLD = 50
        const val MAX_NOTES_LENGTH = 250
        const val NOTES_COUNTER_THRESHOLD = 200
    }
}

private fun String.toEpochMillis(): Long? = runCatching {
    val localDate = kotlinx.datetime.LocalDate.parse(this)
    localDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}.getOrNull()

internal sealed interface ExpenseResult : DomainResult {
    data class UpdateContent(
        val updater: AddExpenseState.() -> AddExpenseState,
    ) : ExpenseResult

    data object NavigateBack : ExpenseResult
    data object ClearError : ExpenseResult
    data object NavigateToReceiptItems : ExpenseResult
}
