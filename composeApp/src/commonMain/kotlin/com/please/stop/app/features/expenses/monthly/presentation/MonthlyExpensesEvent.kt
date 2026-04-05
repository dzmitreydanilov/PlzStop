package com.please.stop.app.features.expenses.monthly.presentation

sealed interface MonthlyExpensesEvent {
    data object PreviousMonthClicked : MonthlyExpensesEvent
    data object NextMonthClicked : MonthlyExpensesEvent
    data class ExpenseClicked(val expenseId: Long) : MonthlyExpensesEvent
    data class ReceiptGroupClicked(val receiptId: Long) : MonthlyExpensesEvent
}
