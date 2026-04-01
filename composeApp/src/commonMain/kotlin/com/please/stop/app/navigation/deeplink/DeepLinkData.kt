package com.please.stop.app.navigation.deeplink

/**
 * Platform-agnostic representation of a parsed deep link URI.
 *
 * @param pathSegments URL path segments (e.g., ["reminder", "edit", "123", "456"])
 * @param queryParams URL query parameters (e.g., {"date" to "2024-01-01"})
 */
data class DeepLinkData(
    val pathSegments: List<String>,
    val queryParams: Map<String, String>
)

/**
 * Parses a URI string into [DeepLinkData].
 *
 * Platform implementations use native URI parsing APIs
 * (Android `Uri`, iOS `NSURL`).
 */
expect fun parseDeepLinkUri(uriString: String): DeepLinkData?
