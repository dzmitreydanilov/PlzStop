package com.please.stop.app.features.export.presentation

import com.please.stop.app.features.auth.google.GoogleUser
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat

sealed interface ExportEvent {
    data class ExportTapped(val startDateMillis: Long, val endDateMillis: Long) : ExportEvent
    data class TabLayoutSelected(val spreadSheetFormat: SpreadSheetFormat) : ExportEvent
    data class DateRangeSelected(
        val startDateMillis: Long,
        val endDateMillis: Long,
    ) : ExportEvent
    data class ConfirmExport(val googleAccessToken: String) : ExportEvent
    data class GoogleAccountConnected(val googleUser: GoogleUser) : ExportEvent
    data object DismissError : ExportEvent
    data object Dismiss : ExportEvent
}
