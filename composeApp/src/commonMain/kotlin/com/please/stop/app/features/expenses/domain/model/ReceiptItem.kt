package com.please.stop.app.features.expenses.domain.model

data class ReceiptItem(
    val name: String,
    val amountMinorUnits: Long,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
)
