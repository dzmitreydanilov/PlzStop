package com.please.stop.app.features.expenses.domain.model

data class AddExpenseFormData(
    val currencyCode: String,
    val currencySymbol: String,
    val decimalPlaces: Int,
    val categories: List<ExpenseCategory>,
    val subcategories: List<ExpenseSubcategory> = emptyList(),
    val currencyConversionEnabled: Boolean = false,
)

data class ExpenseCategory(
    val id: Long,
    val name: String,
    val iconKey: String,
)

data class ExpenseSubcategory(
    val id: Long,
    val parentCategoryId: Long,
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
    val subcategoryId: Long? = null,
    val originalAmountMinorUnits: Long? = null,
    val originalCurrencyCode: String? = null,
    val conversionRate: Double? = null,
)
