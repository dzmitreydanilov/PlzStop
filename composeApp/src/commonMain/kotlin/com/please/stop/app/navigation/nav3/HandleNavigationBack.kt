package com.please.stop.app.navigation.nav3

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Simplified wrapper around [NavigationBackHandler] that handles
 * [rememberNavigationEventState] boilerplate internally.
 *
 * @param enabled Whether the back handler is active. Defaults to `true`.
 * @param onBack Callback invoked when the system back navigation completes.
 */
@Composable
fun HandleNavigationBack(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    val navigationEventState = rememberNavigationEventState<NavigationEventInfo>(
        currentInfo = NavigationEventInfo.None,
    )
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = enabled,
        onBackCompleted = onBack,
    )
}
