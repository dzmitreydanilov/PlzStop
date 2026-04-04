package com.please.stop.app.features.onboarding.presentation

import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.features.onboarding.domain.model.Currency

sealed interface OnboardingResult : Result {
    data class DisplayNameUpdated(val name: String) : OnboardingResult
    data class CurrencyChosen(val currency: Currency) : OnboardingResult
    data class BudgetInputUpdated(val input: String) : OnboardingResult
    data object StepAdvanced : OnboardingResult
    data object StepSkipped : OnboardingResult
    data object StepBacked : OnboardingResult
    data object CurrencySheetOpened : OnboardingResult
    data object CurrencySheetClosed : OnboardingResult
    data object CurrencyDetectionStarted : OnboardingResult
    data object Saving : OnboardingResult
    data object ErrorCleared : OnboardingResult
}
