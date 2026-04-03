package com.please.stop.app.navigation.tabs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.features.analytics.presentation.ui.AnalyticsScreen
import com.please.stop.app.navigation.routes.MainBottomTabs

internal fun EntryProviderScope<NavKey>.analyticsTabEntries() {
    entry<MainBottomTabs.Analytics> {
        AnalyticsScreen()
    }
}
