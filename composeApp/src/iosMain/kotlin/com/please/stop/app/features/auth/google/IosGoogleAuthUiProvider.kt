package com.please.stop.app.features.auth.google

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class IosGoogleAuthUiProvider(
    private val bridge: IosSocialAuthBridge,
) : GoogleAuthUiProvider {

    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>,
    ): GoogleUser? = suspendCancellableCoroutine { continuation ->
        bridge.signInWithGoogle(
            scopes = scopes,
            onSuccess = { idToken, accessToken ->
                continuation.resume(GoogleUser(idToken = idToken, accessToken = accessToken))
            },
            onError = {
                continuation.resume(null)
            },
        )
    }
}
