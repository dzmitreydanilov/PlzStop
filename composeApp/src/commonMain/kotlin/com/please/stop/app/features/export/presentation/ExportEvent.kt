package com.please.stop.app.features.export.presentation

import com.please.stop.app.features.auth.google.GoogleSheetsAuthorizationCode
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat

sealed interface ExportEvent {
    data class StartExport(val startDateMillis: Long, val endDateMillis: Long) : ExportEvent
    data class DestinationSelected(val destination: ExportDestination) : ExportEvent
    data class TabLayoutSelected(val spreadSheetFormat: SpreadSheetFormat) : ExportEvent
    data class DateRangeSelected(val startDateMillis: Long, val endDateMillis: Long) : ExportEvent
    data class FileNameEntered(val fileName: String) : ExportEvent
    data class FolderNameEntered(val folderName: String) : ExportEvent
    data class GoogleAccountConnected(val authorizationCode: GoogleSheetsAuthorizationCode) : ExportEvent
    data object AuthenticationCompleted : ExportEvent
    data object DismissAuthentication : ExportEvent
    data object DismissError : ExportEvent
    data object Dismiss : ExportEvent
}
