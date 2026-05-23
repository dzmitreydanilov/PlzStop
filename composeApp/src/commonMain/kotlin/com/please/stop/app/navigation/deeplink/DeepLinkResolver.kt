package com.please.stop.app.navigation.deeplink

import com.please.stop.app.navigation.routes.MainBottomTabs

/**
 * Resolves deep link URIs to navigation targets.
 *
 * Supported URL patterns:
 * - `home` → Home tab
 * - `analytics` → Analytics tab
 * - `settings` → Settings tab
 */
class DeepLinkResolver {

    fun resolve(data: DeepLinkData): DeepLinkResult? {
        val segments = data.pathSegments
        if (segments.isEmpty()) return null

        return when (segments[0]) {
            "home" -> DeepLinkResult.TabRoute(tab = MainBottomTabs.Home)
            "analytics" -> DeepLinkResult.TabRoute(tab = MainBottomTabs.Analytics)
            "settings" -> DeepLinkResult.TabRoute(tab = MainBottomTabs.Settings)
            "open" -> {
                val url = data.queryParams["url"] ?: return null
                DeepLinkResult.OpenExternalUrl(url)
            }
            else -> null
        }
    }
}
