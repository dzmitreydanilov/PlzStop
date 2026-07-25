package com.please.stop.app.features.categories.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.categories.domain.model.CategorySummary
import com.please.stop.app.features.categories.domain.repository.CategoriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.please.stop.app.core.models.domain.Result as DomainResult

class ObserveCategoriesUseCase(
    private val repository: CategoriesRepository,
) {

    operator fun invoke(): Flow<DomainResult> =
        repository.observeCategorySummaries()
            .map<List<CategorySummary>, DomainResult> { Result.Success(it) }
            .catch { emit(Result.Failure(it.toErrorType())) }

    sealed interface Result : DomainResult {
        data class Success(val data: List<CategorySummary>) : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
