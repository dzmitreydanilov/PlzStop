package com.please.stop.app.features.expenses.edit.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.expenses.domain.model.ExpenseDetail
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class GetExpenseByIdUseCase(
    private val repository: AddExpenseRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(id: Long): DomainResult = withContext(ioDispatcher) {
        repository.getExpenseById(id).fold(
            onSuccess = { expense ->
                if (expense != null) Result.Success(expense) else Result.NotFound
            },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    sealed interface Result : DomainResult {
        data class Success(val expense: ExpenseDetail) : Result
        data object NotFound : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
