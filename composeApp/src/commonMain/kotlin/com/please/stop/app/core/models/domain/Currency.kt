package com.please.stop.app.core.models.domain

import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import kotlinx.serialization.Serializable

@Serializable
data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val decimalPlaces: Int = DEFAULT_CURRENCY_DECIMAL_PLACES,
)
