package com.please.stop.app.features.onboarding.domain.repository

import com.please.stop.app.features.onboarding.domain.model.Currency

interface CurrencyRepository {
    suspend fun getAllCurrencies(): Result<List<Currency>>
}
