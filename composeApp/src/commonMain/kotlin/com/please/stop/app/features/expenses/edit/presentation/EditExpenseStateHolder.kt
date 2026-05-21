package com.please.stop.app.features.expenses.edit.presentation

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.expenses.domain.model.ExpenseDetail
import com.please.stop.app.features.expenses.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.expenses.domain.usecase.ClearPendingReceiptDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.FetchAndApplyExchangeRateUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataResult
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.features.expenses.domain.usecase.SetPendingReceiptDataUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.DeleteExpenseUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.GetExpenseByIdResult
import com.please.stop.app.features.expenses.edit.domain.usecase.GetExpenseByIdUseCase
import com.please.stop.app.features.expenses.presentation.AddExpenseState
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder
import com.please.stop.app.features.expenses.presentation.EditContext
import com.please.stop.app.features.expenses.presentation.ExpenseFormInput
import com.please.stop.app.features.expenses.presentation.ExpenseResult
import com.please.stop.app.utils.date.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import com.please.stop.app.core.models.domain.Result as DomainResult

class EditExpenseStateHolder(
    private val expenseId: Long,
    observeFormDataUseCase: ObserveAddExpenseFormDataUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    saveExpenseUseCase: SaveExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    analyzeReceiptUseCase: AnalyzeReceiptUseCase,
    fetchAndApplyExchangeRateUseCase: FetchAndApplyExchangeRateUseCase,
    setPendingReceiptDataUseCase: SetPendingReceiptDataUseCase,
    clearPendingReceiptDataUseCase: ClearPendingReceiptDataUseCase,
) : BaseExpenseStateHolder(
    observeFormDataUseCase = observeFormDataUseCase,
    saveExpenseUseCase = saveExpenseUseCase,
    analyzeReceiptUseCase = analyzeReceiptUseCase,
    fetchAndApplyExchangeRateUseCase = fetchAndApplyExchangeRateUseCase,
    setPendingReceiptDataUseCase = setPendingReceiptDataUseCase,
    clearPendingReceiptDataUseCase = clearPendingReceiptDataUseCase,
) {
    override val tag = "EditExpenseStateHolder"
    private var pendingExpenseDetail: ExpenseDetail? = null

    override val editContext = EditContext(
        isEditMode = true,
        existingExpenseId = expenseId,
    )

    override fun buildInitialForm() = ExpenseFormInput(
        dateEpochMillis = nowMillis(),
    )

    override suspend fun bootstrap(emit: suspend (DomainResult) -> Unit) {
        super.bootstrap(emit)
        val result = getExpenseByIdUseCase(expenseId)
        emit(result)
    }

    override fun getStateByResult(
        previous: AddExpenseState,
        result: DomainResult,
    ): AddExpenseState = when (result) {
        is GetExpenseByIdResult.Success -> {
            if (previous.currency.code.isEmpty()) {
                pendingExpenseDetail = result.expense
                previous
            } else {
                pendingExpenseDetail = null
                applyExpenseDetailResult(previous, result.expense)
            }
        }
        is ObserveAddExpenseFormDataResult.Success -> {
            val stateWithFormData = super.getStateByResult(previous, result)
            val pendingExpense = pendingExpenseDetail ?: return stateWithFormData
            pendingExpenseDetail = null
            applyExpenseDetailResult(stateWithFormData, pendingExpense)
        }
        is GetExpenseByIdResult.NotFound -> {
            previous.withError(ErrorType.Unknown("Expense not found"))
        }
        is DeleteExpenseUseCase.Result.Failure -> {
            previous.updateStatus { copy(isSaving = false) }.withError(result.errorType)
        }
        else -> super.getStateByResult(previous, result)
    }

    private fun applyExpenseDetailResult(
        previous: AddExpenseState,
        expense: ExpenseDetail,
    ): AddExpenseState = super.getStateByResult(
        previous,
        updateState { applyExpenseDetail(this, expense) },
    )

    override fun hasUnsavedChanges(form: ExpenseFormInput, editContext: EditContext): Boolean {
        val initialForm = editContext.initialForm ?: return false
        return form != initialForm
    }

    override fun handleDeleteClicked(): Flow<DomainResult> = flowOf(
        updateState { updateStatus { copy(showDeleteDialog = true) } }
    )

    override fun handleConfirmDelete(): Flow<DomainResult> = flow {
        emit(updateState { updateStatus { copy(showDeleteDialog = false, isSaving = true) } })
        val id = state.value.editContext.existingExpenseId ?: return@flow

        val result = deleteExpenseUseCase(id)
        emit(result)
        if (result is DeleteExpenseUseCase.Result.Success) {
            emit(ExpenseResult.NavigateBack)
        }
    }

    override fun handleDismissDeleteDialog(): Flow<DomainResult> = flowOf(
        updateState { updateStatus { copy(showDeleteDialog = false) } }
    )

    private fun applyExpenseDetail(
        previous: AddExpenseState,
        expense: ExpenseDetail,
    ): AddExpenseState {
        val hasConversion = expense.originalCurrencyCode != null &&
            expense.conversionRate != null &&
            expense.originalAmountMinorUnits != null

        val amountToDisplay = if (hasConversion) {
            expense.originalAmountMinorUnits
        } else {
            expense.amountMinorUnits
        }
        val formattedAmount = keyboardCalculator.formatFromMinorUnits(amountToDisplay)
        val amountInput = if (formattedAmount.endsWith(".00")) {
            formattedAmount.removeSuffix(".00")
        } else {
            formattedAmount
        }
        keyboardCalculator.setFromAmount(amountInput)
        val keyboardState = keyboardCalculator.getState()

        val loadedForm = previous.form.copy(
            amountInput = amountInput,
            amountDisplayExpression = keyboardState.displayExpression,
            isInExpressionMode = keyboardState.isInExpressionMode,
            title = expense.title,
            selectedCategoryId = expense.categoryId,
            selectedSubcategoryId = expense.subcategoryId,
            dateEpochMillis = expense.dateEpochMillis,
            notes = expense.notes.orEmpty(),
        )

        val conversionState = if (hasConversion) {
            previous.conversion.copy(
                rate = expense.conversionRate,
                fetchedRate = expense.conversionRate,
                isManualOverride = true,
                convertedAmountMinorUnits = expense.amountMinorUnits,
            )
        } else {
            previous.conversion
        }

        return previous.copy(
            editContext = previous.editContext.copy(
                existingExpenseId = expense.id,
                initialForm = loadedForm,
            ),
            form = loadedForm,
            conversion = conversionState,
        )
    }
}
