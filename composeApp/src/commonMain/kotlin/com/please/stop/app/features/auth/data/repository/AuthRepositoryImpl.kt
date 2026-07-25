package com.please.stop.app.features.auth.data.repository

import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import com.please.stop.app.features.auth.domain.repository.AuthRepository
import com.please.stop.app.features.auth.google.GoogleSignInCredential
import kotlinx.coroutines.flow.Flow

internal class AuthRepositoryImpl(
    private val firebaseAuthProvider: FirebaseAuthProvider,
) : AuthRepository {

    override suspend fun signInWithGoogle(credential: GoogleSignInCredential): Result<Unit> =
        firebaseAuthProvider.signInWithGoogleCredential(credential.idToken).map { }

    override suspend fun signInWithApple(appleUser: AppleUser): Result<Unit> =
        firebaseAuthProvider.signInWithAppleCredential(appleUser.identityToken, appleUser.nonce).map { }

    override suspend fun deleteAccount(): DeleteAccountResult =
        firebaseAuthProvider.deleteAccount()

    override suspend fun reauthenticateWithGoogle(credential: GoogleSignInCredential): Result<Unit> =
        firebaseAuthProvider.reauthenticateWithGoogle(credential.idToken)

    override suspend fun reauthenticateWithApple(credential: AppleUser): Result<Unit> =
        firebaseAuthProvider.reauthenticateWithApple(
            identityToken = credential.identityToken,
            nonce = credential.nonce,
        )

    override fun currentSignInProvider(): FirebaseSignInProvider? =
        firebaseAuthProvider.currentSignInProvider()

    override fun observeIsAuthenticated(): Flow<Boolean> =
        firebaseAuthProvider.observeIsAuthenticated()
}
