package com.please.stop.app.features.export.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.export.data.repository.CSVExportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ExportCsvUseCase(
    private val repository: CSVExportRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<ExportResult> {
        return repository.enqueExport(
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
        ).map {
            it.fold(
                onSuccess = { ExportResult.CsvShareLaunched(expenseCount = 0) },
                onFailure = { ExportResult.Failure(it.toErrorType()) },
            )
        }.flowOn(dispatcher)
    }
}

sealed interface ExportResult : Result {
    data class Enqueued(val expenseCount: Int) : ExportResult
    data class CsvShareLaunched(val expenseCount: Int) : ExportResult
    data object GoogleAccountNotLinked : ExportResult
    data object NoExpenses : ExportResult
    data class Failure(override val errorType: ErrorType) : ExportResult, ErrorResult
}
