package com.please.stop.app.features.auth.presentation

import com.please.stop.app.core.models.presentation.Navigation

internal sealed interface AuthNavigation : Navigation {
    data object NavigateToHome : AuthNavigation
}
