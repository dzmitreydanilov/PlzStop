package com.please.stop.app.features.subscriptions.data.storage

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.please.stop.app.kvs.IKvs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface ISubscriptionPromoStorage {
    fun getDismissedTimestamp(): Flow<Long?>
    fun getShownCount(): Flow<Int>
    suspend fun recordDismissal(timestamp: Long)
}

class SubscriptionPromoStorage(
    private val kvs: IKvs,
) : ISubscriptionPromoStorage {

    override fun getDismissedTimestamp(): Flow<Long?> {
        return kvs.getLong(DISMISSED_TIMESTAMP)
    }

    override fun getShownCount(): Flow<Int> {
        return kvs.getInt(SHOWN_COUNT).map { it ?: 0 }
    }

    override suspend fun recordDismissal(timestamp: Long) {
        kvs.saveLong(DISMISSED_TIMESTAMP, timestamp)
        val current = kvs.getInt(SHOWN_COUNT).first() ?: 0
        kvs.saveInt(SHOWN_COUNT, current + 1)
    }

    private companion object {
        val DISMISSED_TIMESTAMP = longPreferencesKey("subscription_promo_dismissed_ts")
        val SHOWN_COUNT = intPreferencesKey("subscription_promo_shown_count")
    }
}
