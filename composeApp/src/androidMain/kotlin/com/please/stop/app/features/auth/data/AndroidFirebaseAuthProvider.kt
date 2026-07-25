package com.please.stop.app.features.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.OAuthProvider
import com.please.stop.app.core.runSuspendCatching
import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.GoogleAuthProvider as FirebaseGoogleAuthProvider

internal class AndroidFirebaseAuthProvider : FirebaseAuthProvider {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private companion object {
        const val APPLE_PROVIDER_ID = "apple.com"
    }

    override suspend fun signInWithGoogleCredential(idToken: String): Result<String> =
        runSuspendCatching {
            val credential = FirebaseGoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.uid ?: error("Firebase user is null after sign-in")
        }

    override suspend fun signInWithAppleCredential(
        identityToken: String,
        nonce: String,
    ): Result<String> = runSuspendCatching {
        val credential = OAuthProvider.newCredentialBuilder(APPLE_PROVIDER_ID)
            .setIdTokenWithRawNonce(identityToken, nonce)
            .build()
        val result = auth.signInWithCredential(credential).await()
        result.user?.uid ?: error("Firebase user is null after sign-in")
    }

    override suspend fun deleteAccount(): DeleteAccountResult {
        val user = auth.currentUser ?: return DeleteAccountResult.Failure(
            IllegalStateException("No current user")
        )
        return try {
            user.delete().await()
            DeleteAccountResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            DeleteAccountResult.NeedsReauthentication
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            DeleteAccountResult.Failure(e)
        }
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = runSuspendCatching {
        val credential = FirebaseGoogleAuthProvider.getCredential(idToken, null)
        auth.currentUser?.reauthenticate(credential)?.await()
            ?: error("No current user for reauthentication")
    }

    override suspend fun reauthenticateWithApple(
        identityToken: String,
        nonce: String,
    ): Result<Unit> = runSuspendCatching {
        val credential = OAuthProvider.newCredentialBuilder(APPLE_PROVIDER_ID)
            .setIdTokenWithRawNonce(identityToken, nonce)
            .build()
        auth.currentUser?.reauthenticate(credential)?.await()
            ?: error("No current user for reauthentication")
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun currentSignInProvider(): FirebaseSignInProvider? =
        auth.currentUser?.providerData?.firstNotNullOfOrNull { userInfo ->
            when (userInfo.providerId) {
                FirebaseGoogleAuthProvider.PROVIDER_ID -> FirebaseSignInProvider.GOOGLE
                APPLE_PROVIDER_ID -> FirebaseSignInProvider.APPLE
                else -> null
            }
        }

    override fun observeIsAuthenticated(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
