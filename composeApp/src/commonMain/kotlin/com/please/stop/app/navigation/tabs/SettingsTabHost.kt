package com.please.stop.app.navigation.tabs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.features.settings.presentation.SettingsScreenRoute
import com.please.stop.app.navigation.routes.MainBottomTabs

internal fun EntryProviderScope<NavKey>.settingsTabEntries(
    onNavigateToUser: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToExportData: () -> Unit,
) {
    entry<MainBottomTabs.Settings> {
        SettingsScreenRoute(
            onNavigateToUser = onNavigateToUser,
            onNavigateToCategories = onNavigateToCategories,
            onNavigateToSubscriptions = onNavigateToSubscriptions,
            onNavigateToExportData = onNavigateToExportData,
        )
    }
}
