package com.please.stop.app.features.expenses.domain.repository

import com.please.stop.app.features.expenses.domain.model.ExpenseCategory
import com.please.stop.app.features.expenses.domain.model.ExpenseSubcategory
import com.please.stop.app.features.expenses.domain.model.ReceiptData

interface ReceiptRepository {
    suspend fun analyzeReceipt(
        imageBytes: ByteArray,
        categories: List<ExpenseCategory>,
        decimalPlaces: Int,
        subcategories: List<ExpenseSubcategory> = emptyList(),
    ): Result<ReceiptData>
}
