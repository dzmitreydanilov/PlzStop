package com.please.stop.app.features.expenses.data.repository

import com.please.stop.app.features.expenses.domain.model.PendingReceiptData
import com.please.stop.app.features.expenses.domain.repository.PendingReceiptItemsRepository

class PendingReceiptItemsRepositoryImpl : PendingReceiptItemsRepository {

    private var pendingData: PendingReceiptData? = null

    override fun setPendingData(data: PendingReceiptData) {
        pendingData = data
    }

    override fun consumePendingData(): PendingReceiptData? {
        val result = pendingData
        pendingData = null
        return result
    }

    override fun clearPendingData() {
        pendingData = null
    }
}
