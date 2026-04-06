package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.features.expenses.domain.model.PendingReceiptData
import com.please.stop.app.features.expenses.domain.repository.PendingReceiptItemsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SetPendingReceiptDataUseCase(
    private val repository: PendingReceiptItemsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(data: PendingReceiptData) = withContext(ioDispatcher) {
        repository.setPendingData(data)
    }
}
