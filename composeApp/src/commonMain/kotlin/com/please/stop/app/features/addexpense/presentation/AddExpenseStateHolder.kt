package com.please.stop.app.features.addexpense.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.addexpense.domain.model.AddExpenseFormData
import com.please.stop.app.features.addexpense.domain.model.ExpenseDetail
import com.please.stop.app.features.addexpense.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.addexpense.domain.usecase.DeleteExpenseUseCase
import com.please.stop.app.features.addexpense.domain.usecase.GetExpenseByIdUseCase
import com.please.stop.app.features.addexpense.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.addexpense.domain.usecase.SaveExpenseUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.math.pow
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.please.stop.app.core.models.domain.Result as DomainResult

class AddExpenseStateHolder(
    private val expenseId: Long?,
    private val preselectedCategoryId: Long?,
    private val observeFormDataUseCase: ObserveAddExpenseFormDataUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val analyzeReceiptUseCase: AnalyzeReceiptUseCase,
) : StateHolder<AddExpenseState, AddExpenseEvent>() {

    override val tag = "AddExpenseStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    private var initialExpense: ExpenseDetail? = null

    @OptIn(ExperimentalTime::class)
    override fun getInitial(): AddExpenseState = AddExpenseState.Loading

    override fun collectFlowsOnInit(): Flow<DomainResult> = observeFormDataUseCase()

    override suspend fun bootstrap(emit: suspend (DomainResult) -> Unit) {
        if (expenseId != null) {
            val result = getExpenseByIdUseCase(expenseId)
            emit(result)
        }
    }

    override fun getNavigationResults(): Set<KClass<out DomainResult>> {
        return setOf(AddExpenseResult.NavigateBack::class)
    }

    override fun getNavigationByResult(result: DomainResult): Navigation? = when (result) {
        is AddExpenseResult.NavigateBack -> AddExpenseNavigation.GoBack
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
                AddExpenseState.Error(result.errorType)
            }
            is GetExpenseByIdUseCase.Result.Success -> {
                applyExpenseDetail(previous, result.expense)
            }
            is GetExpenseByIdUseCase.Result.NotFound -> {
                AddExpenseState.Error(ErrorType.Unknown("Expense not found"))
            }
            is GetExpenseByIdUseCase.Result.Failure -> {
                AddExpenseState.Error(result.errorType)
            }
            is AddExpenseResult.UpdateContent -> result.updater(
                previous as? AddExpenseState.Content ?: return previous
            )
            is SaveExpenseUseCase.Result.Success -> {
                (previous as? AddExpenseState.Content)?.copy(isSaving = false) ?: previous
            }
            is SaveExpenseUseCase.Result.Failure -> {
                (previous as? AddExpenseState.Content)?.copy(
                    isSaving = false,
                    errorType = result.errorType,
                ) ?: previous
            }
            is DeleteExpenseUseCase.Result.Failure -> {
                (previous as? AddExpenseState.Content)?.copy(
                    isSaving = false,
                    errorType = result.errorType,
                ) ?: previous
            }
            is AnalyzeReceiptUseCase.Result.Success -> {
                val data = result.data
                val content = previous as? AddExpenseState.Content ?: return previous
                content.copy(
                    isAnalyzingReceipt = false,
                    title = data.merchantName ?: content.title,
                    amountInput = data.totalAmountMinorUnits?.let {
                        formatMinorUnitsToInput(it, content.decimalPlaces)
                    } ?: content.amountInput,
                    selectedCategoryId = data.categoryId ?: content.selectedCategoryId,
                    dateEpochMillis = data.date?.toEpochMillis() ?: content.dateEpochMillis,
                )
            }
            is AnalyzeReceiptUseCase.Result.Failure -> {
                (previous as? AddExpenseState.Content)?.copy(
                    isAnalyzingReceipt = false,
                    receiptError = result.receiptError,
                ) ?: previous
            }
            else -> super.getStateByResult(previous, result)
        }
        return (newState as? AddExpenseState.Content)?.withDerivedFields() ?: newState
    }

    override fun getErrorStateByResult(result: DomainResult, errorType: ErrorType): AddExpenseState {
        return AddExpenseState.Error(errorType)
    }

    @OptIn(ExperimentalTime::class)
    private fun applyFormData(
        previous: AddExpenseState,
        data: AddExpenseFormData,
    ): AddExpenseState {
        val content = previous as? AddExpenseState.Content ?: AddExpenseState.Content(
            isEditMode = expenseId != null,
            existingExpenseId = expenseId,
            selectedCategoryId = preselectedCategoryId,
            dateEpochMillis = Clock.System.now().toEpochMilliseconds(),
        )
        return content.copy(
            currencySymbol = data.currencySymbol,
            decimalPlaces = data.decimalPlaces,
            categories = data.categories.map { category ->
                CategoryUiModel(
                    id = category.id,
                    name = category.name,
                    iconKey = category.iconKey,
                )
            }.toImmutableList(),
        )
    }

    private fun applyExpenseDetail(
        previous: AddExpenseState,
        expense: ExpenseDetail,
    ): AddExpenseState {
        initialExpense = expense
        val content = previous as? AddExpenseState.Content ?: return previous
        return content.copy(
            existingExpenseId = expense.id,
            amountInput = formatMinorUnitsToInput(expense.amountMinorUnits, content.decimalPlaces),
            title = expense.title,
            selectedCategoryId = expense.categoryId,
            dateEpochMillis = expense.dateEpochMillis,
            notes = expense.notes.orEmpty(),
        )
    }

    override fun resolveEventResult(event: AddExpenseEvent): Flow<DomainResult> = when (event) {
        is AddExpenseEvent.KeyPressed -> flowOf(handleKeyPress(event.key))
        is AddExpenseEvent.TitleChanged -> flowOf(updateContent { copy(title = event.text.take(MAX_TITLE_LENGTH)) })
        is AddExpenseEvent.CategorySelected -> flowOf(updateContent { copy(selectedCategoryId = event.categoryId) })
        is AddExpenseEvent.DateChanged -> flowOf(handleDateChange(event.epochMillis))
        is AddExpenseEvent.TimeChanged -> flowOf(handleTimeChange(event.hour, event.minute))
        is AddExpenseEvent.NotesChanged -> flowOf(updateContent { copy(notes = event.text.take(MAX_NOTES_LENGTH)) })
        is AddExpenseEvent.SaveClicked -> handleSave()
        is AddExpenseEvent.DeleteClicked -> flowOf(updateContent { copy(showDeleteDialog = true) })
        is AddExpenseEvent.ConfirmDelete -> handleDelete()
        is AddExpenseEvent.DismissDeleteDialog -> flowOf(updateContent { copy(showDeleteDialog = false) })
        is AddExpenseEvent.BackClicked -> handleBack()
        is AddExpenseEvent.ConfirmDiscard -> flowOf(AddExpenseResult.NavigateBack)
        is AddExpenseEvent.DismissDiscardDialog -> flowOf(updateContent { copy(showDiscardDialog = false) })
        is AddExpenseEvent.DismissError -> flowOf(updateContent { copy(errorType = null) })
        is AddExpenseEvent.ReceiptScanned -> handleReceiptScanned(event.imageBytes)
        is AddExpenseEvent.DismissReceiptError -> flowOf(updateContent { copy(receiptError = null) })
    }

    private fun handleKeyPress(key: NumericKey): DomainResult {
        return updateContent {
            val newInput = applyKey(amountInput, key, decimalPlaces)
            copy(amountInput = newInput)
        }
    }

    private fun handleDateChange(newDateEpochMillis: Long): DomainResult {
        return updateContent {
            val tz = TimeZone.currentSystemDefault()
            val oldDateTime = Instant.fromEpochMilliseconds(dateEpochMillis).toLocalDateTime(tz)
            val newDate = Instant.fromEpochMilliseconds(newDateEpochMillis).toLocalDateTime(tz)
            val merged = LocalDateTime(
                year = newDate.year,
                month = newDate.month,
                day = newDate.day,
                hour = oldDateTime.hour,
                minute = oldDateTime.minute,
                second = 0,
                nanosecond = 0,
            )
            copy(dateEpochMillis = merged.toInstant(tz).toEpochMilliseconds())
        }
    }

    private fun handleTimeChange(hour: Int, minute: Int): DomainResult {
        return updateContent {
            val tz = TimeZone.currentSystemDefault()
            val oldDateTime = Instant.fromEpochMilliseconds(dateEpochMillis).toLocalDateTime(tz)
            val merged = LocalDateTime(
                year = oldDateTime.year,
                month = oldDateTime.month,
                day = oldDateTime.day,
                hour = hour,
                minute = minute,
                second = 0,
                nanosecond = 0,
            )
            copy(dateEpochMillis = merged.toInstant(tz).toEpochMilliseconds())
        }
    }

    private fun handleSave(): Flow<DomainResult> = flow {
        emit(updateContent { copy(isSaving = true) })
        val content = state.value as? AddExpenseState.Content ?: return@flow

        val amountMinorUnits = parseAmountToMinorUnits(content.amountInput, content.decimalPlaces)
        val categoryId = content.selectedCategoryId ?: return@flow

        val result = saveExpenseUseCase(
            existingId = content.existingExpenseId,
            amountMinorUnits = amountMinorUnits,
            title = content.title.trim(),
            categoryId = categoryId,
            dateEpochMillis = content.dateEpochMillis,
            notes = content.notes.trim().takeIf { it.isNotBlank() },
        )
        emit(result)
        if (result is SaveExpenseUseCase.Result.Success) {
            emit(AddExpenseResult.NavigateBack)
        }
    }

    private fun handleDelete(): Flow<DomainResult> = flow {
        emit(updateContent { copy(showDeleteDialog = false, isSaving = true) })
        val content = state.value as? AddExpenseState.Content ?: return@flow
        val id = content.existingExpenseId ?: return@flow

        val result = deleteExpenseUseCase(id)
        emit(result)
        if (result is DeleteExpenseUseCase.Result.Success) {
            emit(AddExpenseResult.NavigateBack)
        }
    }

    private fun handleReceiptScanned(imageBytes: ByteArray): Flow<DomainResult> = flow {
        emit(updateContent { copy(isAnalyzingReceipt = true, receiptError = null) })
        val result = analyzeReceiptUseCase(imageBytes)
        emit(result)
    }

    private fun handleBack(): Flow<DomainResult> {
        val content = state.value as? AddExpenseState.Content
        return if (content != null && content.hasUnsavedChanges) {
            flowOf(updateContent { copy(showDiscardDialog = true) })
        } else {
            flowOf(AddExpenseResult.NavigateBack)
        }
    }

    private fun AddExpenseState.Content.withDerivedFields(): AddExpenseState.Content {
        return copy(
            isFormValid = isFormValid(),
            hasUnsavedChanges = hasUnsavedChanges(),
        )
    }

    private fun AddExpenseState.Content.isFormValid(): Boolean {
        return amountInput.isNotEmpty() &&
            amountInput.toDoubleOrNull().let { it != null && it > 0 } &&
            title.isNotBlank() &&
            selectedCategoryId != null
    }

    private fun AddExpenseState.Content.hasUnsavedChanges(): Boolean {
        val initial = initialExpense
        return when {
            initial != null -> {
                amountInput != formatMinorUnitsToInput(initial.amountMinorUnits, decimalPlaces) ||
                    title != initial.title ||
                    selectedCategoryId != initial.categoryId ||
                    dateEpochMillis != initial.dateEpochMillis ||
                    notes != (initial.notes.orEmpty())
            }
            isEditMode -> false
            else -> {
                amountInput.isNotEmpty() ||
                    title.isNotBlank() ||
                    selectedCategoryId != null ||
                    notes.isNotBlank()
            }
        }
    }

    private fun updateContent(
        updater: AddExpenseState.Content.() -> AddExpenseState.Content,
    ): DomainResult = AddExpenseResult.UpdateContent(updater)

    companion object {
        const val MAX_TITLE_LENGTH = 60
        const val TITLE_COUNTER_THRESHOLD = 50
        const val MAX_NOTES_LENGTH = 250
        const val NOTES_COUNTER_THRESHOLD = 200
        private const val MAX_AMOUNT_VALUE = 9_999_999

        internal fun applyKey(current: String, key: NumericKey, decimalPlaces: Int): String {
            return when (key) {
                is NumericKey.Backspace -> {
                    if (current.isEmpty()) "" else current.dropLast(1)
                }
                is NumericKey.Decimal -> {
                    if (decimalPlaces == 0) current
                    else if (current.contains('.')) current
                    else if (current.isEmpty()) "0."
                    else "$current."
                }
                is NumericKey.Digit -> {
                    val candidate = current + key.value
                    if (!isValidAmount(candidate, decimalPlaces)) current
                    else candidate
                }
            }
        }

        private fun isValidAmount(input: String, decimalPlaces: Int): Boolean {
            val dotIndex = input.indexOf('.')
            if (dotIndex >= 0) {
                val fracLength = input.length - dotIndex - 1
                if (fracLength > decimalPlaces) return false
            }
            val numericValue = input.toDoubleOrNull() ?: return false
            return numericValue <= MAX_AMOUNT_VALUE
        }

        internal fun parseAmountToMinorUnits(input: String, decimalPlaces: Int): Long {
            if (input.isEmpty()) return 0L
            val value = input.toDoubleOrNull() ?: return 0L
            return (value * 10.0.pow(decimalPlaces)).toLong()
        }

        internal fun formatMinorUnitsToInput(minorUnits: Long, decimalPlaces: Int): String {
            if (decimalPlaces == 0) return minorUnits.toString()
            val divisor = 10.0.pow(decimalPlaces)
            val value = minorUnits / divisor
            val intPart = value.toLong()
            val fracPart = (minorUnits % 10.0.pow(decimalPlaces).toLong())
                .toString()
                .padStart(decimalPlaces, '0')
            return "$intPart.$fracPart"
        }
    }
}

private fun String.toEpochMillis(): Long? = runCatching {
    val localDate = LocalDate.parse(this)
    localDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}.getOrNull()

private sealed interface AddExpenseResult : DomainResult {
    data class UpdateContent(
        val updater: AddExpenseState.Content.() -> AddExpenseState.Content,
    ) : AddExpenseResult

    data object NavigateBack : AddExpenseResult
}
