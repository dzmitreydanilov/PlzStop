package com.please.stop.app.features.home.presentation

import com.please.stop.app.core.models.presentation.Navigation

internal sealed interface HomeNavigation : Navigation {
    data class NavigateToAddExpense(val expenseCategoryId: Long) : HomeNavigation
    data object NavigateToCreateExpense : HomeNavigation
    data object NavigateToSettings : HomeNavigation
    data object NavigateToMonthlyExpenses : HomeNavigation
}
