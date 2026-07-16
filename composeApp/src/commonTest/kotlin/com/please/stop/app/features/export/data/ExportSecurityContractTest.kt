package com.please.stop.app.features.export.data

import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.entity.ExportHistoryEntity
import com.please.stop.app.core.db.entity.ExportStatus
import com.please.stop.app.core.db.entity.UserProfileEntity
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableErrorReason
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableException
import com.please.stop.app.features.export.data.repository.GoogleSheetExportRepository
import com.please.stop.app.features.export.domain.ExportWorkerScheduler
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExportSecurityContractTest {

    @Test
    fun persistedWorkInputContainsOnlyNonSecretConfiguration() {
        val input = ExportWorkRequest(
            exportId = 42,
            tabLayout = "single_tab",
            startDateMillis = 1_000,
            endDateMillis = 2_000,
            spreadsheetTitle = "January expenses",
            folderName = "PlzStop exports",
        ).toPersistedValues()

        assertEquals(
            setOf(
                "exportId",
                "tabLayout",
                "startDate",
                "endDate",
                "spreadsheetTitle",
                "folderName",
            ),
            input.keys,
        )
        assertNoOauthCredentialKeys(input.keys)
    }

    @Test
    fun repositorySchedulesTokenFreePlatformWorkWithApiLayoutValue() = runTest {
        val scheduler = RecordingExportWorkerScheduler()
        val repository = GoogleSheetExportRepository(
            exportHistoryDao = FakeExportHistoryDao(),
            exportWorkerScheduler = scheduler,
        )

        val result = repository.enqueExport(
            startDateMillis = 1_000,
            endDateMillis = 2_000,
            spreadSheetFormat = SpreadSheetFormat.SEPARATE_TABS,
            spreadsheetTitle = "January expenses",
            folderName = "PlzStop exports",
        ).single()

        assertTrue(result.isSuccess)
        assertEquals(
            ExportWorkRequest(
                exportId = 42,
                tabLayout = "separate_tabs",
                startDateMillis = 1_000,
                endDateMillis = 2_000,
                spreadsheetTitle = "January expenses",
                folderName = "PlzStop exports",
            ),
            scheduler.request,
        )
        assertNoOauthCredentialKeys(scheduler.request!!.toPersistedValues().keys)
    }

    @Test
    fun callablePayloadContainsNoOauthCredential() {
        val payload = ExportToSheetsPayloadBuilder().build(
            request = ExportWorkRequest(
                exportId = 42,
                tabLayout = "single_tab",
                startDateMillis = 1_000,
                endDateMillis = 2_000,
                spreadsheetTitle = "January expenses",
                folderName = "PlzStop exports",
            ),
            expenses = emptyList(),
            categories = emptyList(),
            subcategories = emptyList(),
            userProfile = UserProfileEntity(
                displayName = null,
                currencyCode = "EUR",
                currencySymbol = "EUR",
                decimalPlaces = 2,
                monthlyBudget = 0,
                onboardingCompleted = true,
            ),
        )

        assertEquals(
            setOf(
                "exportId",
                "dateRangeLabel",
                "tabLayout",
                "currencySymbol",
                "decimalPlaces",
                "compressed",
                "expenses",
                "title",
                "folderName",
            ),
            payload.keys,
        )
        assertNoOauthCredentialKeys(payload.keys)
    }

    @Test
    fun tokenEndpointOutageIsRetryableByStructuredReason() {
        val error = FirebaseCallableException(
            code = "UNAVAILABLE",
            reason = FirebaseCallableErrorReason.GoogleTokenEndpointUnavailable,
            message = "Export temporarily unavailable",
        )

        assertEquals(ExportFailureDecision.Retry, error.toExportFailureDecision())
    }

    @Test
    fun existingInProgressExportIsRetryableByStructuredReason() {
        val error = FirebaseCallableException(
            code = "ALREADY_EXISTS",
            reason = FirebaseCallableErrorReason.ExportInProgress,
            message = "Export is already running",
        )

        assertEquals(ExportFailureDecision.Retry, error.toExportFailureDecision())
    }

    @Test
    fun reconnectFailureIsTerminalWithoutMessageParsing() {
        val error = FirebaseCallableException(
            code = "FAILED_PRECONDITION",
            reason = FirebaseCallableErrorReason.GoogleReconnectRequired,
            message = "A message that can change",
        )

        val decision = assertIs<ExportFailureDecision.Terminal>(error.toExportFailureDecision())
        assertEquals("GOOGLE_RECONNECT_REQUIRED", decision.reason)
        assertTrue(decision.invalidatesGoogleLink)
    }

    @Test
    fun missingScopesInvalidatesCachedLink() {
        val error = FirebaseCallableException(
            code = "PERMISSION_DENIED",
            reason = FirebaseCallableErrorReason.GoogleScopesMissing,
            message = "Required scopes are missing",
        )

        val decision = assertIs<ExportFailureDecision.Terminal>(error.toExportFailureDecision())
        assertTrue(decision.invalidatesGoogleLink)
    }

    @Test
    fun unrelatedTerminalFailureKeepsCachedLink() {
        val error = FirebaseCallableException(
            code = "INTERNAL",
            reason = null,
            message = "Export failed",
        )

        val decision = assertIs<ExportFailureDecision.Terminal>(error.toExportFailureDecision())
        assertEquals("EXPORT_FAILED", decision.reason)
        assertFalse(decision.invalidatesGoogleLink)
    }

    private fun assertNoOauthCredentialKeys(keys: Set<String>) {
        val forbiddenKeys = setOf(
            "googleAccessToken",
            "accessToken",
            "refreshToken",
            "idToken",
            "authorizationCode",
            "nonce",
        )
        assertEquals(emptySet(), keys intersect forbiddenKeys)
    }
}

private class RecordingExportWorkerScheduler : ExportWorkerScheduler {
    var request: ExportWorkRequest? = null

    override fun enqueue(
        exportId: Long,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
        spreadsheetTitle: String,
        folderName: String,
    ) {
        request = ExportWorkRequest(
            exportId = exportId,
            tabLayout = tabLayout,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            spreadsheetTitle = spreadsheetTitle,
            folderName = folderName,
        )
    }
}

private class FakeExportHistoryDao : ExportHistoryDao {
    override suspend fun insert(entity: ExportHistoryEntity): Long = 42

    override suspend fun updateResult(
        id: Long,
        status: ExportStatus,
        url: String?,
        completedAt: Long,
    ) = Unit

    override suspend fun updateError(
        id: Long,
        status: ExportStatus,
        error: String?,
        completedAt: Long,
    ) = Unit

    override fun observeLatest(): Flow<ExportHistoryEntity?> = flowOf(null)

    override suspend fun deleteAll() = Unit
}
