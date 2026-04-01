package com.please.stop.app.navigation.deeplink

import androidx.core.net.toUri

/**
 * Parses a deep link URI string into [DeepLinkData].
 *
 * Handles both custom scheme (`dogcare://profile`) and HTTPS (`https://dogcare.app/profile`) URIs.
 *
 * For custom scheme URIs the first path component is parsed as the **host** by [android.net.Uri],
 * e.g. `dogcare://reminder/edit/123` → host="reminder", path="/edit/123".
 * This method normalizes by prepending the host to path segments so the resolver
 * always sees `["reminder", "edit", "123"]`.
 */
actual fun parseDeepLinkUri(uriString: String): DeepLinkData? {
    return try {
        val uri = uriString.toUri()
        val rawSegments = uri.pathSegments.orEmpty()
        val host = uri.host
        val scheme = uri.scheme

        // For custom schemes the first logical path component ends up as the host
        val isCustomScheme = scheme != null && scheme != "http" && scheme != "https"
        val pathSegments = if (isCustomScheme && !host.isNullOrEmpty()) {
            listOf(host) + rawSegments
        } else {
            rawSegments
        }

        DeepLinkData(
            pathSegments = pathSegments,
            queryParams = uri.queryParameterNames
                .orEmpty()
                .associateWith { uri.getQueryParameter(it).orEmpty() }
        )
    } catch (_: Exception) {
        null
    }
}
