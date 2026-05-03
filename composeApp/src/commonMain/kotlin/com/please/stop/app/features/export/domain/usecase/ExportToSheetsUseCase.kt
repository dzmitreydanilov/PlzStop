package com.please.stop.app.features.export.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.features.export.domain.repository.ExportRepository
import com.please.stop.app.features.export.domain.repository.ExportValidationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ExportToSheetsUseCase(
    private val repository: ExportRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(
        googleAccessToken: String,
        spreadSheetFormat: SpreadSheetFormat,
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<ExportResult> {
        return repository.validateAndEnqueueExport(
            googleAccessToken = googleAccessToken,
            spreadSheetFormat = spreadSheetFormat,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
        ).map {
            it.fold(
                onSuccess = { it.toResult() },
                onFailure = { ExportResult.Failure(it.toErrorType()) }
            )
        }.flowOn(dispatcher)
    }
}

private fun ExportValidationResult.toResult(): ExportResult = when (this) {
    is ExportValidationResult.Enqueued -> ExportResult.Enqueued(expenseCount)

    ExportValidationResult.NotificationPermissionDenied -> ExportResult.NotificationPermissionDenied

    ExportValidationResult.NoExpenses -> ExportResult.NoExpenses

    ExportValidationResult.Failed -> ExportResult.Failure(ErrorType.Unknown("Something went wrong"))
}

sealed interface ExportResult : Result {
    data class Enqueued(val expenseCount: Int) : ExportResult
    data object GoogleAccountNotLinked : ExportResult
    data object NotificationPermissionDenied : ExportResult
    data object NoExpenses : ExportResult
    data class Failure(override val errorType: ErrorType) : ExportResult, ErrorResult
}
