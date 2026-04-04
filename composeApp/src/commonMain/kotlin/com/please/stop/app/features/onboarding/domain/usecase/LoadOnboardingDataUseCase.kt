package com.please.stop.app.features.onboarding.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class LoadOnboardingDataUseCase(
    private val currencyRepository: CurrencyRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): DomainResult = withContext(ioDispatcher) {
        currencyRepository.getAllCurrencies().fold(
            onSuccess = { currencies ->
                Result.Success(currencies = currencies.toImmutableList())
            },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    sealed interface Result : DomainResult {
        data class Success(
            val currencies: ImmutableList<Currency>,
        ) : Result

        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
