package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.expenses.domain.model.MonthlyExpensesData
import com.please.stop.app.features.expenses.domain.repository.MonthlyExpensesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ObserveMonthlyExpensesUseCase(
    private val repository: MonthlyExpensesRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(year: Int, month: Int): Flow<ObserveMonthlyExpensesResult> {
        return repository.observeExpensesForMonth(year, month)
            .map { result ->
                result.fold(
                    onSuccess = { ObserveMonthlyExpensesResult.Success(year, month, it) },
                    onFailure = {
                        ObserveMonthlyExpensesResult.Failure(
                            year,
                            month,
                            it.toErrorType()
                        )
                    },
                )
            }
            .catch { emit(ObserveMonthlyExpensesResult.Failure(year, month, it.toErrorType())) }
            .flowOn(dispatcher)
    }
}

sealed interface ObserveMonthlyExpensesResult : Result {
    val year: Int
    val month: Int

    data class Success(
        override val year: Int,
        override val month: Int,
        val data: MonthlyExpensesData
    ) : ObserveMonthlyExpensesResult

    data class Failure(
        override val year: Int,
        override val month: Int,
        override val errorType: ErrorType
    ) : ObserveMonthlyExpensesResult, ErrorResult
}
