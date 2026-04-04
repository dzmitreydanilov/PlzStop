package com.please.stop.app.features.expenses.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import com.please.stop.app.features.expenses.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.utils.date.localDateTimeFromMillis
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
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
) : StateHolder<AddExpenseState, AddExpenseEvent>() {

    override val bootstrapTiming = BootstrapTiming.DEFERRED

    protected var keyboardCalculator = KeyboardCalculator(decimalPlaces = 0, currencySymbol = "")

    protected abstract val editContext: EditContext
    protected abstract fun buildInitialForm(): ExpenseFormInput
    protected abstract fun hasUnsavedChanges(form: ExpenseFormInput, editContext: EditContext): Boolean

    @OptIn(ExperimentalTime::class)
    override fun getInitial(): AddExpenseState = AddExpenseState.Loading

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
                        dateEpochMillis = data.date?.toEpochMillis() ?: content.form.dateEpochMillis,
                    ),
                )
            }
            is AnalyzeReceiptUseCase.Result.Failure -> {
                previous.asContent()?.copy(
                    receipt = ReceiptState(error = result.receiptError),
                ) ?: previous
            }
            is ExpenseResult.ClearError -> previous.toContent()
            else -> super.getStateByResult(previous, result)
        }
        return (newState as? AddExpenseState.Content)?.withDerivedFields() ?: newState
    }

    override fun getErrorStateByResult(result: DomainResult, errorType: ErrorType): AddExpenseState {
        return state.value.toError(errorType)
    }

    override fun resolveEventResult(event: AddExpenseEvent): Flow<DomainResult> = when (event) {
        is AddExpenseEvent.KeyPressed -> handleKeyPress(event.key)
        is AddExpenseEvent.TitleChanged -> flowOf(
            updateContent { copy(form = form.copy(title = event.text.take(MAX_TITLE_LENGTH))) }
        )
        is AddExpenseEvent.CategorySelected -> flowOf(
            updateContent {
                copy(form = form.copy(selectedCategoryId = event.categoryId, selectedSubcategoryId = null))
            }
        )
        is AddExpenseEvent.SubcategorySelected -> flowOf(
            updateContent { copy(form = form.copy(selectedSubcategoryId = event.subcategoryId)) }
        )
        is AddExpenseEvent.DateChanged -> flowOf(handleDateChange(event.epochMillis))
        is AddExpenseEvent.TimeChanged -> flowOf(handleTimeChange(event.hour, event.minute))
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
        is AddExpenseEvent.DismissReceiptError -> flowOf(
            updateContent { copy(receipt = ReceiptState()) }
        )
        is AddExpenseEvent.DismissDatePicker -> flowOf(
            updateContent { updateStatus { copy(showDatePicker = false) } }
        )
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
            symbol = data.currencySymbol,
            decimalPlaces = data.decimalPlaces,
        )
        keyboardCalculator = KeyboardCalculator(
            decimalPlaces = currency.decimalPlaces,
            currencySymbol = data.currencySymbol,
        )
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

        val content = previous.asContent() ?: AddExpenseState.Content(
            editContext = editContext,
            currency = currency,
            form = buildInitialForm(),
            categories = persistentListOf(),
            subcategories = persistentListOf(),
            status = FormStatus(),
            receipt = ReceiptState(),
        )
        return content.copy(
            currency = currency,
            categories = categories,
            subcategories = subcategories,
        )
    }

    private fun handleKeyPress(key: NumericKey): Flow<DomainResult> = when (key) {
        is NumericKey.Calendar -> flowOf(
            updateContent { updateStatus { copy(showDatePicker = true) } }
        )
        is NumericKey.Equals -> {
            if (state.value.form.isInExpressionMode) {
                val newState = keyboardCalculator.processKey(key)
                flowOf(updateContent { applyKeyboardState(newState) })
            } else {
                handleSave()
            }
        }
        is NumericKey.CurrencySymbol -> flowOf(updateContent { this })
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
    )

    private fun handleDateChange(newDateEpochMillis: Long): DomainResult {
        return updateContent {
            val tz = TimeZone.currentSystemDefault()
            val oldDateTime = localDateTimeFromMillis(form.dateEpochMillis, tz)
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
            copy(form = form.copy(dateEpochMillis = merged.toInstant(tz).toEpochMilliseconds()))
                .updateStatus { copy(showDatePicker = false) }
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

        val amountMinorUnits = keyboardCalculator.parseToMinorUnits()
        val categoryId = content.form.selectedCategoryId ?: return@flow

        val result = saveExpenseUseCase(
            existingId = content.editContext.existingExpenseId,
            amountMinorUnits = amountMinorUnits,
            title = content.form.title.trim(),
            categoryId = categoryId,
            dateEpochMillis = content.form.dateEpochMillis,
            notes = content.form.notes.trim().takeIf { it.isNotBlank() },
            subcategoryId = content.form.selectedSubcategoryId,
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
        return updateStatus {
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

    protected fun AddExpenseState.toError(errorType: ErrorType): AddExpenseState.Error =
        AddExpenseState.Error(
            errorType = errorType,
            editContext = editContext,
            currency = currency,
            form = form,
            categories = categories,
            subcategories = subcategories,
            status = status,
            receipt = receipt,
        )

    private fun AddExpenseState.toContent(): AddExpenseState = when (this) {
        is AddExpenseState.Error -> AddExpenseState.Content(
            editContext = editContext,
            currency = currency,
            form = form,
            categories = categories,
            subcategories = subcategories,
            status = status,
            receipt = receipt,
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
            status = status,
            receipt = receipt,
        )
        AddExpenseState.Loading -> null
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
