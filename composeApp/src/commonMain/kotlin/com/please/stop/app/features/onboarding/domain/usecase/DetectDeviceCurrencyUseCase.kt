package com.please.stop.app.features.onboarding.domain.usecase

import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import com.please.stop.app.utils.getDeviceCurrencyCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class DetectDeviceCurrencyUseCase(
    private val currencyRepository: CurrencyRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): DomainResult = withContext(ioDispatcher) {
        val deviceCode = getDeviceCurrencyCode()
            ?: return@withContext Result.NotFound

        currencyRepository.getAllCurrencies().fold(
            onSuccess = { currencies ->
                val matched = currencies.find { it.code.equals(deviceCode, ignoreCase = true) }
                if (matched != null) {
                    Result.Detected(matched)
                } else {
                    Result.NotFound
                }
            },
            onFailure = { Result.NotFound },
        )
    }

    sealed interface Result : DomainResult {
        data class Detected(val currency: Currency) : Result
        data object NotFound : Result
    }
}
