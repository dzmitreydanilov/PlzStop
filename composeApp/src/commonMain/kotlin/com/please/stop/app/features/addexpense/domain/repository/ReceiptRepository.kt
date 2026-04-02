package com.please.stop.app.features.addexpense.domain.repository

import com.please.stop.app.features.addexpense.domain.model.ExpenseCategory
import com.please.stop.app.features.addexpense.domain.model.ReceiptData

interface ReceiptRepository {
    suspend fun analyzeReceipt(
        imageBytes: ByteArray,
        categories: List<ExpenseCategory>,
        decimalPlaces: Int,
    ): Result<ReceiptData>
}
