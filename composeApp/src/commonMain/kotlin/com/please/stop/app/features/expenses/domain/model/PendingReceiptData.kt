package com.please.stop.app.features.expenses.domain.model

data class PendingReceiptData(
    val items: List<ReceiptExpenseItem>,
    val merchantName: String?,
    val currency: String?,
    val dateString: String?,
    val categoryId: Long?,
    val subcategoryId: Long?,
    val isManualEntry: Boolean,
)
