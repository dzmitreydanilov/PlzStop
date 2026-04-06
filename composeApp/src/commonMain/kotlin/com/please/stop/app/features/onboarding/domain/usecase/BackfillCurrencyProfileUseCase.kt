package com.please.stop.app.features.onboarding.domain.usecase

import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class BackfillCurrencyProfileUseCase(
    private val userProfileDao: UserProfileDao,
    private val currencyRepository: CurrencyRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke() = withContext(ioDispatcher) {
        val profile = userProfileDao.get() ?: return@withContext
        if (profile.currencySymbol.isNotEmpty()) return@withContext

        val currencies = currencyRepository.getAllCurrencies().getOrNull() ?: return@withContext
        val currency = currencies.find { it.code == profile.currencyCode } ?: return@withContext

        userProfileDao.upsert(
            profile.copy(
                currencySymbol = currency.symbol,
                decimalPlaces = currency.decimalPlaces,
            )
        )
    }
}
