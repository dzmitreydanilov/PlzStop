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

    suspend operator fun invoke(id: Long): GetExpenseByIdResult = withContext(ioDispatcher) {
        repository.getExpenseById(id).fold(
            onSuccess = { expense ->
                if (expense != null) GetExpenseByIdResult.Success(expense) else GetExpenseByIdResult.NotFound
            },
            onFailure = { GetExpenseByIdResult.Failure(it.toErrorType()) },
        )
    }
}

sealed interface GetExpenseByIdResult : DomainResult {
    data class Success(val expense: ExpenseDetail) : GetExpenseByIdResult
    data object NotFound : GetExpenseByIdResult
    data class Failure(override val errorType: ErrorType) : GetExpenseByIdResult, ErrorResult
}
