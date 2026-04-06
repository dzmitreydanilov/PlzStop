package com.please.stop.app.features.expenses.domain.repository

import com.please.stop.app.features.expenses.domain.model.PendingReceiptData

interface PendingReceiptItemsRepository {
    fun setPendingData(data: PendingReceiptData)
    fun consumePendingData(): PendingReceiptData?
    fun clearPendingData()
}
