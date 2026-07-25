package com.please.stop.app.features.categories.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.categories.domain.repository.CategoriesRepository
import com.please.stop.app.features.onboarding.domain.model.Subcategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class LoadSubcategoriesUseCase(
    private val repository: CategoriesRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(categoryId: Long): DomainResult = withContext(ioDispatcher) {
        repository.getSubcategories(categoryId).fold(
            onSuccess = { Result.Success(categoryId, it) },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    sealed interface Result : DomainResult {
        data class Success(
            val categoryId: Long,
            val subcategories: ImmutableList<Subcategory>,
        ) : Result

        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
