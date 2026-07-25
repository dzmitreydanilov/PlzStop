package com.please.stop.app.features.auth.domain.usecase

import androidx.compose.runtime.Composable
import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.models.data.GoogleAccountLink
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import com.please.stop.app.features.auth.domain.repository.GoogleAccountRepository
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import com.please.stop.app.features.auth.google.GoogleAuthUiProvider
import com.please.stop.app.features.auth.google.GoogleSheetsAuthorizationCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class LogoutUseCaseTest {

    @Test
    fun unlinkFailureDoesNotPreventLocalSessionCleanup() = runTest {
        val calls = mutableListOf<String>()
        val useCase = LogoutUseCase(
            googleAccountStorage = RecordingGoogleAccountStorage(calls),
            googleAccountRepository = FailingUnlinkRepository(calls),
            firebaseAuthProvider = RecordingFirebaseAuthProvider(calls),
            googleAuthProvider = RecordingGoogleAuthProvider(calls),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        val results = useCase().toList()

        assertEquals(listOf(LogoutResult.Loading), results.dropLast(1))
        assertIs<LogoutResult.Failure>(results.last())
        assertEquals(listOf("unlink", "storage", "firebase", "google"), calls)
    }
}

private class FailingUnlinkRepository(
    private val calls: MutableList<String>,
) : GoogleAccountRepository {
    override suspend fun link(authorizationCode: GoogleSheetsAuthorizationCode): Result<Unit> =
        Result.success(Unit)

    override suspend fun isLinked(): Result<Boolean> = Result.success(true)

    override suspend fun unlink(): Result<Unit> {
        calls += "unlink"
        return Result.failure(IllegalStateException("offline"))
    }
}

private class RecordingGoogleAccountStorage(
    private val calls: MutableList<String>,
) : IGoogleAccountStorage {
    override suspend fun write(link: GoogleAccountLink) = Unit

    override suspend fun read(): GoogleAccountLink? = null

    override suspend fun delete() {
        calls += "storage"
    }
}

private class RecordingFirebaseAuthProvider(
    private val calls: MutableList<String>,
) : FirebaseAuthProvider {
    override suspend fun signInWithGoogleCredential(idToken: String): Result<String> =
        Result.success("uid")

    override suspend fun signInWithAppleCredential(identityToken: String, nonce: String): Result<String> =
        Result.success("uid")

    override suspend fun deleteAccount(): DeleteAccountResult = DeleteAccountResult.Success

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = Result.success(Unit)

    override suspend fun reauthenticateWithApple(identityToken: String, nonce: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun signOut() {
        calls += "firebase"
    }

    override fun currentSignInProvider(): FirebaseSignInProvider = FirebaseSignInProvider.GOOGLE

    override fun observeIsAuthenticated(): Flow<Boolean> = flowOf(true)
}

private class RecordingGoogleAuthProvider(
    private val calls: MutableList<String>,
) : GoogleAuthProvider {
    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider = error("Not used by logout")

    override suspend fun signOut() {
        calls += "google"
    }
}
