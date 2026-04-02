package com.please.stop.app.features.onboarding.domain.repository

import com.please.stop.app.features.onboarding.domain.model.OnboardingData
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    fun observeIsOnboardingCompleted(): Flow<Boolean>
    suspend fun completeOnboarding(data: OnboardingData): Result<Unit>
}
