package com.please.stop.app.features.export.data.repository

import com.please.stop.app.core.DocumentSharer
import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.features.export.data.CsvExportBuilder
import com.please.stop.app.features.export.data.formatMinorUnits
import com.please.stop.app.features.export.data.inclusiveExportDateRange
import com.please.stop.app.features.export.domain.model.ExportExpenseRow
import com.please.stop.app.utils.date.localDateTimeFromMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class CSVExportRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val userProfileDao: UserProfileDao,
    private val documentSharer: DocumentSharer,
    private val csvExportBuilder: CsvExportBuilder
) {

    fun enqueExport(
        startDateMillis: Long,
        endDateMillis: Long
    ): Flow<Result<Unit>> {
        return flow {
            val dateRange = inclusiveExportDateRange(
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
            )
            val expenses = expenseDao.getExpensesForExport(
                fromEpochMillis = dateRange.fromMillis,
                toEpochMillis = dateRange.toMillis,
            )
            val rows = buildExportRows(expenses)
            val csv = csvExportBuilder.build(rows)
            val result = documentSharer.shareCsv(
                fileName = buildCsvFileName(startDateMillis, endDateMillis),
                content = csv,
            )
            emit(result)
        }
    }

    private suspend fun buildExportRows(expenses: List<ExpenseEntity>): List<ExportExpenseRow> {
        val categories = categoryDao.observeAllIncludingArchived().first().associateBy { it.id }
        val subcategories =
            subcategoryDao.observeAllIncludingArchived().first().associateBy { it.id }
        val decimalPlaces = userProfileDao.get()?.decimalPlaces ?: DEFAULT_DECIMAL_PLACES
        return expenses.map { expense ->
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

    private companion object {
        const val DEFAULT_DECIMAL_PLACES = 2
    }
}
