package com.please.stop.app.features.onboarding.presentation

import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import kotlinx.serialization.Serializable

@Serializable
sealed interface OnboardingState {

    @Serializable
    data object Loading : OnboardingState

    @Serializable
    data class Content(
        val currentStep: OnboardingStep = OnboardingStep.WELCOME,
        val displayName: String = "",
        val selectedCurrency: Currency? = null,
        val currencySymbol: String = "",
        val deviceCurrencyCode: String? = null,
        val monthlyBudgetInput: String = "",
        val decimalPlaces: Int = DEFAULT_CURRENCY_DECIMAL_PLACES,
        val showCurrencySheet: Boolean = false,
        val isDetectingCurrency: Boolean = false,
        val hasAttemptedCurrencyDetection: Boolean = false,
        val isNextEnabled: Boolean = true,
        val isSaving: Boolean = false,
        val error: OnboardingError? = null,
    ) : OnboardingState

    @Serializable
    data class Error(val error: OnboardingError) : OnboardingState

    companion object {
        const val MAX_BUDGET = 9_999_999.0
        const val MAX_DISPLAY_NAME_LENGTH = 24
    }
}
