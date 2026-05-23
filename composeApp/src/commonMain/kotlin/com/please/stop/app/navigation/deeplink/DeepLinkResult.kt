package com.please.stop.app.navigation.deeplink

import androidx.navigation3.runtime.NavKey
import com.please.stop.app.navigation.routes.MainBottomTabs

/**
 * Result of resolving a deep link URI to a navigation target.
 *
 * Two variants handle the distinct navigation mechanisms in the app:
 * - [GlobalRoute] navigates via [Router.replaceStack] for routes in the root backstack
 * - [TabRoute] switches bottom tabs via [BottomNavIntentHolder], optionally pushing a nested route
 */
sealed interface DeepLinkResult {

    /**
     * Navigate to a global route with a synthetic backstack.
     *
     * The backstack is ordered root-first (e.g., [Home, Profile]).
     */
    data class GlobalRoute(val backstack: List<NavKey>) : DeepLinkResult

    /**
     * Switch to a bottom navigation tab, optionally pushing a nested route within it.
     */
    data class TabRoute(
        val tab: MainBottomTabs,
        val nestedRoute: NavKey? = null
    ) : DeepLinkResult

    /**
     * Open an external URL (e.g., a Google Sheets spreadsheet).
     */
    data class OpenExternalUrl(val url: String) : DeepLinkResult
}
