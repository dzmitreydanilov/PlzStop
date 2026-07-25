package com.please.stop.app.features.auth.data

import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import kotlinx.coroutines.flow.Flow

interface FirebaseAuthProvider {
    suspend fun signInWithGoogleCredential(idToken: String): Result<String>
    suspend fun signInWithAppleCredential(identityToken: String, nonce: String): Result<String>
    suspend fun deleteAccount(): DeleteAccountResult
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit>
    suspend fun reauthenticateWithApple(identityToken: String, nonce: String): Result<Unit>
    suspend fun signOut()
    fun currentSignInProvider(): FirebaseSignInProvider?
    fun observeIsAuthenticated(): Flow<Boolean>
}

sealed interface DeleteAccountResult {
    data object Success : DeleteAccountResult
    data object NeedsReauthentication : DeleteAccountResult
    data class Failure(val error: Throwable) : DeleteAccountResult
}
