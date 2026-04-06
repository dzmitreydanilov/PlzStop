package com.please.stop.app.features.analytics.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.analytics.domain.model.DayExpensesData
import com.please.stop.app.features.analytics.domain.repository.AnalyticsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class LoadDayExpensesUseCase(
    private val analyticsRepository: AnalyticsRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Result = withContext(dispatcher) {
        analyticsRepository.loadDayExpenses(year, month, dayOfMonth)
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failure(it.toErrorType()) },
            )
    }

    sealed interface Result : DomainResult {
        data class Success(val data: DayExpensesData) : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
