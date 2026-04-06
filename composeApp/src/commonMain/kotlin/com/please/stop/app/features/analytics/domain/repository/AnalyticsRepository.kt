package com.please.stop.app.features.analytics.domain.repository

import com.please.stop.app.features.analytics.domain.model.AnalyticsData
import com.please.stop.app.features.analytics.domain.model.DayExpensesData
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {

    fun observeAnalyticsData(): Flow<AnalyticsData>

    suspend fun loadDayExpenses(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Result<DayExpensesData>
}
