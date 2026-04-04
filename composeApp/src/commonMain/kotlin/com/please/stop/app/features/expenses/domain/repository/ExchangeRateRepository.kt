package com.please.stop.app.features.expenses.domain.repository

import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun getRate(from: String, to: String, date: String? = null): Flow<Result<Double>>
}
