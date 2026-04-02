package com.please.stop.app.features.onboarding.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.onboarding.domain.model.OnboardingData
import com.please.stop.app.features.onboarding.domain.repository.OnboardingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class CompleteOnboardingUseCase(
    private val repository: OnboardingRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(data: OnboardingData): DomainResult = withContext(ioDispatcher) {
        repository.completeOnboarding(data).fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    sealed interface Result : DomainResult {
        data object Success : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
