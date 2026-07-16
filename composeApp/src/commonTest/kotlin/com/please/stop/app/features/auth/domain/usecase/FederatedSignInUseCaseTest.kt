package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.domain.repository.AuthRepository
import com.please.stop.app.features.auth.google.GoogleSignInCredential
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FederatedSignInUseCaseTest {

    @Test
    fun googleAndAppleIdentityCredentialsEstablishTheSameSessionResult() = runTest {
        val repository = RecordingAuthRepository()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val googleResults = SignInWithGoogleUseCase(repository, dispatcher)(
            GoogleSignInCredential("google-id-token")
        ).toList()
        val appleResults = SignInWithAppleUseCase(repository, dispatcher)(
            AppleUser(
                identityToken = "apple-identity-token",
                nonce = "fresh-raw-nonce",
                email = null,
            )
        ).toList()

        assertEquals(listOf(SignInResult.Loading, SignInResult.Success), googleResults)
        assertEquals(listOf(SignInResult.Loading, SignInResult.Success), appleResults)
        assertEquals("google-id-token", repository.googleCredential?.idToken)
        assertEquals("fresh-raw-nonce", repository.appleCredential?.nonce)
    }

    @Test
    fun restoredFirebaseSessionDoesNotAcquireProviderCredential() = runTest {
        val repository = RecordingAuthRepository()

        assertTrue(ObserveAuthStateUseCase(repository)().first())
        assertNull(repository.googleCredential)
        assertNull(repository.appleCredential)
    }
}

private class RecordingAuthRepository : AuthRepository {
    var googleCredential: GoogleSignInCredential? = null
    var appleCredential: AppleUser? = null

    override suspend fun signInWithGoogle(credential: GoogleSignInCredential): Result<Unit> {
        googleCredential = credential
        return Result.success(Unit)
    }

    override suspend fun signInWithApple(appleUser: AppleUser): Result<Unit> {
        appleCredential = appleUser
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(): DeleteAccountResult = DeleteAccountResult.Success

    override suspend fun reauthenticateWithGoogle(credential: GoogleSignInCredential): Result<Unit> =
        Result.success(Unit)

    override suspend fun reauthenticateWithApple(credential: AppleUser): Result<Unit> =
        Result.success(Unit)

    override fun currentSignInProvider() = null

    override fun observeIsAuthenticated(): Flow<Boolean> = flowOf(true)
}
