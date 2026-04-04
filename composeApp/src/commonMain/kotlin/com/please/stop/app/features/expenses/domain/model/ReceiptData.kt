package com.please.stop.app.features.expenses.domain.model

data class ReceiptData(
    val merchantName: String?,
    val totalAmountMinorUnits: Long?,
    val currency: String?,
    val date: String?,
    val categoryId: Long?,
    val subcategoryId: Long? = null,
    val isPartial: Boolean,
    val message: String?,
)
