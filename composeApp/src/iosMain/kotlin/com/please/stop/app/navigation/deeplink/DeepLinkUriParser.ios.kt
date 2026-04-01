package com.please.stop.app.navigation.deeplink

import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem

/**
 * Parses a deep link URI string into [DeepLinkData].
 *
 * Handles both custom scheme (`dogcare://profile`) and HTTPS (`https://dogcare.app/profile`) URIs.
 *
 * For custom scheme URIs the first path component is parsed as the **host** by [NSURLComponents],
 * e.g. `dogcare://reminder/edit/123` → host="reminder", path="/edit/123".
 * This method normalizes by prepending the host to path segments so the resolver
 * always sees `["reminder", "edit", "123"]`.
 */
actual fun parseDeepLinkUri(uriString: String): DeepLinkData? {
    val components = NSURLComponents(string = uriString) ?: return null

    val rawSegments = components.path
        ?.split("/")
        ?.filter { it.isNotEmpty() }
        .orEmpty()

    val host = components.host
    val scheme = components.scheme

    // For custom schemes the first logical path component ends up as the host
    val isCustomScheme = scheme != null && scheme != "http" && scheme != "https"
    val pathSegments = if (isCustomScheme && !host.isNullOrEmpty()) {
        listOf(host) + rawSegments
    } else {
        rawSegments
    }

    val queryParams = components.queryItems
        ?.associate { item ->
            val queryItem = item as NSURLQueryItem
            (queryItem.name ?: "") to (queryItem.value ?: "")
        }
        .orEmpty()

    return DeepLinkData(
        pathSegments = pathSegments,
        queryParams = queryParams
    )
}
