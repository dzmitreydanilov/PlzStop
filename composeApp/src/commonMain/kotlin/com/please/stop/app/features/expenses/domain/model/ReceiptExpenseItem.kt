package com.please.stop.app.features.expenses.domain.model

data class ReceiptExpenseItem(
    val id: String,
    val name: String,
    val amountMinorUnits: Long,
    val categoryId: Long?,
    val subcategoryId: Long?,
)
