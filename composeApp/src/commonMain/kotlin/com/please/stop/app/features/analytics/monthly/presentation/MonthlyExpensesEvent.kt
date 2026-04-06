package com.please.stop.app.features.analytics.monthly.presentation

sealed interface MonthlyExpensesEvent {
    data class MonthSelected(val year: Int, val month: Int) : MonthlyExpensesEvent
    data class ExpenseClicked(val expenseId: Long) : MonthlyExpensesEvent
    data class ExpenseLongClicked(val expenseId: Long) : MonthlyExpensesEvent
    data class ConfirmDeleteExpense(val expenseId: Long) : MonthlyExpensesEvent
    data object DismissDeleteDialog : MonthlyExpensesEvent
    data class ReceiptGroupClicked(val receiptId: Long, val year: Int, val month: Int) : MonthlyExpensesEvent
}
