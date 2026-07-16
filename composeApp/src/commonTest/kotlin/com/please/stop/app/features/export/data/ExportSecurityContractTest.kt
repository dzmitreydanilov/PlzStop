package com.please.stop.app.features.export.data

import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.entity.ExportHistoryEntity
import com.please.stop.app.core.db.entity.ExportStatus
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
        val payload = buildExportToSheetsPayload(
            exportId = 42,
            dateRangeLabel = "2026-01-01 to 2026-01-31",
            tabLayout = "single_tab",
            currencySymbol = "EUR",
            decimalPlaces = 2,
            compressed = false,
            expenses = emptyList<Map<String, String>>(),
            spreadsheetTitle = "January expenses",
            folderName = "PlzStop exports",
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
