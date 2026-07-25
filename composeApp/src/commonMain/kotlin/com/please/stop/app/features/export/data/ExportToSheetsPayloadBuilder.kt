package com.please.stop.app.features.export.data

import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.core.db.entity.SubcategoryEntity
import com.please.stop.app.core.db.entity.UserProfileEntity
import com.please.stop.app.features.export.domain.model.ExportExpenseRow
import com.please.stop.app.utils.date.localDateTimeFromMillis
import kotlinx.serialization.json.Json

internal class ExportToSheetsPayloadBuilder {

    fun build(
        request: ExportWorkRequest,
        expenses: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        subcategories: List<SubcategoryEntity>,
        userProfile: UserProfileEntity?,
    ): Map<String, Any?> {
        val categoryMap = categories.associateBy { it.id }
        val subcategoryMap = subcategories.associateBy { it.id }
        val decimalPlaces = userProfile?.decimalPlaces ?: DEFAULT_DECIMAL_PLACES
        val rows = expenses.map { expense ->
            ExportExpenseRow(
                date = formatIsoDate(expense.dateEpochMillis),
                title = expense.title,
                category = categoryMap[expense.categoryId]?.name.orEmpty(),
                subcategory = subcategoryMap[expense.subcategoryId]?.name.orEmpty(),
                amount = formatMinorUnits(expense.amountMinorUnits, decimalPlaces),
                notes = expense.notes.orEmpty(),
            )
        }
        val payloadExpenses = buildPayloadExpenses(rows)

        return buildMap {
            put("exportId", request.exportId.toString())
            put(
                "dateRangeLabel",
                "${formatIsoDate(request.startDateMillis)} to ${formatIsoDate(request.endDateMillis)}",
            )
            put("tabLayout", request.tabLayout)
            put("currencySymbol", userProfile?.currencySymbol.orEmpty())
            put("decimalPlaces", decimalPlaces)
            put("compressed", payloadExpenses.compressed)
            put("expenses", payloadExpenses.expenses)
            request.spreadsheetTitle.trim().takeIf(String::isNotEmpty)?.let { put("title", it) }
            request.folderName.trim().takeIf(String::isNotEmpty)?.let { put("folderName", it) }
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

    private data class PayloadExpenses(
        val compressed: Boolean,
        val expenses: Any,
    )

    private companion object {
        const val COMPRESSION_THRESHOLD_BYTES = 100_000
        const val DEFAULT_DECIMAL_PLACES = 2
    }
}

private fun ExportExpenseRow.toPayloadMap(): Map<String, Any> = mapOf(
    "date" to date,
    "title" to title,
    "category" to category,
    "subcategory" to subcategory,
    "amount" to amount.toDouble(),
    "notes" to notes,
)

private fun formatIsoDate(epochMillis: Long): String =
    localDateTimeFromMillis(epochMillis).date.toString()
