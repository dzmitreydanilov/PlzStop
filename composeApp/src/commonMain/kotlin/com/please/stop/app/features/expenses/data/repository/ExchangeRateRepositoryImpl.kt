package com.please.stop.app.features.expenses.data.repository

import com.please.stop.app.core.flow.flowFromSuspend
import com.please.stop.app.core.models.data.mapToResult
import com.please.stop.app.features.expenses.data.remote.ExchangeRateApiService
import com.please.stop.app.features.expenses.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ExchangeRateRepositoryImpl(
    private val apiService: ExchangeRateApiService,
) : ExchangeRateRepository {

    private val cache = mutableMapOf<Triple<String, String, String?>, Double>()

    override fun getRate(from: String, to: String, date: String?): Flow<Result<Double>> {
        val key = Triple(from, to, date)
        cache[key]?.let { return flowOf(Result.success(it)) }

        return flowFromSuspend { apiService.getRate(from, to, date) }
            .mapToResult { response -> response.rates[to] ?: error("Rate for $to not found") }
            .map { result ->
                result.onSuccess { rate -> cache[key] = rate }
                result
            }
    }
}
