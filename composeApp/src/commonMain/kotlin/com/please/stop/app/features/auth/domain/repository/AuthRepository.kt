package com.please.stop.app.features.auth.domain.repository

import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.google.GoogleUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithGoogle(googleUser: GoogleUser): kotlin.Result<Unit>
    suspend fun signInWithApple(appleUser: AppleUser): kotlin.Result<Unit>
    suspend fun deleteAccount(): DeleteAccountResult
    suspend fun reauthenticateWithGoogle(idToken: String): kotlin.Result<Unit>
    suspend fun reauthenticateWithApple(identityToken: String, nonce: String): kotlin.Result<Unit>
    fun observeIsAuthenticated(): Flow<Boolean>
}
