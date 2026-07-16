package com.please.stop.app.features.auth.data

import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class IosFirebaseAuthProvider(
    private val bridge: IosFirebaseAuthBridge,
) : FirebaseAuthProvider {

    override suspend fun signInWithGoogleCredential(idToken: String): Result<String> =
        suspendCancellableCoroutine { continuation ->
            bridge.signInWithGoogle(
                idToken = idToken,
                onSuccess = { uid -> continuation.resume(Result.success(uid)) },
                onError = { msg -> continuation.resume(Result.failure(Exception(msg))) },
            )
        }

    override suspend fun signInWithAppleCredential(
        identityToken: String,
        nonce: String,
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        bridge.signInWithApple(
            identityToken = identityToken,
            nonce = nonce,
            onSuccess = { uid -> continuation.resume(Result.success(uid)) },
            onError = { msg -> continuation.resume(Result.failure(Exception(msg))) },
        )
    }

    override suspend fun deleteAccount(): DeleteAccountResult =
        suspendCancellableCoroutine { continuation ->
            bridge.deleteAccount(
                onSuccess = { continuation.resume(DeleteAccountResult.Success) },
                onNeedsReauthentication = { continuation.resume(DeleteAccountResult.NeedsReauthentication) },
                onError = { msg -> continuation.resume(DeleteAccountResult.Failure(Exception(msg))) },
            )
        }

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            bridge.reauthenticateWithGoogle(
                idToken = idToken,
                onSuccess = { continuation.resume(Result.success(Unit)) },
                onError = { msg -> continuation.resume(Result.failure(Exception(msg))) },
            )
        }

    override suspend fun reauthenticateWithApple(
        identityToken: String,
        nonce: String,
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        bridge.reauthenticateWithApple(
            identityToken = identityToken,
            nonce = nonce,
            onSuccess = { continuation.resume(Result.success(Unit)) },
            onError = { msg -> continuation.resume(Result.failure(Exception(msg))) },
        )
    }

    override suspend fun signOut() {
        suspendCancellableCoroutine { continuation ->
            bridge.signOut { continuation.resume(Unit) }
        }
    }

    override fun currentSignInProvider(): FirebaseSignInProvider? =
        when (bridge.currentSignInProviderId()) {
            GOOGLE_PROVIDER_ID -> FirebaseSignInProvider.GOOGLE
            APPLE_PROVIDER_ID -> FirebaseSignInProvider.APPLE
            else -> null
        }

    override fun observeIsAuthenticated(): Flow<Boolean> = callbackFlow {
        bridge.observeIsAuthenticated { isAuthenticated ->
            trySend(isAuthenticated)
        }
        awaitClose { bridge.removeAuthStateListener() }
    }

    private companion object {
        const val GOOGLE_PROVIDER_ID = "google.com"
        const val APPLE_PROVIDER_ID = "apple.com"
    }
}
