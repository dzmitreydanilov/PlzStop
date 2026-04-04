package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.expenses.data.remote.ReceiptAnalysisException
import com.please.stop.app.features.expenses.domain.model.ReceiptData
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import com.please.stop.app.features.expenses.domain.model.toErrorType
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import com.please.stop.app.features.expenses.domain.repository.ReceiptRepository
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
                val error = ReceiptError.SERVICE_UNAVAILABLE
                return@withContext Result.Failure(
                    errorType = error.toErrorType(),
                    receiptError = error,
                )
            }

            val result = receiptRepository.analyzeReceipt(
                imageBytes = imageBytes,
                categories = formData.categories,
                decimalPlaces = formData.decimalPlaces,
                subcategories = formData.subcategories,
            )

            result.fold(
                onSuccess = { Result.Success(it) },
                onFailure = {
                    val error = it.toReceiptError()
                    Result.Failure(
                        errorType = error.toErrorType(),
                        receiptError = error,
                    )
                },
            )
        }

    sealed interface Result : DomainResult {
        data class Success(val data: ReceiptData) : Result
        data class Failure(
            override val errorType: ErrorType,
            val receiptError: ReceiptError,
        ) : Result, ErrorResult
    }
}

private fun Throwable.toReceiptError(): ReceiptError = when (this) {
    is ReceiptAnalysisException.NotReceipt -> ReceiptError.NOT_RECEIPT
    is ReceiptAnalysisException.Unreadable -> ReceiptError.UNREADABLE
    is ReceiptAnalysisException.ServiceUnavailable -> ReceiptError.SERVICE_UNAVAILABLE
    is ApplicationException.NetworkException -> ReceiptError.NO_NETWORK
    else -> ReceiptError.SERVICE_UNAVAILABLE
}
