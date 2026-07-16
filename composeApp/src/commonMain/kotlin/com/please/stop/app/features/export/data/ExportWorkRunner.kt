package com.please.stop.app.features.export.data

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.ExportStatus
import com.please.stop.app.core.runSuspendCatching
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableErrorReason
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableException
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions
import com.please.stop.app.utils.date.now
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

internal class ExportWorkRunner(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val userProfileDao: UserProfileDao,
    private val callableFunctions: FirebaseCallableFunctions,
    private val exportHistoryDao: ExportHistoryDao,
    private val googleAccountStorage: IGoogleAccountStorage,
    private val payloadBuilder: ExportToSheetsPayloadBuilder,
) {

    suspend fun run(request: ExportWorkRequest): ExportWorkResult {
        return try {
            execute(request)
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            markFailed(exportId = request.exportId, error = EXPORT_FAILED_REASON)
            ExportWorkResult.Failure
        }
    }

    suspend fun markRetryExhausted(exportId: Long) {
        markFailed(
            exportId = exportId,
            error = FirebaseCallableErrorReason.GoogleTokenEndpointUnavailable.value,
        )
    }

    private suspend fun execute(request: ExportWorkRequest): ExportWorkResult {
        val dateRange = inclusiveExportDateRange(
            startDateMillis = request.startDateMillis,
            endDateMillis = request.endDateMillis,
        )
        // TODO: Larger exports require paged DAO reads and matching
        //  idempotent chunk handling in exportToSheets.
        val expenses = expenseDao.getExpensesForExport(
            fromEpochMillis = dateRange.fromMillis,
            toEpochMillis = dateRange.toMillis,
        )
        val allCategories = categoryDao.observeAllIncludingArchived().first()
        val allSubcategories = subcategoryDao.observeAllIncludingArchived().first()
        val userProfile = userProfileDao.get()

        val payload = payloadBuilder.build(
            request = request,
            expenses = expenses,
            categories = allCategories,
            subcategories = allSubcategories,
            userProfile = userProfile,
        )

        return callableFunctions
            .call(functionName = "exportToSheets", data = payload)
            .fold(
                onSuccess = { data ->
                    exportHistoryDao.updateResult(
                        id = request.exportId,
                        status = ExportStatus.SUCCESS,
                        url = data["spreadsheetUrl"] as? String,
                        completedAt = now().toEpochMilliseconds(),
                    )
                    ExportWorkResult.Success
                },
                onFailure = { error ->
                    handleCallableFailure(exportId = request.exportId, error = error)
                },
            )
    }

    private suspend fun handleCallableFailure(
        exportId: Long,
        error: Throwable,
    ): ExportWorkResult {
        return when (val decision = error.toExportFailureDecision()) {
            ExportFailureDecision.Retry -> ExportWorkResult.Retry
            is ExportFailureDecision.Terminal -> {
                if (decision.invalidatesGoogleLink) {
                    runSuspendCatching { googleAccountStorage.delete() }
                }
                markFailed(exportId = exportId, error = decision.reason)
                ExportWorkResult.Failure
            }
        }
    }

    private suspend fun markFailed(exportId: Long, error: String?) {
        exportHistoryDao.updateError(
            id = exportId,
            status = ExportStatus.FAILED,
            error = error,
            completedAt = now().toEpochMilliseconds(),
        )
    }
}

internal sealed interface ExportWorkResult {
    data object Success : ExportWorkResult
    data object Retry : ExportWorkResult
    data object Failure : ExportWorkResult
}

internal sealed interface ExportFailureDecision {
    data object Retry : ExportFailureDecision
    data class Terminal(
        val reason: String,
        val invalidatesGoogleLink: Boolean,
    ) : ExportFailureDecision
}

internal fun Throwable.toExportFailureDecision(): ExportFailureDecision {
    val reason = (this as? FirebaseCallableException)?.reason
    return if (
        reason == FirebaseCallableErrorReason.GoogleTokenEndpointUnavailable ||
        reason == FirebaseCallableErrorReason.ExportInProgress ||
        reason == FirebaseCallableErrorReason.SheetsTemporarilyUnavailable
    ) {
        ExportFailureDecision.Retry
    } else {
        ExportFailureDecision.Terminal(
            reason = reason?.value ?: EXPORT_FAILED_REASON,
            invalidatesGoogleLink = reason == FirebaseCallableErrorReason.GoogleReconnectRequired ||
                reason == FirebaseCallableErrorReason.GoogleScopesMissing,
        )
    }
}

private const val EXPORT_FAILED_REASON = "EXPORT_FAILED"
