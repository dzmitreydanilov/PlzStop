package com.please.stop.app.features.analytics.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.analytics.domain.model.AnalyticsData
import com.please.stop.app.features.analytics.domain.model.DailySpendingPoint
import com.please.stop.app.features.analytics.domain.model.DayExpensesData
import com.please.stop.app.features.analytics.domain.usecase.LoadDayExpensesUseCase
import com.please.stop.app.features.analytics.domain.usecase.ObserveAnalyticsDataUseCase
import com.please.stop.app.utils.date.DAYS_IN_WEEK
import com.please.stop.app.utils.date.formatDayLabel
import com.please.stop.app.utils.date.localDateToday
import com.please.stop.app.utils.formatCurrencyAmount
import com.please.stop.app.utils.minorUnitsToFloat
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import com.please.stop.app.core.models.domain.Result as DomainResult

class AnalyticsStateHolder(
    private val observeAnalyticsDataUseCase: ObserveAnalyticsDataUseCase,
    private val loadDayExpensesUseCase: LoadDayExpensesUseCase,
) : StateHolder<AnalyticsState, AnalyticsEvent>() {

    override val tag = "AnalyticsStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    override fun getInitial(): AnalyticsState = AnalyticsState.Loading

    override fun collectWhileSubscribed(): Flow<DomainResult> = observeAnalyticsDataUseCase()

    override fun resolveEventResult(event: AnalyticsEvent): Flow<DomainResult> = when (event) {
        is AnalyticsEvent.DayTapped -> handleDayTapped(event.dayOfMonth)
        AnalyticsEvent.DismissDaySheet -> flowOf(AnalyticsResult.DismissSheet)
        AnalyticsEvent.DismissError -> flowOf(AnalyticsResult.DismissError)
    }

    private fun handleDayTapped(dayOfMonth: Int): Flow<DomainResult> = flow {
        val content = state.value as? AnalyticsState.Content ?: return@flow
        emit(AnalyticsResult.SheetLoading)
        emit(loadDayExpensesUseCase(content.currentYear, content.currentMonth, dayOfMonth))
    }

    override fun getStateByResult(previous: AnalyticsState, result: DomainResult): AnalyticsState {
        return when (result) {
            is ObserveAnalyticsDataUseCase.Result.Success -> result.data.toContent(previous)
            is AnalyticsResult.DismissError -> AnalyticsState.Loading
            is AnalyticsResult.SheetLoading -> previous.updateContent { copy(isDaySheetLoading = true) }
            is AnalyticsResult.DismissSheet -> previous.updateContent {
                copy(selectedDaySheet = null, isDaySheetLoading = false)
            }
            is LoadDayExpensesUseCase.Result.Success -> previous.updateContent {
                copy(
                    selectedDaySheet = result.data.toSheetUi(currentYear, currentMonth),
                    isDaySheetLoading = false,
                )
            }
            is LoadDayExpensesUseCase.Result.Failure -> previous.updateContent {
                copy(isDaySheetLoading = false)
            }
            else -> super.getStateByResult(previous, result)
        }
    }

    private inline fun AnalyticsState.updateContent(
        transform: AnalyticsState.Content.() -> AnalyticsState.Content,
    ): AnalyticsState = when (this) {
        is AnalyticsState.Content -> transform()
        is AnalyticsState.Loading,
        is AnalyticsState.Error,
        -> this
    }

    override fun getErrorStateByResult(result: DomainResult, errorType: ErrorType): AnalyticsState {
        return when (result) {
            is ObserveAnalyticsDataUseCase.Result.Failure -> state.value.toError(result.errorType)
            else -> state.value.toError(errorType)
        }
    }

    private fun AnalyticsData.toContent(previous: AnalyticsState): AnalyticsState.Content {
        val today = localDateToday()

        val activeCategories = categorySpending.filter { it.spentMinorUnits > 0 }
        val hasBudget = monthlyBudgetMinorUnits > 0

        val budgetBurn = if (hasBudget) buildBudgetBurn() else null
        val projectedTotal = if (hasBudget && currentDayOfMonth > 0) buildProjectedTotal() else null
        val budgetPacingPoints = if (hasBudget) buildBudgetPacingPoints() else emptyList()

        val spendingSlices = activeCategories.mapIndexed { _, item ->
            val avgDaily = if (currentDayOfMonth > 0) item.spentMinorUnits / currentDayOfMonth else 0L
            SpendingSlice(
                name = item.name,
                formattedAmount = formatCurrencyAmount(item.spentMinorUnits, currencySymbol, decimalPlaces),
                formattedAvgDaily = formatCurrencyAmount(avgDaily, currencySymbol, decimalPlaces),
                amount = minorUnitsToFloat(item.spentMinorUnits, decimalPlaces),
            )
        }.toImmutableList()

        val dailyUiPoints = dailySpending.map { point ->
            DailySpendingUiPoint(
                dayOfMonth = point.dayOfMonth,
                amount = minorUnitsToFloat(point.totalMinorUnits, decimalPlaces),
                formattedAmount = formatCurrencyAmount(point.totalMinorUnits, currencySymbol, decimalPlaces),
            )
        }.toImmutableList()

        val monthlyBarItems = monthlyTotals.map { month ->
            MonthlyBarUiItem(
                label = month.label,
                amount = minorUnitsToFloat(month.totalMinorUnits, decimalPlaces),
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
            currentYear = today.year,
            currentMonth = today.month.number,
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

    private fun AnalyticsData.buildBudgetPacingPoints(): List<Float> {
        val budgetFloat = minorUnitsToFloat(monthlyBudgetMinorUnits, decimalPlaces)
        val dailyBudget = budgetFloat / daysInMonth
        return (1..daysInMonth).map { day -> dailyBudget * day }
    }

    private fun AnalyticsData.buildHeatmapDays(
        dailyPoints: List<DailySpendingPoint>,
    ): List<HeatmapDayUi> {
        val today = localDateToday()
        val firstDayOfMonth = LocalDate(today.year, today.month.number, 1)
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
        return activeCategories.mapIndexed { _, item ->
            val categoryTotal = item.spentMinorUnits.coerceAtLeast(1L)
            CategoryProgressUi(
                name = item.name,
                iconKey = item.iconKey,
                percentage = item.spentMinorUnits.toFloat() / total,
                formattedAmount = formatCurrencyAmount(item.spentMinorUnits, currencySymbol, decimalPlaces),
                subcategories = item.subcategorySpending.map { sub ->
                    SubcategoryProgressUi(
                        name = sub.name,
                        percentage = sub.spentMinorUnits.toFloat() / categoryTotal,
                        formattedAmount = formatCurrencyAmount(sub.spentMinorUnits, currencySymbol, decimalPlaces),
                    )
                }.toImmutableList(),
            )
        }
    }

    private fun DayExpensesData.toSheetUi(year: Int, month: Int): DayExpensesSheetUi {
        val tz = TimeZone.currentSystemDefault()
        val dayStartMillis = LocalDate(year, month, dayOfMonth)
            .atStartOfDayIn(tz)
            .toEpochMilliseconds()
        val total = expenses.sumOf { it.amountMinorUnits }

        return DayExpensesSheetUi(
            dayLabel = formatDayLabel(dayStartMillis),
            totalFormatted = formatCurrencyAmount(total, currencySymbol, decimalPlaces),
            expenses = expenses.map { item ->
                DayExpenseUiItem(
                    title = item.title,
                    categoryIconKey = item.categoryIconKey,
                    subcategoryName = item.subcategoryName,
                    formattedAmount = formatCurrencyAmount(item.amountMinorUnits, currencySymbol, decimalPlaces),
                )
            }.toImmutableList(),
        )
    }

    private fun AnalyticsState.toError(errorType: ErrorType): AnalyticsState.Error = AnalyticsState.Error(
        errorType = errorType,
        totalSpentFormatted = totalSpentFormatted,
        categoriesCount = categoriesCount,
        activeCategoriesCount = activeCategoriesCount,
        hasAnyExpenses = hasAnyExpenses,
    )

    companion object {
        private const val MAX_BURN_PERCENTAGE = 1.5f
    }
}

private sealed interface AnalyticsResult : DomainResult {
    data object SheetLoading : AnalyticsResult
    data object DismissSheet : AnalyticsResult
    data object DismissError : AnalyticsResult
}
