package com.please.stop.app.features.auth.google

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class IosGoogleAuthProvider(
    private val bridge: IosSocialAuthBridge,
) : GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider = IosGoogleAuthUiProvider(bridge)

    override suspend fun signOut() {
        suspendCancellableCoroutine { continuation ->
            bridge.signOut { continuation.resume(Unit) }
        }
    }
}
