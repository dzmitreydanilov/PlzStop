package com.please.stop.app.features.analytics.monthly.presentation

sealed interface OperationsEvent {
    data class MonthSelected(val year: Int, val month: Int) : OperationsEvent
    data class ExpenseClicked(val expenseId: Long) : OperationsEvent
    data class ExpenseLongClicked(val expenseId: Long) : OperationsEvent
    data class ConfirmDeleteExpense(val expenseId: Long) : OperationsEvent
    data object DismissDeleteDialog : OperationsEvent
    data class ReceiptGroupClicked(val receiptId: Long, val year: Int, val month: Int) : OperationsEvent
}
