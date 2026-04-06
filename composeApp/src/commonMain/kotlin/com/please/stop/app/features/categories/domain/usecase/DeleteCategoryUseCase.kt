package com.please.stop.app.features.categories.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.categories.domain.model.ExpenseDeletionAction
import com.please.stop.app.features.categories.domain.repository.CategoriesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class DeleteCategoryUseCase(
    private val repository: CategoriesRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun checkExpenses(categoryId: Long): DomainResult = withContext(ioDispatcher) {
        repository.countExpensesForCategory(categoryId).fold(
            onSuccess = { count ->
                if (count > 0) Result.HasExpenses(categoryId, count) else Result.NoExpenses(categoryId)
            },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    suspend operator fun invoke(
        categoryId: Long,
        action: ExpenseDeletionAction,
    ): DomainResult = withContext(ioDispatcher) {
        repository.deleteCategoryWithExpenses(categoryId, action).fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    suspend fun deleteEmpty(categoryId: Long): DomainResult = withContext(ioDispatcher) {
        repository.deleteCategory(categoryId).fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    sealed interface Result : DomainResult {
        data class HasExpenses(val categoryId: Long, val count: Int) : Result
        data class NoExpenses(val categoryId: Long) : Result
        data object Success : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
