package com.please.stop.app.features.analytics.domain.model

data class AnalyticsData(
    val currencySymbol: String,
    val decimalPlaces: Int,
    val totalSpentMinorUnits: Long,
    val monthlyBudgetMinorUnits: Long,
    val daysInMonth: Int,
    val currentDayOfMonth: Int,
    val categorySpending: List<CategorySpendingItem>,
    val dailySpending: List<DailySpendingPoint>,
    val monthlyTotals: List<MonthlyTotal>,
)

data class CategorySpendingItem(
    val categoryId: Long,
    val name: String,
    val iconKey: String,
    val spentMinorUnits: Long,
    val subcategorySpending: List<SubcategorySpendingItem> = emptyList(),
)

data class SubcategorySpendingItem(
    val subcategoryId: Long,
    val name: String,
    val spentMinorUnits: Long,
)

data class DailySpendingPoint(
    val dayOfMonth: Int,
    val totalMinorUnits: Long,
)

data class MonthlyTotal(
    val label: String,
    val totalMinorUnits: Long,
    val isCurrent: Boolean,
)
