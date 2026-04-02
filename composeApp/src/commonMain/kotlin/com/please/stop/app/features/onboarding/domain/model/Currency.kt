package com.please.stop.app.features.onboarding.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val decimalPlaces: Int,
    val isPopular: Boolean,
)
