package com.please.stop.app.features.export.domain.repository

import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import kotlinx.coroutines.flow.Flow

sealed interface ExportValidationResult {
    data class Enqueued(val expenseCount: Int) : ExportValidationResult
    data object NotificationPermissionDenied : ExportValidationResult
    data object NoExpenses : ExportValidationResult

    data object Failed : ExportValidationResult
}

interface ExportRepository {
    fun validateAndEnqueueExport(
        googleAccessToken: String,
        spreadSheetFormat: SpreadSheetFormat,
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<Result<ExportValidationResult>>
}
