package com.please.stop.app.features.home.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.home.domain.repository.HomeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class AddCategoryUseCase(
    private val repository: HomeRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(name: String, iconKey: String): DomainResult =
        withContext(ioDispatcher) {
            repository.addCategory(name, iconKey).fold(
                onSuccess = { Result.Success },
                onFailure = { Result.Failure(it.toErrorType()) },
            )
        }

    sealed interface Result : DomainResult {
        data object Success : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
