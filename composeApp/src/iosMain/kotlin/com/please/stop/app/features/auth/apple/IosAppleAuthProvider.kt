package com.please.stop.app.features.auth.apple

import com.please.stop.app.features.auth.google.IosSocialAuthBridge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class IosAppleAuthProvider(
    private val bridge: IosSocialAuthBridge,
) : AppleAuthProvider {

    override suspend fun signIn(): AppleUser? = suspendCancellableCoroutine { continuation ->
        bridge.signInWithApple(
            onSuccess = { identityToken, nonce, email ->
                continuation.resume(AppleUser(identityToken = identityToken, nonce = nonce, email = email))
            },
            onError = {
                continuation.resume(null)
            },
        )
    }

    override suspend fun signOut() {
        // Apple doesn't provide a sign-out SDK method
    }
}
