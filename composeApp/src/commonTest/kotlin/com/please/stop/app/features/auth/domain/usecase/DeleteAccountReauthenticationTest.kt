package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.domain.model.FirebaseReauthenticationCredential
import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteAccountReauthenticationTest {

    @Test
    fun googleCredentialUsesGoogleReauthentication() = runTest {
        val provider = RecordingDeleteAuthProvider()

        provider.reauthenticate(
            FirebaseReauthenticationCredential.Google(idToken = "google-id-token"),
        )

        assertEquals("google-id-token", provider.googleIdToken)
        assertEquals(null, provider.appleCredential)
    }

    @Test
    fun appleCredentialUsesAppleReauthentication() = runTest {
        val provider = RecordingDeleteAuthProvider()

        provider.reauthenticate(
            FirebaseReauthenticationCredential.Apple(
                identityToken = "apple-identity-token",
                nonce = "raw-nonce",
            ),
        )

        assertEquals(null, provider.googleIdToken)
        assertEquals("apple-identity-token" to "raw-nonce", provider.appleCredential)
    }
}

private class RecordingDeleteAuthProvider : FirebaseAuthProvider {
    var googleIdToken: String? = null
    var appleCredential: Pair<String, String>? = null

    override suspend fun signInWithGoogleCredential(idToken: String): Result<String> =
        Result.success("uid")

    override suspend fun signInWithAppleCredential(
        identityToken: String,
        nonce: String,
    ): Result<String> = Result.success("uid")

    override suspend fun deleteAccount(): DeleteAccountResult = DeleteAccountResult.Success

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> {
        googleIdToken = idToken
        return Result.success(Unit)
    }

    override suspend fun reauthenticateWithApple(
        identityToken: String,
        nonce: String,
    ): Result<Unit> {
        appleCredential = identityToken to nonce
        return Result.success(Unit)
    }

    override suspend fun signOut() = Unit

    override fun currentSignInProvider(): FirebaseSignInProvider = FirebaseSignInProvider.GOOGLE

    override fun observeIsAuthenticated(): Flow<Boolean> = flowOf(true)
}
