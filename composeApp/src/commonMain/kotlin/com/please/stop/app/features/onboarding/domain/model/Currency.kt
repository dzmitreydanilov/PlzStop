package com.please.stop.app.features.onboarding.domain.model

import kotlinx.serialization.Serializable
import com.please.stop.app.core.models.domain.Currency as CoreCurrency

@Serializable
data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val decimalPlaces: Int,
) {
    fun toCoreCurrency(): CoreCurrency = CoreCurrency(
        code = code,
        symbol = symbol,
        name = name,
        decimalPlaces = decimalPlaces,
    )
}
