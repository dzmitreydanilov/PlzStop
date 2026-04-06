package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.core.models.domain.Result as DomainResult

class FetchAndApplyExchangeRateUseCase(
    private val fetchExchangeRateUseCase: FetchExchangeRateUseCase,
) {

    sealed interface Result : DomainResult {
        data class RateFetched(val rate: Double) : Result
        data object FetchFailed : Result
        data object Disabled : Result
    }

    suspend operator fun invoke(from: String, to: String, date: String): Result =
        when (val result = fetchExchangeRateUseCase(from = from, to = to, date = date)) {
            is FetchExchangeRateUseCase.Result.Success -> Result.RateFetched(result.rate)
            is FetchExchangeRateUseCase.Result.Failure -> Result.FetchFailed
            is FetchExchangeRateUseCase.Result.Disabled -> Result.Disabled
        }
}
