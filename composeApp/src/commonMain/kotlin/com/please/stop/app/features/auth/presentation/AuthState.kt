package com.please.stop.app.features.auth.presentation

import com.please.stop.app.core.models.domain.ErrorType

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Error(val errorType: ErrorType) : AuthState
}
