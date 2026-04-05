package com.please.stop.app.features.expenses.domain.repository

import com.please.stop.app.features.expenses.domain.model.ReceiptExpenseItem

interface ReceiptSaveRepository {
    suspend fun saveReceiptWithExpenses(
        merchantName: String?,
        currency: String?,
        dateEpochMillis: Long?,
        items: List<ReceiptExpenseItem>,
        defaultCategoryId: Long,
        dateMillis: Long,
        conversionRate: Double?,
        originalCurrencyCode: String?,
    ): Result<Unit>
}
