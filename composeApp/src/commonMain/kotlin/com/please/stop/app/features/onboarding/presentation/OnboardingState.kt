package com.please.stop.app.features.onboarding.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.features.onboarding.domain.model.Currency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Stable
@Serializable
sealed interface OnboardingState {

    @Serializable
    data object Loading : OnboardingState

    @Serializable
    data class Content(
        val currentStep: OnboardingStep = OnboardingStep.WELCOME,
        val displayName: String = "",
        @Serializable(with = ImmutableListSerializer::class)
        val currencies: ImmutableList<Currency> = persistentListOf(),
        val currencySearchQuery: String = "",
        @Serializable(with = ImmutableListSerializer::class)
        val popularCurrencies: ImmutableList<Currency> = persistentListOf(),
        @Serializable(with = ImmutableListSerializer::class)
        val otherCurrencies: ImmutableList<Currency> = persistentListOf(),
        val selectedCurrency: Currency? = null,
        val currencySymbol: String = "",
        val monthlyBudgetInput: String = "",
        val decimalPlaces: Int = 2,
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
