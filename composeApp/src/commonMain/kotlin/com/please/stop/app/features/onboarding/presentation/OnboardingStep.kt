package com.please.stop.app.features.onboarding.presentation

import kotlinx.serialization.Serializable

@Serializable
enum class OnboardingStep {
    WELCOME, CURRENCY, BUDGET;

    fun next(): OnboardingStep = when (this) {
        WELCOME -> CURRENCY
        CURRENCY -> BUDGET
        BUDGET -> BUDGET
    }

    /**
     * Returns the previous step, or null if back navigation is not allowed.
     * Welcome is shown only once — no returning to it.
     */
    fun previous(): OnboardingStep? = when (this) {
        WELCOME -> null
        CURRENCY -> null
        BUDGET -> CURRENCY
    }
}
