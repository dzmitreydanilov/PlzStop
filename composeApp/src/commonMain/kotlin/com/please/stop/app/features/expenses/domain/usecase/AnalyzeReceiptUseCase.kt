package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.features.expenses.data.remote.ReceiptAnalysisException
import com.please.stop.app.features.expenses.domain.model.ReceiptData
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import com.please.stop.app.features.expenses.domain.repository.ReceiptRepository
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import com.please.stop.app.network.ApplicationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class AnalyzeReceiptUseCase(
    private val receiptRepository: ReceiptRepository,
    private val addExpenseRepository: AddExpenseRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(imageBytes: ByteArray): DomainResult =
        withContext(ioDispatcher) {
            val formData = addExpenseRepository.getFormData().getOrElse {
                return@withContext Result.Failure(ReceiptError.SERVICE_UNAVAILABLE)
            }

            val result = receiptRepository.analyzeReceipt(
                imageBytes = imageBytes,
                categories = formData.categories,
                decimalPlaces = formData.decimalPlaces,
            )

            result.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failure(it.toReceiptError()) },
            )
        }

    sealed interface Result : DomainResult {
        data class Success(val data: ReceiptData) : Result
        data class Failure(val receiptError: ReceiptError) : Result
    }
}

private fun Throwable.toReceiptError(): ReceiptError = when (this) {
    is ReceiptAnalysisException.Unreadable -> ReceiptError.UNREADABLE
    is ReceiptAnalysisException.ServiceUnavailable -> ReceiptError.SERVICE_UNAVAILABLE
    is ApplicationException.NetworkException -> ReceiptError.NO_NETWORK
    else -> ReceiptError.SERVICE_UNAVAILABLE
}
