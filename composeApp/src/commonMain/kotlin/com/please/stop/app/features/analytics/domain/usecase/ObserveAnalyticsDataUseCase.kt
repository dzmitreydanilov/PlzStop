package com.please.stop.app.features.analytics.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.analytics.domain.model.AnalyticsData
import com.please.stop.app.features.analytics.domain.repository.AnalyticsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.please.stop.app.core.models.domain.Result as DomainResult

class ObserveAnalyticsDataUseCase(
    private val analyticsRepository: AnalyticsRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    operator fun invoke(): Flow<DomainResult> {
        return analyticsRepository.observeAnalyticsData()
            .map<AnalyticsData, DomainResult> { Result.Success(it) }
            .catch { emit(Result.Failure(it.toErrorType())) }
            .onStart { emit(Result.Loading) }
            .flowOn(dispatcher)
    }

    sealed interface Result : DomainResult {
        data object Loading : Result
        data class Success(val data: AnalyticsData) : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
