package com.please.stop.app.features.analytics.domain.model

data class DayExpensesData(
    val dayOfMonth: Int,
    val currencySymbol: String,
    val decimalPlaces: Int,
    val expenses: List<DayExpenseItem>,
)

data class DayExpenseItem(
    val id: Long,
    val title: String,
    val categoryIconKey: String,
    val subcategoryName: String? = null,
    val amountMinorUnits: Long,
    val dateEpochMillis: Long,
)
