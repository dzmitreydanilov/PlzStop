package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import com.please.stop.app.core.models.domain.Result as DomainResult

class ObserveAddExpenseFormDataUseCase(
    private val repository: AddExpenseRepository,
    private val dispatcher: CoroutineDispatcher
) {

    operator fun invoke(): Flow<DomainResult> {
        return repository.observeFormData()
            .map<AddExpenseFormData, DomainResult> {
                ObserveAddExpenseFormDataResult.Success(it)
            }
            .catch { emit(ObserveAddExpenseFormDataResult.Failure(it.toErrorType())) }
            .flowOn(dispatcher)
    }
}

sealed interface ObserveAddExpenseFormDataResult : DomainResult {
    data class Success(val data: AddExpenseFormData) : ObserveAddExpenseFormDataResult
    data class Failure(override val errorType: ErrorType) :
        ObserveAddExpenseFormDataResult,
        ErrorResult
}
