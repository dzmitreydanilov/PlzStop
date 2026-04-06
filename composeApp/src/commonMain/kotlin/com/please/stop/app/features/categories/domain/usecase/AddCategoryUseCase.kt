package com.please.stop.app.features.categories.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.categories.domain.repository.CategoriesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class AddCategoryUseCase(
    private val repository: CategoriesRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(name: String, iconKey: String, comment: String?): DomainResult =
        withContext(ioDispatcher) {
            repository.addCategory(name, iconKey, comment).fold(
                onSuccess = { Result.Success },
                onFailure = { Result.Failure(it.toErrorType()) },
            )
        }

    sealed interface Result : DomainResult {
        data object Success : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
