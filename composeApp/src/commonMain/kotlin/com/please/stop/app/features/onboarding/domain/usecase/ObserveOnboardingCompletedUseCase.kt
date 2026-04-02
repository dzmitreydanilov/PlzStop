package com.please.stop.app.features.onboarding.domain.usecase

import com.please.stop.app.features.onboarding.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveOnboardingCompletedUseCase(private val repository: OnboardingRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeIsOnboardingCompleted()
}
