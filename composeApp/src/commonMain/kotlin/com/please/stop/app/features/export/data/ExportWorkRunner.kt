package com.please.stop.app.features.export.data

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.ExportStatus
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableErrorReason
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableException
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions
import com.please.stop.app.features.export.domain.model.ExportExpenseRow
import com.please.stop.app.utils.date.localDateTimeFromMillis
import com.please.stop.app.utils.date.now
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

internal class ExportWorkRunner(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val userProfileDao: UserProfileDao,
    private val callableFunctions: FirebaseCallableFunctions,
    private val exportHistoryDao: ExportHistoryDao,
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
        val expenses = expenseDao.getExpensesInRange(
            fromEpochMillis = dateRange.fromMillis,
            toEpochMillis = dateRange.toMillis,
        )
        val allCategories = categoryDao.observeAllIncludingArchived().first()
        val allSubcategories = subcategoryDao.observeAllIncludingArchived().first()
        val userProfile = userProfileDao.get()

        val categoryMap = allCategories.associateBy { it.id }
        val subcategoryMap = allSubcategories.associateBy { it.id }
        val decimalPlaces = userProfile?.decimalPlaces ?: DEFAULT_DECIMAL_PLACES
        val expensesList = expenses
            .sortedBy { it.dateEpochMillis }
            .map { expense ->
                ExportExpenseRow(
                    date = formatIsoDate(expense.dateEpochMillis),
                    title = expense.title,
                    category = categoryMap[expense.categoryId]?.name.orEmpty(),
                    subcategory = subcategoryMap[expense.subcategoryId]?.name.orEmpty(),
                    amount = formatMinorUnits(expense.amountMinorUnits, decimalPlaces),
                    notes = expense.notes.orEmpty(),
                )
            }

        val payloadExpenses = buildPayloadExpenses(expensesList)
        val payload = buildExportToSheetsPayload(
            exportId = request.exportId,
            dateRangeLabel = "${formatIsoDate(request.startDateMillis)} to " +
                formatIsoDate(request.endDateMillis),
            tabLayout = request.tabLayout,
            currencySymbol = userProfile?.currencySymbol.orEmpty(),
            decimalPlaces = decimalPlaces,
            compressed = payloadExpenses.compressed,
            expenses = payloadExpenses.expenses,
            spreadsheetTitle = request.spreadsheetTitle,
            folderName = request.folderName,
        )

        return callableFunctions.call(functionName = "exportToSheets", data = payload).fold(
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
                markFailed(exportId = exportId, error = decision.reason)
                ExportWorkResult.Failure
            }
        }
    }

    private fun buildPayloadExpenses(expenses: List<ExportExpenseRow>): PayloadExpenses {
        val expensesJson = Json.encodeToString(expenses)
        val bytes = expensesJson.encodeToByteArray()
        return if (bytes.size >= COMPRESSION_THRESHOLD_BYTES) {
            PayloadExpenses(
                compressed = true,
                expenses = gzipBase64(bytes),
            )
        } else {
            PayloadExpenses(
                compressed = false,
                expenses = expenses.map { it.toPayloadMap() },
            )
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

    private data class PayloadExpenses(
        val compressed: Boolean,
        val expenses: Any,
    )

    private companion object {
        const val COMPRESSION_THRESHOLD_BYTES = 100_000
        const val DEFAULT_DECIMAL_PLACES = 2
    }
}

internal sealed interface ExportWorkResult {
    data object Success : ExportWorkResult
    data object Retry : ExportWorkResult
    data object Failure : ExportWorkResult
}

internal sealed interface ExportFailureDecision {
    data object Retry : ExportFailureDecision
    data class Terminal(val reason: String) : ExportFailureDecision
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
        ExportFailureDecision.Terminal(reason = reason?.value ?: EXPORT_FAILED_REASON)
    }
}

internal fun buildExportToSheetsPayload(
    exportId: Long,
    dateRangeLabel: String,
    tabLayout: String,
    currencySymbol: String,
    decimalPlaces: Int,
    compressed: Boolean,
    expenses: Any,
    spreadsheetTitle: String,
    folderName: String,
): Map<String, Any?> = buildMap {
    put("exportId", exportId.toString())
    put("dateRangeLabel", dateRangeLabel)
    put("tabLayout", tabLayout)
    put("currencySymbol", currencySymbol)
    put("decimalPlaces", decimalPlaces)
    put("compressed", compressed)
    put("expenses", expenses)
    spreadsheetTitle.trim().takeIf(String::isNotEmpty)?.let { put("title", it) }
    folderName.trim().takeIf(String::isNotEmpty)?.let { put("folderName", it) }
}

private fun ExportExpenseRow.toPayloadMap(): Map<String, Any> = mapOf(
    "date" to date,
    "title" to title,
    "category" to category,
    "subcategory" to subcategory,
    "amount" to amount.toDouble(),
    "notes" to notes,
)

private fun formatIsoDate(epochMillis: Long): String = localDateTimeFromMillis(epochMillis).date.toString()

private fun formatMinorUnits(minorUnits: Long, decimalPlaces: Int): String {
    if (decimalPlaces == 0) return minorUnits.toString()
    val multiplier = powerOfTen(decimalPlaces)
    val sign = if (minorUnits < 0) "-" else ""
    val absoluteValue = abs(minorUnits)
    val whole = absoluteValue / multiplier
    val fraction = (absoluteValue % multiplier).toString().padStart(decimalPlaces, '0')
    return "$sign$whole.$fraction"
}

private fun powerOfTen(decimalPlaces: Int): Long {
    var result = INITIAL_MULTIPLIER
    repeat(decimalPlaces) { result *= DECIMAL_RADIX }
    return result
}

private const val INITIAL_MULTIPLIER = 1L
private const val DECIMAL_RADIX = 10L
private const val EXPORT_FAILED_REASON = "EXPORT_FAILED"
