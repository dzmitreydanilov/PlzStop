package com.please.stop.app.core.featureflags

import com.please.stop.app.features.onboarding.data.repository.RemoteConfigDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FeatureFlagsImpl(
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : FeatureFlags {

    private val mutex = Mutex()
    private val flags = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override fun observeSubcategoriesEnabled(): Flow<Boolean> {
        return observeFlag(FeatureFlagKey.SUBCATEGORIES_ENABLED)
    }

    override suspend fun subcategoriesEnabled(): Boolean {
        return getFlag(FeatureFlagKey.SUBCATEGORIES_ENABLED)
    }

    override suspend fun refresh() {
        val updated = FeatureFlagKey.entries.associate { key ->
            key.remoteConfigKey to remoteConfigDataSource.fetchBoolean(key.remoteConfigKey)
        }
        mutex.withLock {
            flags.value = updated
        }
    }

    private fun observeFlag(key: FeatureFlagKey): Flow<Boolean> {
        return flags.map { it[key.remoteConfigKey] ?: false }
    }

    private suspend fun getFlag(key: FeatureFlagKey): Boolean {
        val current = flags.value[key.remoteConfigKey]
        if (current != null) return current

        val value = remoteConfigDataSource.fetchBoolean(key.remoteConfigKey)
        mutex.withLock {
            flags.value = flags.value + (key.remoteConfigKey to value)
        }
        return value
    }
}
