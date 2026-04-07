package com.please.stop.app.navigation.tabs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.features.analytics.monthly.presentation.ui.MonthlyExpensesScreen
import com.please.stop.app.navigation.routes.MainBottomTabs

internal fun EntryProviderScope<NavKey>.operationsTabEntries(
    onNavigateToEditExpense: (expenseId: Long) -> Unit,
    onNavigateToCreateExpense: () -> Unit,
) {
    entry<MainBottomTabs.Operations> {
        MonthlyExpensesScreen(
            onNavigateToEditExpense = onNavigateToEditExpense,
            onNavigateToCreateExpense = onNavigateToCreateExpense,
        )
    }
}
