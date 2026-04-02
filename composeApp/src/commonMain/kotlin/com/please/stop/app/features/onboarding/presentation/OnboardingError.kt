package com.please.stop.app.features.onboarding.presentation

import com.please.stop.app.core.models.domain.ErrorType
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingError(
    val errorType: ErrorType,
    val context: ErrorContext,
) {
    @Serializable
    enum class ErrorContext {
        LOAD_CURRENCIES,
        SAVE_ONBOARDING,
    }
}
