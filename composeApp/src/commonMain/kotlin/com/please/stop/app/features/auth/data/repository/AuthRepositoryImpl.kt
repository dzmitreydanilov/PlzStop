package com.please.stop.app.features.auth.data.repository

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.models.data.GoogleAccountLink
import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.domain.repository.AuthRepository
import com.please.stop.app.features.auth.google.GoogleUser
import kotlinx.coroutines.flow.Flow

internal class AuthRepositoryImpl(
    private val firebaseAuthProvider: FirebaseAuthProvider,
    private val googleAccountStorage: IGoogleAccountStorage,
) : AuthRepository {

    override suspend fun signInWithGoogle(googleUser: GoogleUser): Result<Unit> =
        firebaseAuthProvider.signInWithGoogleCredential(googleUser.idToken).map { _ ->
            googleAccountStorage.write(GoogleAccountLink(email = "", isConnected = true))
        }

    override suspend fun signInWithApple(appleUser: AppleUser): Result<Unit> =
        firebaseAuthProvider.signInWithAppleCredential(appleUser.identityToken, appleUser.nonce).map { }

    override suspend fun deleteAccount(): DeleteAccountResult =
        firebaseAuthProvider.deleteAccount()

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> =
        firebaseAuthProvider.reauthenticateWithGoogle(idToken)

    override suspend fun reauthenticateWithApple(identityToken: String, nonce: String): Result<Unit> =
        firebaseAuthProvider.reauthenticateWithApple(identityToken, nonce)

    override fun observeIsAuthenticated(): Flow<Boolean> =
        firebaseAuthProvider.observeIsAuthenticated()
}
