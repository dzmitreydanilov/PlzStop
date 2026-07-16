package com.please.stop.app.features.auth.domain.repository

import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.google.GoogleSignInCredential
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithGoogle(credential: GoogleSignInCredential): Result<Unit>
    suspend fun signInWithApple(appleUser: AppleUser): Result<Unit>
    suspend fun deleteAccount(): DeleteAccountResult
    suspend fun reauthenticateWithGoogle(credential: GoogleSignInCredential): kotlin.Result<Unit>
    suspend fun reauthenticateWithApple(credential: AppleUser): kotlin.Result<Unit>
    fun observeIsAuthenticated(): Flow<Boolean>
}
