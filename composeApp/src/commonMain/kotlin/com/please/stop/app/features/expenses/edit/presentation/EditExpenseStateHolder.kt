package com.please.stop.app.features.expenses.edit.presentation

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.expenses.domain.model.ExpenseDetail
import com.please.stop.app.features.expenses.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.expenses.domain.usecase.FetchExchangeRateUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.DeleteExpenseUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.GetExpenseByIdUseCase
import com.please.stop.app.features.expenses.presentation.AddExpenseState
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder
import com.please.stop.app.features.expenses.presentation.ConversionState
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
    fetchExchangeRateUseCase: FetchExchangeRateUseCase,
) : BaseExpenseStateHolder(
    observeFormDataUseCase = observeFormDataUseCase,
    saveExpenseUseCase = saveExpenseUseCase,
    analyzeReceiptUseCase = analyzeReceiptUseCase,
    fetchExchangeRateUseCase = fetchExchangeRateUseCase,
) {
    override val tag = "EditExpenseStateHolder"

    override val editContext = EditContext(
        isEditMode = true,
        existingExpenseId = expenseId,
    )

    override fun buildInitialForm() = ExpenseFormInput(
        dateEpochMillis = nowMillis(),
    )

    override suspend fun bootstrap(emit: suspend (DomainResult) -> Unit) {
        val result = getExpenseByIdUseCase(expenseId)
        emit(result)
    }

    override fun getStateByResult(
        previous: AddExpenseState,
        result: DomainResult,
    ): AddExpenseState = when (result) {
        is GetExpenseByIdUseCase.Result.Success -> {
            applyExpenseDetail(previous, result.expense)
        }
        is GetExpenseByIdUseCase.Result.NotFound -> {
            previous.toError(ErrorType.Unknown("Expense not found"))
        }
        is GetExpenseByIdUseCase.Result.Failure -> {
            previous.toError(result.errorType)
        }
        is DeleteExpenseUseCase.Result.Failure -> {
            val content = previous.asContent() ?: return previous
            content.updateStatus { copy(isSaving = false) }.toError(result.errorType)
        }
        else -> super.getStateByResult(previous, result)
    }

    override fun hasUnsavedChanges(form: ExpenseFormInput, editContext: EditContext): Boolean {
        val initialForm = editContext.initialForm ?: return false
        return form != initialForm
    }

    override fun handleDeleteClicked(): Flow<DomainResult> = flowOf(
        updateContent { updateStatus { copy(showDeleteDialog = true) } }
    )

    override fun handleConfirmDelete(): Flow<DomainResult> = flow {
        emit(updateContent { updateStatus { copy(showDeleteDialog = false, isSaving = true) } })
        val content = state.value.asContent() ?: return@flow
        val id = content.editContext.existingExpenseId ?: return@flow

        val result = deleteExpenseUseCase(id)
        emit(result)
        if (result is DeleteExpenseUseCase.Result.Success) {
            emit(ExpenseResult.NavigateBack)
        }
    }

    override fun handleDismissDeleteDialog(): Flow<DomainResult> = flowOf(
        updateContent { updateStatus { copy(showDeleteDialog = false) } }
    )

    private fun applyExpenseDetail(
        previous: AddExpenseState,
        expense: ExpenseDetail,
    ): AddExpenseState {
        val content = previous.asContent() ?: return previous

        val hasConversion = expense.originalCurrencyCode != null &&
            expense.conversionRate != null &&
            expense.originalAmountMinorUnits != null

        val amountToDisplay = if (hasConversion) {
            expense.originalAmountMinorUnits
        } else {
            expense.amountMinorUnits
        }

        val loadedForm = content.form.copy(
            amountInput = keyboardCalculator.formatFromMinorUnits(amountToDisplay),
            title = expense.title,
            selectedCategoryId = expense.categoryId,
            selectedSubcategoryId = expense.subcategoryId,
            dateEpochMillis = expense.dateEpochMillis,
            notes = expense.notes.orEmpty(),
        )

        val conversionState = if (hasConversion) {
            content.conversion.copy(
                rate = expense.conversionRate,
                fetchedRate = expense.conversionRate,
                isManualOverride = true,
                convertedAmountMinorUnits = expense.amountMinorUnits,
            )
        } else {
            content.conversion
        }

        return content.copy(
            editContext = content.editContext.copy(
                existingExpenseId = expense.id,
                initialForm = loadedForm,
            ),
            form = loadedForm,
            conversion = conversionState,
        )
    }
}
