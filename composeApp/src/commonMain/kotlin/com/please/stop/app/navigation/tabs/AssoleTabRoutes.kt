package com.dog.care.navigation.tabs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dog.care.features.assol.AssolScreenRoute
import com.please.stop.app.navigation.routes.AuthRoute
import com.dog.care.navigation.routes.MainBottomTabs

internal fun EntryProviderScope<NavKey>.assolChatEntries(
    onNavigateSignIn: (AuthRoute) -> Unit,
    onNavigatePaywall: () -> Unit,
    onNavigateArticles: () -> Unit
) {
    entry<MainBottomTabs.AiAssistant> {
        AssolScreenRoute(
            onNavigateSignIn = onNavigateSignIn,
            onNavigatePaywall = onNavigatePaywall,
            onNavigateArticles = onNavigateArticles
        )
    }
}
