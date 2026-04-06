package com.please.stop.app.features.analytics.presentation

import androidx.compose.ui.graphics.Color
import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.analytics.domain.model.AnalyticsData
import com.please.stop.app.features.analytics.domain.model.DailySpendingPoint
import com.please.stop.app.features.analytics.domain.model.DayExpensesData
import com.please.stop.app.features.analytics.domain.usecase.LoadDayExpensesUseCase
import com.please.stop.app.features.analytics.domain.usecase.ObserveAnalyticsDataUseCase
import com.please.stop.app.uicomponents.categoryEmojiForKey
import com.please.stop.app.utils.date.formatDayLabel
import com.please.stop.app.utils.date.localDateToday
import com.please.stop.app.utils.formatCurrencyAmount
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.math.pow
import com.please.stop.app.core.models.domain.Result as DomainResult

class AnalyticsStateHolder(
    private val observeAnalyticsDataUseCase: ObserveAnalyticsDataUseCase,
    private val loadDayExpensesUseCase: LoadDayExpensesUseCase,
) : StateHolder<AnalyticsState, AnalyticsEvent>() {

    override val tag = "AnalyticsStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    override fun getInitial(): AnalyticsState = AnalyticsState.Loading

    override fun collectFlowsOnInit(): Flow<DomainResult> = observeAnalyticsDataUseCase()

    override fun resolveEventResult(event: AnalyticsEvent): Flow<DomainResult> = when (event) {
        is AnalyticsEvent.DayTapped -> handleDayTapped(event.dayOfMonth)
        AnalyticsEvent.DismissDaySheet -> flowOf(AnalyticsResult.DismissSheet)
    }

    private fun handleDayTapped(dayOfMonth: Int): Flow<DomainResult> = flow {
        emit(AnalyticsResult.SheetLoading)
        emit(loadDayExpensesUseCase(currentYear, currentMonth, dayOfMonth))
    }

    override fun getStateByResult(previous: AnalyticsState, result: DomainResult): AnalyticsState {
        return when (result) {
            is ObserveAnalyticsDataUseCase.Result.Success -> result.data.toContent(previous)
            is ObserveAnalyticsDataUseCase.Result.Failure -> previous.toError()
            is AnalyticsResult.SheetLoading -> (previous as? AnalyticsState.Content)
                ?.copy(isDaySheetLoading = true) ?: previous
            is AnalyticsResult.DismissSheet -> (previous as? AnalyticsState.Content)
                ?.copy(selectedDaySheet = null, isDaySheetLoading = false) ?: previous
            is LoadDayExpensesUseCase.Result.Success -> (previous as? AnalyticsState.Content)
                ?.copy(
                    selectedDaySheet = result.data.toSheetUi(),
                    isDaySheetLoading = false,
                ) ?: previous
            is LoadDayExpensesUseCase.Result.Failure -> (previous as? AnalyticsState.Content)
                ?.copy(isDaySheetLoading = false) ?: previous
            else -> super.getStateByResult(previous, result)
        }
    }

    override fun getErrorStateByResult(result: DomainResult, errorType: ErrorType): AnalyticsState {
        return state.value.toError()
    }

    private fun AnalyticsData.toContent(previous: AnalyticsState): AnalyticsState.Content {
        val today = localDateToday()
        currentYear = today.year
        currentMonth = today.monthNumber

        val activeCategories = categorySpending.filter { it.spentMinorUnits > 0 }
        val divisor = 10.0.pow(decimalPlaces).toFloat()
        val hasBudget = monthlyBudgetMinorUnits > 0

        val budgetBurn = if (hasBudget) buildBudgetBurn() else null
        val projectedTotal = if (hasBudget && currentDayOfMonth > 0) buildProjectedTotal() else null
        val budgetPacingPoints = if (hasBudget) buildBudgetPacingPoints(divisor) else emptyList()

        val spendingSlices = activeCategories.mapIndexed { i, item ->
            val avgDaily = if (currentDayOfMonth > 0) item.spentMinorUnits / currentDayOfMonth else 0L
            SpendingSlice(
                name = item.name,
                formattedAmount = formatCurrencyAmount(item.spentMinorUnits, currencySymbol, decimalPlaces),
                formattedAvgDaily = formatCurrencyAmount(avgDaily, currencySymbol, decimalPlaces),
                amount = item.spentMinorUnits / divisor,
                color = chartColor(i),
            )
        }.toImmutableList()

        val dailyUiPoints = dailySpending.map { point ->
            DailySpendingUiPoint(
                dayOfMonth = point.dayOfMonth,
                amount = point.totalMinorUnits / divisor,
                formattedAmount = formatCurrencyAmount(point.totalMinorUnits, currencySymbol, decimalPlaces),
            )
        }.toImmutableList()

        val monthlyBarItems = monthlyTotals.map { month ->
            MonthlyBarUiItem(
                label = month.label,
                amount = month.totalMinorUnits / divisor,
                formattedAmount = formatCurrencyAmount(month.totalMinorUnits, currencySymbol, decimalPlaces),
                isCurrent = month.isCurrent,
            )
        }.toImmutableList()

        val heatmapDays = buildHeatmapDays(dailySpending)

        val categoryProgressItems = buildCategoryProgress(activeCategories)

        val previousContent = previous as? AnalyticsState.Content
        return AnalyticsState.Content(
            totalSpentFormatted = formatCurrencyAmount(totalSpentMinorUnits, currencySymbol, decimalPlaces),
            categoriesCount = categorySpending.size,
            activeCategoriesCount = activeCategories.size,
            hasAnyExpenses = totalSpentMinorUnits > 0,
            budgetBurn = budgetBurn,
            projectedTotal = projectedTotal,
            budgetPacingPoints = budgetPacingPoints.toImmutableList(),
            spendingSlices = spendingSlices,
            dailySpending = dailyUiPoints,
            monthlyBars = monthlyBarItems,
            heatmapDays = heatmapDays.toImmutableList(),
            categoryProgress = categoryProgressItems.toImmutableList(),
            selectedDaySheet = previousContent?.selectedDaySheet,
            isDaySheetLoading = previousContent?.isDaySheetLoading ?: false,
        )
    }

    private fun AnalyticsData.buildBudgetBurn(): BudgetBurnUi {
        val percentage = (totalSpentMinorUnits.toFloat() / monthlyBudgetMinorUnits).coerceIn(0f, MAX_BURN_PERCENTAGE)
        val remainingDays = (daysInMonth - currentDayOfMonth).coerceAtLeast(0)
        val remainingBudget = (monthlyBudgetMinorUnits - totalSpentMinorUnits).coerceAtLeast(0L)
        val dailyAllowance = if (remainingDays > 0) remainingBudget / remainingDays else 0L

        return BudgetBurnUi(
            spentFormatted = formatCurrencyAmount(totalSpentMinorUnits, currencySymbol, decimalPlaces),
            budgetFormatted = formatCurrencyAmount(monthlyBudgetMinorUnits, currencySymbol, decimalPlaces),
            percentage = percentage,
            dailyAllowanceFormatted = formatCurrencyAmount(dailyAllowance, currencySymbol, decimalPlaces),
        )
    }

    private fun AnalyticsData.buildProjectedTotal(): ProjectedTotalUi {
        val dailyRate = totalSpentMinorUnits.toDouble() / currentDayOfMonth
        val projected = (dailyRate * daysInMonth).toLong()
        val diff = projected - monthlyBudgetMinorUnits
        val isOver = diff > 0
        val absDiff = kotlin.math.abs(diff)

        return ProjectedTotalUi(
            projectedFormatted = formatCurrencyAmount(projected, currencySymbol, decimalPlaces),
            overUnderFormatted = formatCurrencyAmount(absDiff, currencySymbol, decimalPlaces),
            isOverBudget = isOver,
        )
    }

    private fun AnalyticsData.buildBudgetPacingPoints(divisor: Float): List<Float> {
        val budgetFloat = monthlyBudgetMinorUnits / divisor
        val dailyBudget = budgetFloat / daysInMonth
        return (1..daysInMonth).map { day -> dailyBudget * day }
    }

    private fun AnalyticsData.buildHeatmapDays(
        dailyPoints: List<DailySpendingPoint>,
    ): List<HeatmapDayUi> {
        val today = localDateToday()
        val firstDayOfMonth = LocalDate(today.year, today.monthNumber, 1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal + 1

        val dailyMap = dailyPoints.associate { it.dayOfMonth to it.totalMinorUnits }
        val maxAmount = dailyMap.values.maxOrNull() ?: 1L

        return (1..daysInMonth).map { day ->
            val offset = firstDayOfWeek - 1 + day - 1
            val dayOfWeek = offset % DAYS_IN_WEEK + 1
            val weekOfMonth = offset / DAYS_IN_WEEK
            val amount = dailyMap[day] ?: 0L
            val intensity = if (maxAmount > 0) amount.toFloat() / maxAmount else 0f

            HeatmapDayUi(
                dayOfMonth = day,
                dayOfWeek = dayOfWeek,
                weekOfMonth = weekOfMonth,
                intensity = intensity,
                formattedAmount = formatCurrencyAmount(amount, currencySymbol, decimalPlaces),
            )
        }
    }

    private fun AnalyticsData.buildCategoryProgress(
        activeCategories: List<com.please.stop.app.features.analytics.domain.model.CategorySpendingItem>,
    ): List<CategoryProgressUi> {
        val total = totalSpentMinorUnits.coerceAtLeast(1L)
        return activeCategories.mapIndexed { i, item ->
            CategoryProgressUi(
                name = item.name,
                iconKey = item.iconKey,
                percentage = item.spentMinorUnits.toFloat() / total,
                formattedAmount = formatCurrencyAmount(item.spentMinorUnits, currencySymbol, decimalPlaces),
                color = chartColor(i),
            )
        }
    }

    private fun DayExpensesData.toSheetUi(): DayExpensesSheetUi {
        val tz = TimeZone.currentSystemDefault()
        val dayStartMillis = LocalDate(currentYear, currentMonth, dayOfMonth)
            .atStartOfDayIn(tz)
            .toEpochMilliseconds()
        val total = expenses.sumOf { it.amountMinorUnits }

        return DayExpensesSheetUi(
            dayLabel = formatDayLabel(dayStartMillis),
            totalFormatted = formatCurrencyAmount(total, currencySymbol, decimalPlaces),
            expenses = expenses.map { item ->
                DayExpenseUiItem(
                    title = item.title,
                    categoryEmoji = categoryEmojiForKey(item.categoryIconKey),
                    formattedAmount = formatCurrencyAmount(item.amountMinorUnits, currencySymbol, decimalPlaces),
                )
            }.toImmutableList(),
        )
    }

    private fun AnalyticsState.toError(): AnalyticsState.Error = AnalyticsState.Error(
        totalSpentFormatted = totalSpentFormatted,
        categoriesCount = categoriesCount,
        activeCategoriesCount = activeCategoriesCount,
        hasAnyExpenses = hasAnyExpenses,
    )

    companion object {
        private const val DAYS_IN_WEEK = 7
        private const val MAX_BURN_PERCENTAGE = 1.5f

        private val CHART_PALETTE = listOf(
            Color(0xFF14B8A6),
            Color(0xFF3B82F6),
            Color(0xFF8B5CF6),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFFEC4899),
        )

        private fun chartColor(index: Int): Color =
            CHART_PALETTE[index % CHART_PALETTE.size]
    }
}

private sealed interface AnalyticsResult : DomainResult {
    data object SheetLoading : AnalyticsResult
    data object DismissSheet : AnalyticsResult
}

