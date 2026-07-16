package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.models.data.GoogleAccountLink
import com.please.stop.app.features.auth.domain.repository.GoogleAccountRepository
import com.please.stop.app.features.auth.google.GoogleSheetsAuthorizationCode
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableErrorReason
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableException
import com.please.stop.app.features.export.domain.usecase.CheckGoogleAccountLinkageUseCase
import com.please.stop.app.features.export.domain.usecase.HasGoogleAccountLinkageResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleAccountFlowTest {

    @Test
    fun successfulLinkConsumesOneTimeCodeAndWritesOnlyNonSecretMarker() = runTest {
        val repository = FakeGoogleAccountRepository()
        val storage = FakeGoogleAccountStorage()
        val useCase = ConnectGoogleAccountUseCase(
            googleAccountStorage = storage,
            googleAccountRepository = repository,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        assertEquals(
            ConnectGoogleAccount.Success,
            useCase(GoogleSheetsAuthorizationCode("one-time-code")),
        )
        assertEquals("one-time-code", repository.linkedCode?.value)
        assertEquals(GoogleAccountLink(email = "", isConnected = true), storage.link)
    }

    @Test
    fun missingRefreshTokenReasonStartsReconnectFlow() = runTest {
        val repository = FakeGoogleAccountRepository(
            linkResult = Result.failure(
                FirebaseCallableException(
                    code = "FAILED_PRECONDITION",
                    reason = FirebaseCallableErrorReason.GoogleRefreshTokenMissing,
                    message = "Reconnect required",
                )
            )
        )
        val storage = FakeGoogleAccountStorage()
        val useCase = ConnectGoogleAccountUseCase(
            googleAccountStorage = storage,
            googleAccountRepository = repository,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        assertEquals(
            ConnectGoogleAccount.ReconnectRequired,
            useCase(GoogleSheetsAuthorizationCode("discarded-code")),
        )
        assertNull(storage.link)
    }

    @Test
    fun linkLookupFailureIsNotMappedToUnlinked() = runTest {
        val repository = FakeGoogleAccountRepository(
            linkedResult = Result.failure(IllegalStateException("network unavailable")),
        )
        val useCase = CheckGoogleAccountLinkageUseCase(
            googleAccountRepository = repository,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        assertIs<HasGoogleAccountLinkageResult.Failure>(useCase().single())
    }

    @Test
    fun firebaseSignInRequiredStartsAuthenticationFlow() = runTest {
        val repository = FakeGoogleAccountRepository(
            linkedResult = Result.failure(
                FirebaseCallableException(
                    code = "UNAUTHENTICATED",
                    reason = FirebaseCallableErrorReason.FirebaseSignInRequired,
                    message = "Authentication required",
                )
            ),
        )
        val useCase = CheckGoogleAccountLinkageUseCase(
            googleAccountRepository = repository,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        assertEquals(
            HasGoogleAccountLinkageResult.AuthenticationRequired,
            useCase().single(),
        )
    }

    @Test
    fun appCheckPermissionDeniedDoesNotStartAuthenticationFlow() = runTest {
        val repository = FakeGoogleAccountRepository(
            linkedResult = Result.failure(
                FirebaseCallableException(
                    code = "PERMISSION_DENIED",
                    reason = null,
                    message = "App attestation failed",
                )
            ),
        )
        val useCase = CheckGoogleAccountLinkageUseCase(
            googleAccountRepository = repository,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        assertIs<HasGoogleAccountLinkageResult.Failure>(useCase().single())
    }
}

private class FakeGoogleAccountRepository(
    private val linkResult: Result<Unit> = Result.success(Unit),
    private val linkedResult: Result<Boolean> = Result.success(false),
    private val unlinkResult: Result<Unit> = Result.success(Unit),
) : GoogleAccountRepository {
    var linkedCode: GoogleSheetsAuthorizationCode? = null

    override suspend fun link(authorizationCode: GoogleSheetsAuthorizationCode): Result<Unit> {
        linkedCode = authorizationCode
        return linkResult
    }

    override suspend fun isLinked(): Result<Boolean> = linkedResult

    override suspend fun unlink(): Result<Unit> = unlinkResult
}

private class FakeGoogleAccountStorage : IGoogleAccountStorage {
    var link: GoogleAccountLink? = null

    override suspend fun write(link: GoogleAccountLink) {
        this.link = link
    }

    override suspend fun read(): GoogleAccountLink? = link

    override suspend fun delete() {
        link = null
    }
}
