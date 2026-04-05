package com.please.stop.app.features.expenses.domain.repository

import com.please.stop.app.features.expenses.domain.model.CurrencyDisplayConfig
import com.please.stop.app.features.expenses.domain.model.MonthlyExpensesData
import kotlinx.coroutines.flow.Flow

interface MonthlyExpensesRepository {
    fun observeExpensesForMonth(year: Int, month: Int): Flow<Result<MonthlyExpensesData>>
    suspend fun getCurrencyConfig(): Result<CurrencyDisplayConfig>
}
