package com.please.stop.app.features.export.data.repository

import com.please.stop.app.core.DocumentSharer
import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.features.export.data.CsvExportBuilder
import com.please.stop.app.features.export.domain.model.ExportExpenseRow
import com.please.stop.app.features.export.domain.repository.ExportRepository
import com.please.stop.app.utils.date.localDateTimeFromMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

class CSVExportRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val userProfileDao: UserProfileDao,
    private val documentSharer: DocumentSharer,
    private val csvExportBuilder: CsvExportBuilder
) : ExportRepository {

    override fun enqueExport(
        startDateMillis: Long,
        endDateMillis: Long
    ): Flow<Result<Unit>> {
        return flow {
            val expenses = expenseDao.getExpensesInRange(startDateMillis, endDateMillis)
            val rows = buildExportRows(expenses)
            val csv = csvExportBuilder.build(rows)
            documentSharer.shareCsv(
                fileName = buildCsvFileName(startDateMillis, endDateMillis),
                content = csv,
            )
            emit(Result.success(Unit))
        }
    }

    private suspend fun buildExportRows(expenses: List<ExpenseEntity>): List<ExportExpenseRow> {
        val categories = categoryDao.observeAllIncludingArchived().first().associateBy { it.id }
        val subcategories =
            subcategoryDao.observeAllIncludingArchived().first().associateBy { it.id }
        val decimalPlaces = userProfileDao.get()?.decimalPlaces ?: DEFAULT_DECIMAL_PLACES
        return expenses
            .sortedBy { it.dateEpochMillis }
            .map { expense ->
                ExportExpenseRow(
                    date = formatIsoDate(expense.dateEpochMillis),
                    title = expense.title,
                    category = categories[expense.categoryId]?.name.orEmpty(),
                    subcategory = subcategories[expense.subcategoryId]?.name.orEmpty(),
                    amount = formatMinorUnits(expense.amountMinorUnits, decimalPlaces),
                    notes = expense.notes.orEmpty(),
                )
            }
    }

    private fun buildCsvFileName(startDateMillis: Long, endDateMillis: Long): String {
        return "plzstop-export-${formatIsoDate(startDateMillis)}-to-${formatIsoDate(endDateMillis)}.csv"
    }


    private fun formatIsoDate(epochMillis: Long): String =
        localDateTimeFromMillis(epochMillis).date.toString()

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

    private companion object {
        const val DEFAULT_DECIMAL_PLACES = 2
        const val INITIAL_MULTIPLIER = 1L
        const val DECIMAL_RADIX = 10L
    }
}
