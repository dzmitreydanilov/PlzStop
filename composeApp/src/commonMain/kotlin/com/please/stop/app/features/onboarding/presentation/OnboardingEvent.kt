package com.please.stop.app.features.onboarding.presentation

import com.please.stop.app.features.onboarding.domain.model.Currency

sealed interface OnboardingEvent {
    data class DisplayNameChanged(val name: String) : OnboardingEvent
    data class CurrencySelected(val currency: Currency) : OnboardingEvent
    data class BudgetInputChanged(val input: String) : OnboardingEvent
    data object NextTapped : OnboardingEvent
    data object SkipTapped : OnboardingEvent
    data object BackTapped : OnboardingEvent
    data object SelectCurrencyTapped : OnboardingEvent
    data object CurrencySheetDismissed : OnboardingEvent
    data object ErrorDismissed : OnboardingEvent
    data object RetryLoadCurrencies : OnboardingEvent
}
