package com.please.stop.app.features.export.domain.usecase

import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.flow.flowFromSuspend
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class HasExpensesToExportUseCase(
    private val expenseDao: ExpenseDao
) {

    operator fun invoke(): Flow<ExportExpensesAvailabilityResult> {
        return flowFromSuspend { expenseDao.hasActiveExpenses() }
            .map { hasExpenses ->
                if (hasExpenses) {
                    ExportExpensesAvailabilityResult.Available
                } else {
                    ExportExpensesAvailabilityResult.Empty
                }
            }
            .catch { e -> ExportExpensesAvailabilityResult.Failure(errorType = e.toErrorType()) }
    }
}

sealed interface ExportExpensesAvailabilityResult : com.please.stop.app.core.models.domain.Result {
    data object Available : ExportExpensesAvailabilityResult
    data object Empty : ExportExpensesAvailabilityResult
    data class Failure(override val errorType: ErrorType) : ExportExpensesAvailabilityResult, ErrorResult
}
