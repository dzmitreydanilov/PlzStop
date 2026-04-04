package com.please.stop.app.core.featureflags

internal enum class FeatureFlagKey(val remoteConfigKey: String) {
    SUBCATEGORIES_ENABLED("subcategories_enabled"),
    CURRENCY_CONVERSION_ENABLED("currency_conversion_enabled"),
}
