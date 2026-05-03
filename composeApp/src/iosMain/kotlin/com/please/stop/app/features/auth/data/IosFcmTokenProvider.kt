package com.please.stop.app.features.auth.data

import com.please.stop.app.core.IFcmTokenProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class IosFcmTokenProvider(
    private val bridge: IosFcmTokenBridge,
) : IFcmTokenProvider {

    override suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        bridge.getToken(
            onSuccess = { token -> continuation.resume(token) },
            onError = { continuation.resume(null) },
        )
    }
}
