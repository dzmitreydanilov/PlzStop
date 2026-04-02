package com.please.stop.app.features.onboarding.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.repository.CategoryRepository
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import com.please.stop.app.features.onboarding.presentation.CategoryUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class LoadOnboardingDataUseCase(
    private val currencyRepository: CurrencyRepository,
    private val categoryRepository: CategoryRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): DomainResult = withContext(ioDispatcher) {
        val currenciesDeferred = async { currencyRepository.getAllCurrencies() }
        val categoriesDeferred = async { categoryRepository.getDefaultCategories() }

        val currenciesResult = currenciesDeferred.await()
        val categoriesResult = categoriesDeferred.await()

        currenciesResult.fold(
            onSuccess = { currencies ->
                categoriesResult.fold(
                    onSuccess = { categories ->
                        Result.Success(
                            currencies = currencies.toImmutableList(),
                            categories = categories.toImmutableList(),
                        )
                    },
                    onFailure = { Result.Failure(it.toErrorType()) },
                )
            },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    sealed interface Result : DomainResult {
        data class Success(
            val currencies: ImmutableList<Currency>,
            val categories: ImmutableList<CategoryUiModel>,
        ) : Result

        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
