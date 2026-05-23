package com.please.stop.app.features.auth.data

import kotlinx.coroutines.flow.Flow

interface FirebaseAuthProvider {
    suspend fun signInWithGoogleCredential(idToken: String): kotlin.Result<String>
    suspend fun signInWithAppleCredential(identityToken: String, nonce: String): kotlin.Result<String>
    suspend fun deleteAccount(): DeleteAccountResult
    suspend fun reauthenticateWithGoogle(idToken: String): kotlin.Result<Unit>
    suspend fun reauthenticateWithApple(identityToken: String, nonce: String): kotlin.Result<Unit>
    suspend fun signOut()
    fun observeIsAuthenticated(): Flow<Boolean>
}

sealed interface DeleteAccountResult {
    data object Success : DeleteAccountResult
    data object NeedsReauthentication : DeleteAccountResult
    data class Failure(val error: Throwable) : DeleteAccountResult
}
