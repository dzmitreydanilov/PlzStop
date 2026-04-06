package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.features.expenses.domain.model.PendingReceiptData
import com.please.stop.app.features.expenses.domain.repository.PendingReceiptItemsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class ConsumePendingReceiptDataUseCase(
    private val repository: PendingReceiptItemsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(): Result = withContext(ioDispatcher) {
        val data = repository.consumePendingData()
        if (data != null) Result.Success(data) else Result.NoPendingData
    }

    sealed interface Result : DomainResult {
        data class Success(val data: PendingReceiptData) : Result
        data object NoPendingData : Result
    }
}
