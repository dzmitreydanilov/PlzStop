package com.please.stop.app.features.export.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.utils.date.nowMillis

@Stable
sealed interface ExportState {

    val currentSpreadSheetFormat: SpreadSheetFormat
    val currentDestination: ExportDestination
    val currentStartDateMillis: Long
    val currentEndDateMillis: Long
    val fileName: String?
    val hasExpensesToExport: Boolean

    data class Idle(
        override val currentSpreadSheetFormat: SpreadSheetFormat = SpreadSheetFormat.SINGLE_TAB,
        override val currentDestination: ExportDestination = ExportDestination.GOOGLE_SHEETS,
        override val currentStartDateMillis: Long = nowMillis(),
        override val currentEndDateMillis: Long = nowMillis(),
        override val fileName: String? = null,
        override val hasExpensesToExport: Boolean = true
    ) : ExportState

    data class Confirm(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override val currentDestination: ExportDestination,
        override val currentStartDateMillis: Long,
        override val currentEndDateMillis: Long,
        override val fileName: String?,
        override val hasExpensesToExport: Boolean
    ) : ExportState

    data class Enqueued(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override val currentDestination: ExportDestination,
        override val currentStartDateMillis: Long,
        override val currentEndDateMillis: Long,
        override val fileName: String?,
        override val hasExpensesToExport: Boolean
    ) : ExportState

    data class CsvShareLaunched(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override val currentDestination: ExportDestination,
        override val currentStartDateMillis: Long,
        override val currentEndDateMillis: Long,
        override val fileName: String?,
        override val hasExpensesToExport: Boolean
    ) : ExportState

    data class NeedsGoogleAccount(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override val currentDestination: ExportDestination,
        override val currentStartDateMillis: Long,
        override val currentEndDateMillis: Long,
        override val fileName: String?,
        override val hasExpensesToExport: Boolean
    ) : ExportState

    data class Error(
        val errorType: ErrorType,
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override val currentDestination: ExportDestination,
        override val currentStartDateMillis: Long,
        override val currentEndDateMillis: Long,
        override val fileName: String?,
        override val hasExpensesToExport: Boolean
    ) : ExportState
}
