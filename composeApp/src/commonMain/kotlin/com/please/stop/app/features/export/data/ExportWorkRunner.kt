package com.please.stop.app.features.export.data

import com.please.stop.app.core.IFcmTokenProvider
import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.ExportStatus
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
    private val fcmTokenProvider: IFcmTokenProvider,
    private val exportHistoryDao: ExportHistoryDao,
) {

    suspend fun run(
        exportId: Long,
        googleAccessToken: String,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
    ): Boolean {
        return try {
            execute(
                exportId = exportId,
                googleAccessToken = googleAccessToken,
                tabLayout = tabLayout,
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            markFailed(exportId = exportId, error = e.message)
            false
        }
    }

    private suspend fun execute(
        exportId: Long,
        googleAccessToken: String,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
    ): Boolean {
        val expenses = expenseDao.getExpensesInRange(startDateMillis, endDateMillis)
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
        val fcmToken = fcmTokenProvider.getToken()
        val payload = mapOf(
            "googleAccessToken" to googleAccessToken,
            "fcmToken" to fcmToken,
            "dateRangeLabel" to "${formatIsoDate(startDateMillis)} to ${formatIsoDate(endDateMillis)}",
            "tabLayout" to tabLayout,
            "currencySymbol" to userProfile?.currencySymbol.orEmpty(),
            "decimalPlaces" to decimalPlaces,
            "compressed" to payloadExpenses.compressed,
            "expenses" to payloadExpenses.expenses,
        )

        return callableFunctions.call(functionName = "exportToSheets", data = payload).fold(
            onSuccess = { data ->
                exportHistoryDao.updateResult(
                    id = exportId,
                    status = ExportStatus.SUCCESS,
                    url = data["spreadsheetUrl"] as? String,
                    completedAt = now().toEpochMilliseconds(),
                )
                true
            },
            onFailure = { error ->
                markFailed(exportId = exportId, error = error.message)
                false
            },
        )
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

private fun ExportExpenseRow.toPayloadMap(): Map<String, String> = mapOf(
    "date" to date,
    "title" to title,
    "category" to category,
    "subcategory" to subcategory,
    "amount" to amount,
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
