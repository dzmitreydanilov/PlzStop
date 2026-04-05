package com.please.stop.app.features.expenses.monthly.presentation

import com.please.stop.app.core.models.presentation.Navigation

internal sealed interface MonthlyExpensesNavigation : Navigation {
    data class OpenEditExpense(val expenseId: Long) : MonthlyExpensesNavigation
}
