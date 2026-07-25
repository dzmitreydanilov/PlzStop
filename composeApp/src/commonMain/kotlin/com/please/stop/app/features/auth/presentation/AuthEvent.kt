package com.please.stop.app.features.auth.presentation

import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.google.GoogleSignInCredential

sealed interface AuthEvent {
    data class GoogleSignInCompleted(val credential: GoogleSignInCredential) : AuthEvent
    data object GoogleSignInCancelled : AuthEvent
    data class AppleSignInCompleted(val appleUser: AppleUser) : AuthEvent
    data object AppleSignInCancelled : AuthEvent
    data object DismissError : AuthEvent
}
