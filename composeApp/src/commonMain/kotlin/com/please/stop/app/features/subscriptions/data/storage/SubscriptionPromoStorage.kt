package com.please.stop.app.features.subscriptions.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ISubscriptionPromoStorage {
    fun getDismissedTimestamp(): Flow<Long?>
    fun getShownCount(): Flow<Int>
    suspend fun recordDismissal(timestamp: Long)
}

class SubscriptionPromoStorage(
    private val dataStore: DataStore<Preferences>,
) : ISubscriptionPromoStorage {

    override fun getDismissedTimestamp(): Flow<Long?> {
        return dataStore.data.map { prefs -> prefs[DISMISSED_TIMESTAMP] }
    }

    override fun getShownCount(): Flow<Int> {
        return dataStore.data.map { prefs -> prefs[SHOWN_COUNT] ?: 0 }
    }

    override suspend fun recordDismissal(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[DISMISSED_TIMESTAMP] = timestamp
            prefs[SHOWN_COUNT] = (prefs[SHOWN_COUNT] ?: 0) + 1
        }
    }

    private companion object {
        val DISMISSED_TIMESTAMP = longPreferencesKey("subscription_promo_dismissed_ts")
        val SHOWN_COUNT = intPreferencesKey("subscription_promo_shown_count")
    }
}
