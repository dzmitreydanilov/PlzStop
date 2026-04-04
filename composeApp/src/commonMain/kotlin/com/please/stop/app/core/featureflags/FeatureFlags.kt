package com.please.stop.app.core.featureflags

import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all feature flags.
 * Backed by Remote Config with reactive updates via [StateFlow].
 *
 * To add a new flag:
 * 1. Add an observe + get method pair here
 * 2. Add the key to [FeatureFlagKey]
 * 3. Implement in [FeatureFlagsImpl]
 */
interface FeatureFlags {

    fun observeSubcategoriesEnabled(): Flow<Boolean>

    suspend fun subcategoriesEnabled(): Boolean

    fun observeCurrencyConversionEnabled(): Flow<Boolean>

    suspend fun currencyConversionEnabled(): Boolean

    suspend fun refresh()
}
