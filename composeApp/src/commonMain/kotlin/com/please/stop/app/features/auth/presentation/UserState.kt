package com.please.stop.app.features.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import com.please.stop.app.uicomponents.error.ScreenOverlay

@Stable
sealed interface UserState {
    val isAuthenticated: Boolean
    val signInProvider: FirebaseSignInProvider?
    val isAppleSignInSupported: Boolean
    val isLoading: Boolean
    val isDeleteReauthenticationRequired: Boolean

    data class Content(
        override val isAuthenticated: Boolean,
        override val signInProvider: FirebaseSignInProvider? = null,
        override val isAppleSignInSupported: Boolean,
        override val isLoading: Boolean = false,
        override val isDeleteReauthenticationRequired: Boolean = false,
    ) : UserState

    data class Error(
        val errorType: ErrorType,
        override val isAuthenticated: Boolean,
        override val signInProvider: FirebaseSignInProvider?,
        override val isAppleSignInSupported: Boolean,
        override val isLoading: Boolean = false,
        override val isDeleteReauthenticationRequired: Boolean = false,
    ) : UserState
}

internal val UserState.asOverlay: ScreenOverlay?
    @Composable get() = when (this) {
        is UserState.Error -> ScreenOverlay.Error(type = errorType)
        else -> null
    }

internal fun UserState.toError(errorType: ErrorType): UserState.Error = UserState.Error(
    errorType = errorType,
    isAuthenticated = isAuthenticated,
    signInProvider = signInProvider,
    isAppleSignInSupported = isAppleSignInSupported,
    isDeleteReauthenticationRequired = isDeleteReauthenticationRequired,
)
