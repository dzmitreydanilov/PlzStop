package com.please.stop.app.features.onboarding.data.repository

/**
 * Fetches JSON data from Firebase Remote Config.
 *
 * On Android: uses Firebase Remote Config SDK.
 * On iOS: returns null (falls back to bundled JSON) until a compatible SDK is available.
 *
 * Remote Config parameter keys:
 * - [KEY_CURRENCIES] — JSON array of currency objects
 * - [KEY_DEFAULT_CATEGORIES] — JSON array of default category objects
 */
interface RemoteConfigDataSource {

    suspend fun fetchString(key: String): String?

    suspend fun fetchBoolean(key: String): Boolean

    companion object {
        const val KEY_CURRENCIES = "currencies"
        const val KEY_DEFAULT_CATEGORIES = "default_categories"
        const val KEY_DEFAULT_SUBCATEGORIES = "default_subcategories"
    }
}
