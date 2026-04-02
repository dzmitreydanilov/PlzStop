package com.please.stop.app.navigation.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.navigation.routes.MainBottomTabs
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_analytics_coming_soon

internal fun EntryProviderScope<NavKey>.analyticsTabEntries() {
    entry<MainBottomTabs.Analytics> {
        AnalyticsPlaceholderScreen()
    }
}

@Composable
private fun AnalyticsPlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(Res.string.onboarding_analytics_coming_soon))
    }
}
