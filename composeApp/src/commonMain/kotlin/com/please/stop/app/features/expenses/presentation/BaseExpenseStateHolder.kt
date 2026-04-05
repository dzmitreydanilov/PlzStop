package com.please.stop.app.features.expenses.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import com.please.stop.app.features.expenses.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.expenses.domain.usecase.FetchExchangeRateUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.features.onboarding.domain.model.Currency
import kotlin.math.roundToLong
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
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import com.please.stop.app.core.models.domain.Result as DomainResult

abstract class BaseExpenseStateHolder(
    private val observeFormDataUseCase: ObserveAddExpenseFormDataUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val analyzeReceiptUseCase: AnalyzeReceiptUseCase,
    private val fetchExchangeRateUseCase: FetchExchangeRateUseCase,
) : StateHolder<AddExpenseState, AddExpenseEvent>() {

    override val bootstrapTiming = BootstrapTiming.DEFERRED

    protected var keyboardCalculator = KeyboardCalculator(decimalPlaces = 0, currencySymbol = "")

    protected abstract val editContext: EditContext
    protected abstract fun buildInitialForm(): ExpenseFormInput
    protected abstract fun hasUnsavedChanges(
        form: ExpenseFormInput,
        editContext: EditContext
    ): Boolean

    override fun getInitial(): AddExpenseState = AddExpenseState.Content(
        editContext = editContext,
        currency = CurrencyConfig(symbol = "", decimalPlaces = 0),
        form = buildInitialForm(),
        categories = persistentListOf(),
        subcategories = persistentListOf(),
        status = FormStatus(),
        receipt = ReceiptState(),
    )

    override fun collectFlowsOnInit(): Flow<DomainResult> = observeFormDataUseCase()

    override fun getNavigationResults(): Set<KClass<out DomainResult>> {
        return setOf(ExpenseResult.NavigateBack::class)
    }

    override fun getNavigationByResult(result: DomainResult): Navigation? = when (result) {
        is ExpenseResult.NavigateBack -> AddExpenseNavigation.GoBack
        else -> null
    }

    @OptIn(ExperimentalTime::class)
    override fun getStateByResult(
        previous: AddExpenseState,
        result: DomainResult,
    ): AddExpenseState {
        val newState = when (result) {
            is ObserveAddExpenseFormDataUseCase.Result.Success -> {
                applyFormData(previous, result.data)
            }

            is ObserveAddExpenseFormDataUseCase.Result.Failure -> {
                previous.toError(result.errorType)
            }

            is ExpenseResult.UpdateContent -> result.updater(
                previous.asContent() ?: return previous
            )

            is SaveExpenseUseCase.Result.Success -> {
                previous.asContent()?.updateStatus { copy(isSaving = false) } ?: previous
            }

            is SaveExpenseUseCase.Result.Failure -> {
                val content = previous.asContent() ?: return previous
                content.updateStatus { copy(isSaving = false) }.toError(result.errorType)
            }

            is AnalyzeReceiptUseCase.Result.Success -> {
                val data = result.data
                val content = previous.asContent() ?: return previous
                val newAmountInput = data.totalAmountMinorUnits?.let {
                    keyboardCalculator.formatFromMinorUnits(it)
                } ?: content.form.amountInput
                if (data.totalAmountMinorUnits != null) {
                    keyboardCalculator.setFromAmount(newAmountInput)
                }
                val kbState = keyboardCalculator.getState()
                val newCategoryId = data.categoryId ?: content.form.selectedCategoryId
                val categoryChanged = newCategoryId != content.form.selectedCategoryId
                content.copy(
                    receipt = ReceiptState(),
                    form = content.form.copy(
                        title = data.merchantName ?: content.form.title,
                        amountInput = newAmountInput,
                        amountDisplayExpression = kbState.displayExpression,
                        isInExpressionMode = kbState.isInExpressionMode,
                        selectedCategoryId = newCategoryId,
                        selectedSubcategoryId = when {
                            categoryChanged -> data.subcategoryId
                            else -> data.subcategoryId ?: content.form.selectedSubcategoryId
                        },
                        dateEpochMillis = data.date?.toEpochMillis()
                            ?: content.form.dateEpochMillis,
                    ),
                )
            }

            is ExpenseResult.ClearError -> previous.toContent()
            else -> super.getStateByResult(previous, result)
        }
        return (newState as? AddExpenseState.Content)?.withDerivedFields() ?: newState
    }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType
    ): AddExpenseState {
        val receiptError = (result as? AnalyzeReceiptUseCase.Result.Failure)?.receiptError
        return state.value.toError(errorType, receiptError)
    }

    override fun resolveEventResult(event: AddExpenseEvent): Flow<DomainResult> {
        return when (event) {
            is AddExpenseEvent.KeyPressed -> handleKeyPress(event.key)
            is AddExpenseEvent.TitleChanged -> flowOf(
                updateContent { copy(form = form.copy(title = event.text.take(MAX_TITLE_LENGTH))) }
            )

            is AddExpenseEvent.CategorySelected -> flowOf(
                updateContent {
                    copy(
                        form = form.copy(
                            selectedCategoryId = event.categoryId,
                            selectedSubcategoryId = null
                        )
                    )
                }
            )

            is AddExpenseEvent.SubcategorySelected -> flowOf(
                updateContent { copy(form = form.copy(selectedSubcategoryId = event.subcategoryId)) }
            )

            is AddExpenseEvent.DateChanged -> handleDateChange(event.epochMillis)
            is AddExpenseEvent.NotesChanged -> flowOf(
                updateContent { copy(form = form.copy(notes = event.text.take(MAX_NOTES_LENGTH))) }
            )

            is AddExpenseEvent.SaveClicked -> handleSave()
            is AddExpenseEvent.DeleteClicked -> handleDeleteClicked()
            is AddExpenseEvent.ConfirmDelete -> handleConfirmDelete()
            is AddExpenseEvent.DismissDeleteDialog -> handleDismissDeleteDialog()
            is AddExpenseEvent.BackClicked -> handleBack()
            is AddExpenseEvent.ConfirmDiscard -> flow {
                emit(updateContent { updateStatus { copy(showDiscardDialog = false) } })
                emit(ExpenseResult.NavigateBack)
            }

            is AddExpenseEvent.DismissDiscardDialog -> flowOf(
                updateContent { updateStatus { copy(showDiscardDialog = false) } }
            )

            is AddExpenseEvent.DismissError -> flowOf(ExpenseResult.ClearError)
            is AddExpenseEvent.ReceiptScanned -> handleReceiptScanned(event.imageBytes)
            is AddExpenseEvent.DismissDatePicker -> flowOf(
                updateContent { updateStatus { copy(showDatePicker = false) } }
            )

            is AddExpenseEvent.DismissCurrencyPicker -> flowOf(
                updateContent { copy(showCurrencyPicker = false) }
            )

            is AddExpenseEvent.ExpenseCurrencySelected -> handleCurrencySelection(event.currency)
            is AddExpenseEvent.ShowRateOverrideSheet -> flowOf(
                updateContent {
                    copy(
                        conversion = conversion.copy(
                            showRateEditSheet = true,
                            rateEditInput = conversion.rate?.toString() ?: "",
                        )
                    )
                }
            )

            is AddExpenseEvent.DismissRateOverrideSheet -> flowOf(
                updateContent { copy(conversion = conversion.copy(showRateEditSheet = false)) }
            )

            is AddExpenseEvent.RateOverrideInputChanged -> flowOf(
                updateContent { copy(conversion = conversion.copy(rateEditInput = event.input)) }
            )

            is AddExpenseEvent.ConfirmRateOverride -> flowOf(
                updateContent { applyManualRateOverride() }
            )

            is AddExpenseEvent.ResetToFetchedRate -> flowOf(
                updateContent { resetToFetchedRate() }
            )

            is AddExpenseEvent.ToggleSaveInOriginalCurrency -> flowOf(
                updateContent {
                    copy(conversion = conversion.copy(saveInOriginalCurrency = !conversion.saveInOriginalCurrency))
                }
            )
        }
    }

    protected open fun handleDeleteClicked(): Flow<DomainResult> =
        flowOf(updateContent { this })

    protected open fun handleConfirmDelete(): Flow<DomainResult> =
        flowOf(updateContent { this })

    protected open fun handleDismissDeleteDialog(): Flow<DomainResult> =
        flowOf(updateContent { this })

    @OptIn(ExperimentalTime::class)
    protected fun applyFormData(
        previous: AddExpenseState,
        data: AddExpenseFormData,
    ): AddExpenseState {
        val currency = CurrencyConfig(
            code = data.currencyCode,
            symbol = data.currencySymbol,
            decimalPlaces = data.decimalPlaces,
        )
        val existingCurrency = previous.asContent()?.currency
        val hasUserSelectedCurrency = existingCurrency != null &&
                existingCurrency.code != previous.conversion.defaultCurrencyCode &&
                existingCurrency.code.isNotEmpty()
        if (!hasUserSelectedCurrency) {
            keyboardCalculator = KeyboardCalculator(
                decimalPlaces = currency.decimalPlaces,
                currencySymbol = data.currencySymbol,
            )
        }
        val categories = data.categories.map { category ->
            CategoryUiModel(
                id = category.id,
                name = category.name,
                iconKey = category.iconKey,
            )
        }.toImmutableList()

        val subcategories = data.subcategories.map { sub ->
            SubcategoryUiModel(
                id = sub.id,
                parentCategoryId = sub.parentCategoryId,
                name = sub.name,
                iconKey = sub.iconKey,
            )
        }.toImmutableList()

        val existingContent = previous.asContent()
        val content = existingContent ?: AddExpenseState.Content(
            editContext = editContext,
            currency = currency,
            form = buildInitialForm(),
            categories = persistentListOf(),
            subcategories = persistentListOf(),
            status = FormStatus(),
            receipt = ReceiptState(),
        )
        val preserveUserCurrency = existingContent != null &&
                existingContent.currency.code != existingContent.conversion.defaultCurrencyCode &&
                existingContent.currency.code.isNotEmpty()
        return content.copy(
            currency = if (preserveUserCurrency) existingContent.currency else currency,
            categories = categories,
            subcategories = subcategories,
            conversion = content.conversion.copy(
                defaultCurrencyCode = currency.code,
                defaultCurrencySymbol = currency.symbol,
            ),
            currencyConversionEnabled = data.currencyConversionEnabled,
        )
    }

    private fun handleKeyPress(key: NumericKey): Flow<DomainResult> = when (key) {
        is NumericKey.Calendar -> flowOf(
            updateContent { updateStatus { copy(showDatePicker = true) } }
        )

        is NumericKey.Notes -> emptyFlow<DomainResult>()

        is NumericKey.Equals -> {
            if (state.value.form.isInExpressionMode) {
                val newState = keyboardCalculator.processKey(key)
                flowOf(updateContent { applyKeyboardState(newState) })
            } else {
                handleSave()
            }
        }

        is NumericKey.CurrencySymbol -> flowOf(
            updateContent { copy(showCurrencyPicker = true) }
        )

        else -> {
            val newState = keyboardCalculator.processKey(key)
            flowOf(updateContent { applyKeyboardState(newState) })
        }
    }

    private fun AddExpenseState.Content.applyKeyboardState(
        kbState: KeyboardState,
    ): AddExpenseState.Content = copy(
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
        val content = state.value.asContent() ?: return@flow
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

        emit(updateContent {
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
        })

        if (isSameCurrency) return@flow

        val dateString = localDateTimeFromMillis(content.form.dateEpochMillis).date.toString()
        val result =
            fetchExchangeRateUseCase(from = selected.code, to = defaultCode, date = dateString)
        when (result) {
            is FetchExchangeRateUseCase.Result.Success -> emit(updateContent {
                copy(
                    conversion = conversion.copy(
                        isLoading = false,
                        rate = result.rate,
                        fetchedRate = result.rate,
                    ),
                ).withUpdatedConversion()
            })

            is FetchExchangeRateUseCase.Result.Failure -> emit(updateContent {
                copy(conversion = conversion.copy(isLoading = false, hasFetchError = true))
            })

            is FetchExchangeRateUseCase.Result.Disabled -> emit(updateContent {
                copy(
                    conversion = ConversionState(
                        defaultCurrencyCode = conversion.defaultCurrencyCode,
                        defaultCurrencySymbol = conversion.defaultCurrencySymbol,
                    ),
                )
            })
        }
    }

    private fun AddExpenseState.Content.withUpdatedConversion(): AddExpenseState.Content {
        val rate = conversion.rate ?: return this
        val amountMinorUnits = keyboardCalculator.parseToMinorUnits()
        if (amountMinorUnits <= 0) {
            return copy(conversion = conversion.copy(convertedAmountMinorUnits = null))
        }
        val converted = (amountMinorUnits * rate).roundToLong()
        return copy(conversion = conversion.copy(convertedAmountMinorUnits = converted))
    }

    private fun AddExpenseState.Content.applyManualRateOverride(): AddExpenseState.Content {
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

    private fun AddExpenseState.Content.resetToFetchedRate(): AddExpenseState.Content {
        val fetched = conversion.fetchedRate ?: return this
        return copy(
            conversion = conversion.copy(
                rate = fetched,
                isManualOverride = false,
            ),
        ).withUpdatedConversion()
    }

    private fun handleDateChange(newDateEpochMillis: Long): Flow<DomainResult> = flow {
        val tz = TimeZone.currentSystemDefault()
        val content = state.value.asContent() ?: return@flow
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

        emit(updateContent {
            copy(form = form.copy(dateEpochMillis = mergedMillis))
                .updateStatus { copy(showDatePicker = false) }
        })

        val conv = content.conversion
        val isForeignCurrency = content.currency.code != conv.defaultCurrencyCode &&
                conv.defaultCurrencyCode.isNotEmpty() &&
                content.currency.code.isNotEmpty()
        val dateChanged = oldDateTime.date != newDate.date

        if (isForeignCurrency && dateChanged) {
            emit(updateContent {
                copy(
                    conversion = conversion.copy(
                        isLoading = true,
                        rate = null,
                        fetchedRate = null,
                        isManualOverride = false,
                        convertedAmountMinorUnits = null,
                        hasFetchError = false,
                    ),
                )
            })
            val dateString = newDate.date.toString()
            val result = fetchExchangeRateUseCase(
                from = content.currency.code,
                to = conv.defaultCurrencyCode,
                date = dateString,
            )
            when (result) {
                is FetchExchangeRateUseCase.Result.Success -> emit(updateContent {
                    copy(
                        conversion = conversion.copy(
                            isLoading = false,
                            rate = result.rate,
                            fetchedRate = result.rate,
                        ),
                    ).withUpdatedConversion()
                })

                is FetchExchangeRateUseCase.Result.Failure -> emit(updateContent {
                    copy(conversion = conversion.copy(isLoading = false))
                })

                is FetchExchangeRateUseCase.Result.Disabled -> emit(updateContent {
                    copy(
                        conversion = ConversionState(
                            defaultCurrencyCode = conversion.defaultCurrencyCode,
                            defaultCurrencySymbol = conversion.defaultCurrencySymbol,
                        ),
                    )
                })
            }
        }
    }

    private fun handleTimeChange(hour: Int, minute: Int): DomainResult {
        return updateContent {
            val tz = TimeZone.currentSystemDefault()
            val oldDateTime = localDateTimeFromMillis(form.dateEpochMillis, tz)
            val merged = LocalDateTime(
                year = oldDateTime.year,
                month = oldDateTime.month,
                day = oldDateTime.day,
                hour = hour,
                minute = minute,
                second = 0,
                nanosecond = 0,
            )
            copy(form = form.copy(dateEpochMillis = merged.toInstant(tz).toEpochMilliseconds()))
        }
    }

    private fun handleSave(): Flow<DomainResult> = flow {
        emit(updateContent { updateStatus { copy(isSaving = true) } })
        val content = state.value.asContent() ?: return@flow

        val enteredAmountMinorUnits = keyboardCalculator.parseToMinorUnits()
        val categoryId = content.form.selectedCategoryId ?: return@flow

        val conv = content.conversion
        val isForeignCurrency = content.currency.code != conv.defaultCurrencyCode &&
                conv.rate != null

        val amountToSave: Long
        val originalAmount: Long?
        val originalCurrencyCode: String?

        if (isForeignCurrency) {
            if (conv.saveInOriginalCurrency) {
                amountToSave = enteredAmountMinorUnits
                originalAmount = conv.convertedAmountMinorUnits
                originalCurrencyCode = conv.defaultCurrencyCode
            } else {
                amountToSave = conv.convertedAmountMinorUnits ?: enteredAmountMinorUnits
                originalAmount = enteredAmountMinorUnits
                originalCurrencyCode = content.currency.code
            }
        } else {
            amountToSave = enteredAmountMinorUnits
            originalAmount = null
            originalCurrencyCode = null
        }

        val result = saveExpenseUseCase(
            existingId = content.editContext.existingExpenseId,
            amountMinorUnits = amountToSave,
            title = content.form.title.trim(),
            categoryId = categoryId,
            dateEpochMillis = content.form.dateEpochMillis,
            notes = content.form.notes.trim().takeIf { it.isNotBlank() },
            subcategoryId = content.form.selectedSubcategoryId,
            originalAmountMinorUnits = originalAmount,
            originalCurrencyCode = originalCurrencyCode,
            conversionRate = if (isForeignCurrency) conv.rate else null,
        )
        emit(result)
        if (result is SaveExpenseUseCase.Result.Success) {
            emit(ExpenseResult.NavigateBack)
        }
    }

    private fun handleReceiptScanned(imageBytes: ByteArray): Flow<DomainResult> = flow {
        emit(updateContent { copy(receipt = ReceiptState(isAnalyzing = true)) })
        val result = analyzeReceiptUseCase(imageBytes)
        emit(result)
    }

    private fun handleBack(): Flow<DomainResult> {
        return if (state.value.status.hasUnsavedChanges) {
            flowOf(updateContent { updateStatus { copy(showDiscardDialog = true) } })
        } else {
            flowOf(ExpenseResult.NavigateBack)
        }
    }

    private fun AddExpenseState.Content.withDerivedFields(): AddExpenseState.Content {
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

    protected fun AddExpenseState.Content.updateStatus(
        updater: FormStatus.() -> FormStatus,
    ): AddExpenseState.Content = copy(status = status.updater())

    protected fun AddExpenseState.toError(
        errorType: ErrorType,
        receiptError: ReceiptError? = null,
    ): AddExpenseState.Error =
        AddExpenseState.Error(
            errorType = errorType,
            receiptError = receiptError,
            editContext = editContext,
            currency = currency,
            form = form,
            categories = categories,
            subcategories = subcategories,
            showCurrencyPicker = showCurrencyPicker,
            status = status,
            receipt = receipt,
            conversion = conversion,
            currencyConversionEnabled = currencyConversionEnabled,
        )

    private fun AddExpenseState.toContent(): AddExpenseState = when (this) {
        is AddExpenseState.Error -> AddExpenseState.Content(
            editContext = editContext,
            currency = currency,
            form = form,
            categories = categories,
            subcategories = subcategories,
            showCurrencyPicker = showCurrencyPicker,
            status = status,
            receipt = receipt,
            conversion = conversion,
            currencyConversionEnabled = currencyConversionEnabled,
        )

        else -> this
    }

    protected fun AddExpenseState.asContent(): AddExpenseState.Content? = when (this) {
        is AddExpenseState.Content -> this
        is AddExpenseState.Error -> AddExpenseState.Content(
            editContext = editContext,
            currency = currency,
            form = form,
            categories = categories,
            subcategories = subcategories,
            showCurrencyPicker = showCurrencyPicker,
            status = status,
            receipt = receipt,
            conversion = conversion,
            currencyConversionEnabled = currencyConversionEnabled,
        )
    }

    protected fun updateContent(
        updater: AddExpenseState.Content.() -> AddExpenseState.Content,
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
        val updater: AddExpenseState.Content.() -> AddExpenseState.Content,
    ) : ExpenseResult

    data object NavigateBack : ExpenseResult
    data object ClearError : ExpenseResult
}
