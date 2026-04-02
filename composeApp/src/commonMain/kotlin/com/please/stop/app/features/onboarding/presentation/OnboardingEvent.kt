package com.please.stop.app.features.onboarding.presentation

import com.please.stop.app.features.onboarding.domain.model.Currency

sealed interface OnboardingEvent {
    data class DisplayNameChanged(val name: String) : OnboardingEvent
    data class CurrencySearchQueryChanged(val query: String) : OnboardingEvent
    data class CurrencySelected(val currency: Currency) : OnboardingEvent
    data class BudgetInputChanged(val input: String) : OnboardingEvent
    data class CategoryToggled(val categoryId: Long) : OnboardingEvent
    data class CustomCategoryAdded(val name: String, val iconKey: String) : OnboardingEvent
    data object NextTapped : OnboardingEvent
    data object BackTapped : OnboardingEvent
    data object ErrorDismissed : OnboardingEvent
    data object RetryLoadCurrencies : OnboardingEvent
}
