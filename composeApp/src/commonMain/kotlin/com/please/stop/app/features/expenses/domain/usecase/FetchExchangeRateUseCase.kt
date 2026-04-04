package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.core.featureflags.FeatureFlags
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.expenses.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class FetchExchangeRateUseCase(
    private val repository: ExchangeRateRepository,
    private val featureFlags: FeatureFlags,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(from: String, to: String, date: String? = null): Result =
        withContext(ioDispatcher) {
            if (!featureFlags.currencyConversionEnabled()) {
                return@withContext Result.Disabled
            }
            repository.getRate(from, to, date).first().fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failure(it.toErrorType()) },
            )
        }

    sealed interface Result : DomainResult {
        data class Success(val rate: Double) : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
        data object Disabled : Result
    }
}
