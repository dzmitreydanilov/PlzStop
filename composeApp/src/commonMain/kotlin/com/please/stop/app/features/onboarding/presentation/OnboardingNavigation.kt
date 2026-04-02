package com.please.stop.app.features.onboarding.presentation

import com.please.stop.app.core.models.presentation.Navigation

internal sealed interface OnboardingNavigation : Navigation {
    data object NavigateToHome : OnboardingNavigation
}
