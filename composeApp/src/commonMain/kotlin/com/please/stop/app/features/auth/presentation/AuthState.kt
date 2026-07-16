package com.please.stop.app.features.auth.presentation

import androidx.compose.runtime.Composable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.uicomponents.error.ScreenOverlay

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Error(val errorType: ErrorType) : AuthState
}

internal val AuthState.asOverlay: ScreenOverlay?
    @Composable get() = when (this) {
        is AuthState.Error -> ScreenOverlay.Error(type = errorType)
        else -> null
    }
