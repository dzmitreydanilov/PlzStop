package com.please.stop.app.features.auth.presentation

import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.google.GoogleSignInCredential

sealed interface UserEvent {
    data class GoogleSignInCompleted(val credential: GoogleSignInCredential) : UserEvent
    data class AppleSignInCompleted(val user: AppleUser) : UserEvent
    data object DeleteAccount : UserEvent
    data class DeleteAccountWithGoogle(val credential: GoogleSignInCredential) : UserEvent
    data class DeleteAccountWithApple(val user: AppleUser) : UserEvent
    data object DismissDeleteReauthentication : UserEvent
    data object SignInCancelled : UserEvent
    data object Logout : UserEvent
    data object DismissError : UserEvent
}
