package com.please.stop.app.features.auth.data

import com.please.stop.app.core.models.data.GoogleAccountLink
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object GoogleAccountLinkCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(link: GoogleAccountLink): String = json.encodeToString(link)

    fun decode(value: String): GoogleAccountLink? = runCatching {
        json.decodeFromString<GoogleAccountLink>(value)
    }.getOrNull() ?: decodeLegacy(value)

    private fun decodeLegacy(value: String): GoogleAccountLink? {
        val parts = value.split("|")
        if (parts.size != LEGACY_PART_COUNT) return null

        return GoogleAccountLink(
            email = parts[0],
            isConnected = parts[1].toBooleanStrictOrNull() ?: return null,
        )
    }

    private const val LEGACY_PART_COUNT = 2
}
