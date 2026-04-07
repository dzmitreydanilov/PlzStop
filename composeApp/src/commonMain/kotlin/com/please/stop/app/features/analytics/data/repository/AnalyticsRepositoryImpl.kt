package com.please.stop.app.features.analytics.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.core.db.entity.UserProfileEntity
import com.please.stop.app.core.featureflags.FeatureFlags
import com.please.stop.app.features.analytics.domain.model.AnalyticsData
import com.please.stop.app.features.analytics.domain.model.CategorySpendingItem
import com.please.stop.app.features.analytics.domain.model.DailySpendingPoint
import com.please.stop.app.features.analytics.domain.model.DayExpenseItem
import com.please.stop.app.features.analytics.domain.model.DayExpensesData
import com.please.stop.app.features.analytics.domain.model.MonthlyTotal
import com.please.stop.app.features.analytics.domain.model.SubcategorySpendingItem
import com.please.stop.app.features.analytics.domain.repository.AnalyticsRepository
import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import com.please.stop.app.utils.date.lengthOfMonth
import com.please.stop.app.utils.date.localDateTimeFromMillis
import com.please.stop.app.utils.date.localDateToday
import com.please.stop.app.utils.date.monthMillisRange
import com.please.stop.app.utils.date.monthName
import com.please.stop.app.utils.date.previousMonth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.plus

private const val MONTHS_TO_SHOW = 6
private const val MONTH_LABEL_LENGTH = 3

class AnalyticsRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val expenseDao: ExpenseDao,
    private val featureFlags: FeatureFlags,
    private val ioDispatcher: CoroutineDispatcher,
) : AnalyticsRepository {

    override fun observeAnalyticsData(): Flow<AnalyticsData> {
        return featureFlags.observeSubcategoriesEnabled()
            .flatMapLatest { enabled ->
                if (enabled) observeWithSubcategories() else observeWithoutSubcategories()
            }
            .flowOn(ioDispatcher)
    }

    private fun observeWithoutSubcategories(): Flow<AnalyticsData> {
        val ctx = currentMonthContext()

        return combine(
            categoryDao.observeAll(),
            expenseDao.observeSpendingByCategory(ctx.fromMillis, ctx.toMillis),
            expenseDao.observeTotalSpending(ctx.fromMillis, ctx.toMillis),
            expenseDao.observeExpensesInRange(ctx.fromMillis, ctx.toMillis),
        ) { categories, spendingByCategory, totalSpent, currentMonthExpenses ->
            val profile = userProfileDao.get()
            val categoryMap = categories.associateBy { it.id }

            val categorySpending = spendingByCategory
                .filter { it.totalMinorUnits > 0 }
                .map { spending ->
                    val entity = categoryMap[spending.categoryId]
                    CategorySpendingItem(
                        categoryId = spending.categoryId,
                        name = entity?.name.orEmpty(),
                        iconKey = entity?.iconKey.orEmpty(),
                        spentMinorUnits = spending.totalMinorUnits,
                    )
                }

            buildAnalyticsData(ctx, profile, categorySpending, totalSpent, currentMonthExpenses)
        }
    }

    private fun observeWithSubcategories(): Flow<AnalyticsData> {
        val ctx = currentMonthContext()

        return combine(
            categoryDao.observeAll(),
            subcategoryDao.observeAll(),
            expenseDao.observeSpendingByCategoryAndSubcategory(ctx.fromMillis, ctx.toMillis),
            expenseDao.observeTotalSpending(ctx.fromMillis, ctx.toMillis),
            expenseDao.observeExpensesInRange(ctx.fromMillis, ctx.toMillis),
        ) { categories, subcategories, spendingByBoth, totalSpent, currentMonthExpenses ->
            val profile = userProfileDao.get()
            val categoryMap = categories.associateBy { it.id }
            val subcategoryMap = subcategories.associateBy { it.id }

            val categorySpending = spendingByBoth
                .groupBy { it.categoryId }
                .mapNotNull { (categoryId, items) ->
                    val totalForCategory = items.sumOf { it.totalMinorUnits }
                    if (totalForCategory <= 0) return@mapNotNull null

                    val entity = categoryMap[categoryId]
                    val subcatItems = items
                        .filter { it.subcategoryId != null && it.totalMinorUnits > 0 }
                        .map { item ->
                            SubcategorySpendingItem(
                                subcategoryId = item.subcategoryId ?: 0L,
                                name = subcategoryMap[item.subcategoryId]?.name.orEmpty(),
                                spentMinorUnits = item.totalMinorUnits,
                            )
                        }
                        .sortedByDescending { it.spentMinorUnits }

                    CategorySpendingItem(
                        categoryId = categoryId,
                        name = entity?.name.orEmpty(),
                        iconKey = entity?.iconKey.orEmpty(),
                        spentMinorUnits = totalForCategory,
                        subcategorySpending = subcatItems,
                    )
                }

            buildAnalyticsData(ctx, profile, categorySpending, totalSpent, currentMonthExpenses)
        }
    }

    private suspend fun buildAnalyticsData(
        ctx: MonthContext,
        profile: UserProfileEntity?,
        categorySpending: List<CategorySpendingItem>,
        totalSpent: Long?,
        currentMonthExpenses: List<ExpenseEntity>,
    ): AnalyticsData {
        val dailySpending =
            buildDailySpending(currentMonthExpenses.map { it.dateEpochMillis to it.amountMinorUnits })
        val monthlyTotals = buildMonthlyTotals(
            ctx.monthRanges,
            ctx.today.year,
            ctx.today.month.number
        )

        return AnalyticsData(
            currencySymbol = profile?.currencySymbol.orEmpty(),
            decimalPlaces = profile?.decimalPlaces ?: DEFAULT_CURRENCY_DECIMAL_PLACES,
            totalSpentMinorUnits = totalSpent ?: 0L,
            monthlyBudgetMinorUnits = profile?.monthlyBudget ?: 0L,
            daysInMonth = ctx.daysInMonth,
            currentDayOfMonth = ctx.today.day,
            categorySpending = categorySpending,
            dailySpending = dailySpending,
            monthlyTotals = monthlyTotals,
        )
    }

    private fun buildDailySpending(expenses: List<Pair<Long, Long>>): List<DailySpendingPoint> {
        return expenses
            .groupBy { (dateMillis, _) ->
                localDateTimeFromMillis(dateMillis).date.dayOfMonth
            }
            .map { (dayOfMonth, dayExpenses) ->
                DailySpendingPoint(
                    dayOfMonth = dayOfMonth,
                    totalMinorUnits = dayExpenses.sumOf { it.second },
                )
            }
            .sortedBy { it.dayOfMonth }
    }

    private suspend fun buildMonthlyTotals(
        monthRanges: List<MonthRangeInfo>,
        currentYear: Int,
        currentMonth: Int,
    ): List<MonthlyTotal> {
        return monthRanges.map { info ->
            val total = expenseDao.observeTotalSpending(info.fromMillis, info.toMillis).first() ?: 0L
            MonthlyTotal(
                label = monthName(info.month).take(MONTH_LABEL_LENGTH),
                totalMinorUnits = total,
                isCurrent = info.year == currentYear && info.month == currentMonth,
            )
        }
    }

    private fun buildMonthRanges(currentYear: Int, currentMonth: Int): List<MonthRangeInfo> {
        val ranges = mutableListOf<MonthRangeInfo>()
        var year = currentYear
        var month = currentMonth

        repeat(MONTHS_TO_SHOW) { i ->
            if (i > 0) {
                val prev = previousMonth(year, month)
                year = prev.first
                month = prev.second
            }
            val range = monthMillisRange(year, month)
            ranges.add(MonthRangeInfo(year, month, range.fromMillis, range.toMillis))
        }

        return ranges.reversed()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun loadDayExpenses(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Result<DayExpensesData> = withContext(ioDispatcher) {
        try {
            val tz = TimeZone.currentSystemDefault()
            val dayStart = LocalDate(year, month, dayOfMonth)
            val fromMillis = dayStart.atStartOfDayIn(tz).toEpochMilliseconds()
            val toMillis = dayStart.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()

            val expenses = expenseDao.getExpensesInRange(fromMillis, toMillis)
            val categories = categoryDao.observeAll().first().associateBy { it.id }
            val subcatsEnabled = featureFlags.subcategoriesEnabled()
            val subcategoryMap = if (subcatsEnabled) {
                subcategoryDao.observeAll().first().associateBy { it.id }
            } else {
                emptyMap()
            }
            val profile = userProfileDao.get()

            val items = expenses.map { entity ->
                DayExpenseItem(
                    id = entity.id,
                    title = entity.title,
                    categoryIconKey = categories[entity.categoryId]?.iconKey.orEmpty(),
                    subcategoryName = entity.subcategoryId?.let { subcategoryMap[it]?.name },
                    amountMinorUnits = entity.amountMinorUnits,
                    dateEpochMillis = entity.dateEpochMillis,
                )
            }

            Result.success(
                DayExpensesData(
                    dayOfMonth = dayOfMonth,
                    currencySymbol = profile?.currencySymbol.orEmpty(),
                    decimalPlaces = profile?.decimalPlaces ?: DEFAULT_CURRENCY_DECIMAL_PLACES,
                    expenses = items,
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun currentMonthContext(): MonthContext {
        val today = localDateToday()
        val range = monthMillisRange(today.year, today.monthNumber)
        return MonthContext(
            fromMillis = range.fromMillis,
            toMillis = range.toMillis,
            today = today,
            daysInMonth = LocalDate(today.year, today.month.number, 1).lengthOfMonth(),
            monthRanges = buildMonthRanges(today.year, today.month.number),
        )
    }

    private data class MonthContext(
        val fromMillis: Long,
        val toMillis: Long,
        val today: LocalDate,
        val daysInMonth: Int,
        val monthRanges: List<MonthRangeInfo>,
    )

    private data class MonthRangeInfo(
        val year: Int,
        val month: Int,
        val fromMillis: Long,
        val toMillis: Long,
    )
}
