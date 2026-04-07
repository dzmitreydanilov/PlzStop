package com.please.stop.app.features.expenses.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.ReceiptDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.core.db.entity.ReceiptEntity
import com.please.stop.app.features.expenses.domain.model.CurrencyDisplayConfig
import com.please.stop.app.features.expenses.domain.model.DayGroup
import com.please.stop.app.features.expenses.domain.model.ExpenseListItem
import com.please.stop.app.features.expenses.domain.model.MonthlyExpenseEntry
import com.please.stop.app.features.expenses.domain.model.MonthlyExpensesData
import com.please.stop.app.features.expenses.domain.repository.MonthlyExpensesRepository
import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import com.please.stop.app.utils.date.monthMillisRange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class MonthlyExpensesRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val receiptDao: ReceiptDao,
    private val userProfileDao: UserProfileDao,
    private val ioDispatcher: CoroutineDispatcher,
) : MonthlyExpensesRepository {

    @OptIn(ExperimentalTime::class)
    override fun observeExpensesForMonth(year: Int, month: Int): Flow<Result<MonthlyExpensesData>> {
        val range = monthMillisRange(year, month)
        return expenseDao.observeExpensesInRange(range.fromMillis, range.toMillis)
            .map { entities ->
                val categoryMap = categoryDao.observeAllIncludingArchived().first().associateBy { it.id }
                val receiptIds = entities.mapNotNull { it.receiptId }.distinct()
                val receiptMap = if (receiptIds.isNotEmpty()) {
                    receiptDao.getByIds(receiptIds).associateBy { it.id }
                } else {
                    emptyMap()
                }
                val currency = resolveCurrencyConfig()
                Result.success(mapToMonthlyExpensesData(year, month, entities, categoryMap, receiptMap, currency))
            }
            .catch { emit(Result.failure(it)) }
            .flowOn(ioDispatcher)
    }

    override suspend fun getCurrencyConfig(): Result<CurrencyDisplayConfig> = runCatching {
        resolveCurrencyConfig()
    }

    private suspend fun resolveCurrencyConfig(): CurrencyDisplayConfig {
        val profile = userProfileDao.get()
        return CurrencyDisplayConfig(
            symbol = profile?.currencySymbol.orEmpty(),
            decimalPlaces = profile?.decimalPlaces ?: DEFAULT_CURRENCY_DECIMAL_PLACES,
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun mapToMonthlyExpensesData(
        year: Int,
        month: Int,
        entities: List<ExpenseEntity>,
        categoryMap: Map<Long, CategoryEntity>,
        receiptMap: Map<Long, ReceiptEntity>,
        currency: CurrencyDisplayConfig,
    ): MonthlyExpensesData {
        val tz = TimeZone.currentSystemDefault()
        val grouped = entities.groupBy { entity ->
            val date = Instant.fromEpochMilliseconds(entity.dateEpochMillis)
                .toLocalDateTime(tz).date
            date.atStartOfDayIn(tz).toEpochMilliseconds()
        }
        val dayGroups = grouped.entries
            .sortedByDescending { it.key }
            .map { (dayMillis, dayEntities) ->
                DayGroup(
                    dayEpochMillis = dayMillis,
                    entries = buildEntriesForDay(dayEntities, categoryMap, receiptMap),
                    totalMinorUnits = dayEntities.sumOf { it.amountMinorUnits },
                )
            }
        return MonthlyExpensesData(
            year = year,
            month = month,
            dayGroups = dayGroups,
            totalMinorUnits = entities.sumOf { it.amountMinorUnits },
            currency = currency,
        )
    }

    private fun buildEntriesForDay(
        dayEntities: List<ExpenseEntity>,
        categoryMap: Map<Long, CategoryEntity>,
        receiptMap: Map<Long, ReceiptEntity>,
    ): List<MonthlyExpenseEntry> {
        val receiptGrouped = mutableMapOf<Long, MutableList<ExpenseEntity>>()
        val singles = mutableListOf<ExpenseEntity>()

        for (entity in dayEntities) {
            val receiptId = entity.receiptId
            if (receiptId != null) {
                receiptGrouped.getOrPut(receiptId) { mutableListOf() }.add(entity)
            } else {
                singles.add(entity)
            }
        }

        val singleEntries = singles.map { entity ->
            MonthlyExpenseEntry.Single(entity.toExpenseListItem(categoryMap))
        }
        val receiptEntries = receiptGrouped.map { (receiptId, receiptExpenses) ->
            MonthlyExpenseEntry.ReceiptGroup(
                receiptId = receiptId,
                merchantName = receiptMap[receiptId]?.merchantName,
                expenses = receiptExpenses.map { it.toExpenseListItem(categoryMap) },
                totalMinorUnits = receiptExpenses.sumOf { it.amountMinorUnits },
            )
        }

        return (singleEntries + receiptEntries).sortedByDescending { entry ->
            when (entry) {
                is MonthlyExpenseEntry.Single -> entry.expense.dateEpochMillis
                is MonthlyExpenseEntry.ReceiptGroup -> entry.expenses.maxOfOrNull { it.dateEpochMillis } ?: 0L
            }
        }
    }
}

private fun ExpenseEntity.toExpenseListItem(categoryMap: Map<Long, CategoryEntity>): ExpenseListItem {
    val category = categoryMap[categoryId]
    return ExpenseListItem(
        id = id,
        title = title,
        categoryName = category?.name.orEmpty(),
        categoryIconKey = category?.iconKey.orEmpty(),
        amountMinorUnits = amountMinorUnits,
        dateEpochMillis = dateEpochMillis,
    )
}
