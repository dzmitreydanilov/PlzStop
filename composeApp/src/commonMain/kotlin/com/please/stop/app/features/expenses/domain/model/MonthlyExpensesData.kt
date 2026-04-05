package com.please.stop.app.features.expenses.domain.model

data class CurrencyDisplayConfig(
    val symbol: String,
    val decimalPlaces: Int,
)

data class ExpenseListItem(
    val id: Long,
    val title: String,
    val categoryName: String,
    val categoryIconKey: String,
    val amountMinorUnits: Long,
    val dateEpochMillis: Long,
)

sealed interface MonthlyExpenseEntry {
    data class Single(val expense: ExpenseListItem) : MonthlyExpenseEntry
    data class ReceiptGroup(
        val receiptId: Long,
        val merchantName: String?,
        val expenses: List<ExpenseListItem>,
        val totalMinorUnits: Long,
    ) : MonthlyExpenseEntry
}

data class DayGroup(
    val dayEpochMillis: Long,
    val entries: List<MonthlyExpenseEntry>,
    val totalMinorUnits: Long,
)

data class MonthlyExpensesData(
    val year: Int,
    val month: Int,
    val dayGroups: List<DayGroup>,
    val totalMinorUnits: Long,
    val currency: CurrencyDisplayConfig,
)
