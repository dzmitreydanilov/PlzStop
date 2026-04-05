package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.expenses.domain.model.ReceiptExpenseItem
import com.please.stop.app.features.expenses.domain.repository.ReceiptSaveRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class SaveReceiptExpensesUseCase(
    private val repository: ReceiptSaveRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        merchantName: String?,
        currency: String?,
        dateEpochMillis: Long?,
        items: List<ReceiptExpenseItem>,
        defaultCategoryId: Long,
        dateMillis: Long,
        conversionRate: Double?,
        originalCurrencyCode: String?,
    ): DomainResult = withContext(ioDispatcher) {
        val result = repository.saveReceiptWithExpenses(
            merchantName = merchantName,
            currency = currency,
            dateEpochMillis = dateEpochMillis,
            items = items,
            defaultCategoryId = defaultCategoryId,
            dateMillis = dateMillis,
            conversionRate = conversionRate,
            originalCurrencyCode = originalCurrencyCode,
        )
        result.fold(
            onSuccess = { Result.Success },
            onFailure = { Result.Failure(it.toErrorType()) },
        )
    }

    sealed interface Result : DomainResult {
        data object Success : Result
        data class Failure(override val errorType: ErrorType) : Result, ErrorResult
    }
}
