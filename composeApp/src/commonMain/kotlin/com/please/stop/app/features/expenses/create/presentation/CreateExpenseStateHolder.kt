package com.please.stop.app.features.expenses.create.presentation

import com.please.stop.app.features.expenses.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder
import com.please.stop.app.features.expenses.presentation.EditContext
import com.please.stop.app.features.expenses.presentation.ExpenseFormInput
import com.please.stop.app.utils.date.nowMillis

class CreateExpenseStateHolder(
    private val preselectedCategoryId: Long?,
    observeFormDataUseCase: ObserveAddExpenseFormDataUseCase,
    saveExpenseUseCase: SaveExpenseUseCase,
    analyzeReceiptUseCase: AnalyzeReceiptUseCase,
) : BaseExpenseStateHolder(
    observeFormDataUseCase = observeFormDataUseCase,
    saveExpenseUseCase = saveExpenseUseCase,
    analyzeReceiptUseCase = analyzeReceiptUseCase,
) {
    override val tag = "CreateExpenseStateHolder"

    override val editContext = EditContext(
        isEditMode = false,
        existingExpenseId = null,
    )

    override fun buildInitialForm() = ExpenseFormInput(
        selectedCategoryId = preselectedCategoryId,
        dateEpochMillis = nowMillis(),
    )

    override fun hasUnsavedChanges(form: ExpenseFormInput, editContext: EditContext): Boolean {
        return form.amountInput.isNotEmpty() ||
            form.title.isNotBlank() ||
            form.selectedCategoryId != preselectedCategoryId ||
            form.selectedSubcategoryId != null ||
            form.notes.isNotBlank()
    }
}
