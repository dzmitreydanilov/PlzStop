package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.please.stop.app.core.models.domain.Result as DomainResult

class SaveExpenseUseCase(
    private val repository: AddExpenseRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        existingId: Long?,
        amountMinorUnits: Long,
        title: String,
        categoryId: Long,
        dateEpochMillis: Long,
        notes: String?,
        subcategoryId: Long? = null,
        originalAmountMinorUnits: Long? = null,
        originalCurrencyCode: String? = null,
        conversionRate: Double? = null,
    ): DomainResult = withContext(ioDispatcher) {
        val result = if (existingId != null) {
            repository.updateExpense(
                id = existingId,
                amountMinorUnits = amountMinorUnits,
                title = title,
                categoryId = categoryId,
                dateEpochMillis = dateEpochMillis,
                notes = notes,
                subcategoryId = subcategoryId,
                originalAmountMinorUnits = originalAmountMinorUnits,
                originalCurrencyCode = originalCurrencyCode,
                conversionRate = conversionRate,
            )
        } else {
            repository.saveExpense(
                amountMinorUnits = amountMinorUnits,
                title = title,
                categoryId = categoryId,
                dateEpochMillis = dateEpochMillis,
                notes = notes,
                subcategoryId = subcategoryId,
                originalAmountMinorUnits = originalAmountMinorUnits,
                originalCurrencyCode = originalCurrencyCode,
                conversionRate = conversionRate,
            ).map { }
        }
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
