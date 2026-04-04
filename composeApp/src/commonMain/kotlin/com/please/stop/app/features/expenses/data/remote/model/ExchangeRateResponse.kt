package com.please.stop.app.features.expenses.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRateResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
)
