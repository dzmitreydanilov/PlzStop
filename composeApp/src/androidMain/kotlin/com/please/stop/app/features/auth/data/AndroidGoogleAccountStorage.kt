package com.please.stop.app.features.auth.data

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.models.data.GoogleAccountLink
import com.please.stop.app.crypto.Crypto
import kotlinx.coroutines.flow.first

internal class AndroidGoogleAccountStorage(
    private val dataStore: DataStore<Preferences>,
) : IGoogleAccountStorage {

    override suspend fun write(link: GoogleAccountLink) {
        val value = "${link.email}|${link.isConnected}"
        val encrypted = Crypto.encrypt(value.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        dataStore.edit { prefs -> prefs[KEY_DATA] = encoded }
    }

    override suspend fun read(): GoogleAccountLink? {
        val encoded = dataStore.data.first()[KEY_DATA] ?: return null
        val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
        val decrypted = Crypto.decrypt(encrypted).toString(Charsets.UTF_8)
        val parts = decrypted.split("|")
        if (parts.size != 2) return null
        return GoogleAccountLink(
            email = parts[0],
            isConnected = parts[1].toBooleanStrictOrNull() ?: false,
        )
    }

    override suspend fun delete() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_DATA = stringPreferencesKey("data")
    }
}
