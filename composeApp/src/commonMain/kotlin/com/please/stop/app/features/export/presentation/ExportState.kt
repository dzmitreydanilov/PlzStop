package com.please.stop.app.features.export.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.utils.date.nowMillis

@Stable
sealed interface ExportState {

    val currentSpreadSheetFormat: SpreadSheetFormat
    var currentStartDateMillis: Long
    var currentEndDateMillis: Long

    data class Idle(
        override val currentSpreadSheetFormat: SpreadSheetFormat = SpreadSheetFormat.SINGLE_TAB,
        override var currentStartDateMillis: Long = nowMillis(),
        override var currentEndDateMillis: Long = nowMillis()
    ) : ExportState

    data class Confirm(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override var currentStartDateMillis: Long,
        override var currentEndDateMillis: Long
    ) : ExportState

    data class Enqueued(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override var currentStartDateMillis: Long,
        override var currentEndDateMillis: Long

    ) : ExportState

    data class NeedsGoogleAccount(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override var currentStartDateMillis: Long,
        override var currentEndDateMillis: Long
    ) : ExportState

    data class NeedsNotificationPermission(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override var currentStartDateMillis: Long,
        override var currentEndDateMillis: Long
    ) : ExportState

    data class NoExpenses(
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override var currentStartDateMillis: Long,
        override var currentEndDateMillis: Long
    ) : ExportState

    data class Error(
        val errorType: ErrorType,
        override val currentSpreadSheetFormat: SpreadSheetFormat,
        override var currentStartDateMillis: Long,
        override var currentEndDateMillis: Long
    ) : ExportState
}
