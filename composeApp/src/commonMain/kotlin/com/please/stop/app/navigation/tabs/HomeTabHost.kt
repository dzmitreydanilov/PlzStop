package com.please.stop.app.navigation.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.navigation.routes.MainBottomTabs
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_home_coming_soon

internal fun EntryProviderScope<NavKey>.homeTabEntries(
    onNavigateOnboarding: () -> Unit,
) {
    entry<MainBottomTabs.Home> {
        HomePlaceholderScreen(onNavigateOnboarding = onNavigateOnboarding)
    }
}

@Composable
private fun HomePlaceholderScreen(onNavigateOnboarding: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.onboarding_home_coming_soon))
        OutlinedButton(onClick = onNavigateOnboarding) {
            Text("Open Onboarding")
        }
    }
}
