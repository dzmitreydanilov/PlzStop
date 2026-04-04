package com.please.stop.app.features.onboarding.presentation

import kotlinx.serialization.Serializable

@Serializable
enum class OnboardingStep {
    WELCOME, CURRENCY, NAME, BUDGET;

    fun next(): OnboardingStep = when (this) {
        WELCOME -> CURRENCY
        CURRENCY -> NAME
        NAME -> BUDGET
        BUDGET -> BUDGET
    }

    /**
     * Returns the previous step, or null if back navigation is not allowed.
     * WELCOME is shown only once — no returning to it from CURRENCY.
     */
    fun previous(): OnboardingStep? = when (this) {
        WELCOME -> null
        CURRENCY -> null
        NAME -> CURRENCY
        BUDGET -> NAME
    }

    val isSkippable: Boolean
        get() = this == NAME || this == BUDGET
}
