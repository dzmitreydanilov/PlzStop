package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.features.expenses.domain.repository.PendingReceiptItemsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ClearPendingReceiptDataUseCase(
    private val repository: PendingReceiptItemsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke() = withContext(ioDispatcher) {
        repository.clearPendingData()
    }
}
