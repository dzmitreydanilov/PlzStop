package com.please.stop.app.navigation.tabs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.features.home.presentation.ui.HomeScreen
import com.please.stop.app.navigation.routes.MainBottomTabs

internal fun EntryProviderScope<NavKey>.homeTabEntries(
    onNavigateToAddExpense: (categoryId: Long) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    entry<MainBottomTabs.Home> {
        HomeScreen(
            onNavigateToAddExpense = onNavigateToAddExpense,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}
