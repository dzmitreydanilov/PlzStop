package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.expenses.domain.model.MonthlyExpensesData
import com.please.stop.app.features.expenses.domain.repository.MonthlyExpensesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import com.please.stop.app.core.models.domain.Result as DomainResult

class ObserveMonthlyExpensesUseCase(
    private val repository: MonthlyExpensesRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(year: Int, month: Int): Flow<DomainResult> {
        return repository.observeExpensesForMonth(year, month)
            .map<kotlin.Result<MonthlyExpensesData>, DomainResult> { result ->
                result.fold(
                    onSuccess = { Result.Success(it) },
                    onFailure = { Result.Failure(it.toErrorType()) },
                )
            }
            .catch { emit(Result.Failure(it.toErrorType())) }
            .flowOn(dispatcher)
    }

    sealed interface Result : DomainResult {
        data class Success(val data: MonthlyExpensesData) : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
