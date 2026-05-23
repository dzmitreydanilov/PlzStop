package com.please.stop.app.features.analytics.presentation

import com.please.stop.app.core.models.domain.ErrorType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface AnalyticsState {

    val totalSpentFormatted: String?
    val categoriesCount: Int
    val activeCategoriesCount: Int
    val hasAnyExpenses: Boolean

    data object Loading : AnalyticsState {
        override val totalSpentFormatted: String? = null
        override val categoriesCount: Int = 0
        override val activeCategoriesCount: Int = 0
        override val hasAnyExpenses: Boolean = false
    }

    data class Content(
        override val totalSpentFormatted: String?,
        override val categoriesCount: Int,
        override val activeCategoriesCount: Int,
        override val hasAnyExpenses: Boolean,
        val currentYear: Int,
        val currentMonth: Int,
        val budgetBurn: BudgetBurnUi?,
        val projectedTotal: ProjectedTotalUi?,
        val budgetPacingPoints: ImmutableList<Float>,
        val spendingSlices: ImmutableList<SpendingSlice>,
        val dailySpending: ImmutableList<DailySpendingUiPoint>,
        val monthlyBars: ImmutableList<MonthlyBarUiItem>,
        val heatmapDays: ImmutableList<HeatmapDayUi>,
        val categoryProgress: ImmutableList<CategoryProgressUi>,
        val selectedDaySheet: DayExpensesSheetUi? = null,
        val isDaySheetLoading: Boolean = false,
    ) : AnalyticsState

    data class Error(
        val errorType: ErrorType,
        override val totalSpentFormatted: String?,
        override val categoriesCount: Int,
        override val activeCategoriesCount: Int,
        override val hasAnyExpenses: Boolean,
    ) : AnalyticsState
}

data class BudgetBurnUi(
    val spentFormatted: String,
    val budgetFormatted: String,
    val percentage: Float,
    val dailyAllowanceFormatted: String,
)

data class ProjectedTotalUi(
    val projectedFormatted: String,
    val overUnderFormatted: String,
    val isOverBudget: Boolean,
)

data class SpendingSlice(
    val name: String,
    val formattedAmount: String,
    val formattedAvgDaily: String,
    val amount: Float,
)

data class DailySpendingUiPoint(
    val dayOfMonth: Int,
    val amount: Float,
    val formattedAmount: String,
)

data class MonthlyBarUiItem(
    val label: String,
    val amount: Float,
    val formattedAmount: String,
    val isCurrent: Boolean,
)

data class HeatmapDayUi(
    val dayOfMonth: Int,
    val dayOfWeek: Int,
    val weekOfMonth: Int,
    val intensity: Float,
    val formattedAmount: String,
)

data class CategoryProgressUi(
    val name: String,
    val iconKey: String,
    val percentage: Float,
    val formattedAmount: String,
    val subcategories: ImmutableList<SubcategoryProgressUi> = persistentListOf(),
)

data class SubcategoryProgressUi(
    val name: String,
    val percentage: Float,
    val formattedAmount: String,
)

data class DayExpenseUiItem(
    val title: String,
    val categoryEmoji: String,
    val subcategoryName: String? = null,
    val formattedAmount: String,
)

data class DayExpensesSheetUi(
    val dayLabel: String,
    val totalFormatted: String,
    val expenses: ImmutableList<DayExpenseUiItem>,
)
