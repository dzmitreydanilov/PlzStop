package com.please.stop.app.features.addexpense.domain.model

data class AddExpenseFormData(
    val currencySymbol: String,
    val decimalPlaces: Int,
    val categories: List<ExpenseCategory>,
)

data class ExpenseCategory(
    val id: Long,
    val name: String,
    val iconKey: String,
)

data class ExpenseDetail(
    val id: Long,
    val amountMinorUnits: Long,
    val title: String,
    val categoryId: Long,
    val dateEpochMillis: Long,
    val notes: String?,
)
