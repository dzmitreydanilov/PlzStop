package com.please.stop.app.features.onboarding.domain.model

data class OnboardingData(
    val displayName: String?,
    val currency: Currency,
    val monthlyBudgetMinorUnits: Long,
)
