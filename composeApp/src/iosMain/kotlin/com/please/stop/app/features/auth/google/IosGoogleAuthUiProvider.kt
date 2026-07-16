package com.please.stop.app.features.auth.google

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class IosGoogleAuthUiProvider(
    private val bridge: IosSocialAuthBridge,
) : GoogleAuthUiProvider {

    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
    ): GoogleSignInCredential? = suspendCancellableCoroutine { continuation ->
        bridge.signInWithGoogle(
            onSuccess = { idToken ->
                continuation.resume(GoogleSignInCredential(idToken = idToken))
            },
            onError = {
                continuation.resume(null)
            },
        )
    }

    override suspend fun authorizeSheets(forceConsent: Boolean): GoogleSheetsAuthorizationCode? =
        suspendCancellableCoroutine { continuation ->
            bridge.authorizeGoogleSheets(
                scopes = GoogleAuthUiProvider.GOOGLE_SHEETS_SCOPES,
                forceConsent = forceConsent,
                onSuccess = { code ->
                    continuation.resume(GoogleSheetsAuthorizationCode(value = code))
                },
                onError = {
                    continuation.resume(null)
                },
            )
        }
}
