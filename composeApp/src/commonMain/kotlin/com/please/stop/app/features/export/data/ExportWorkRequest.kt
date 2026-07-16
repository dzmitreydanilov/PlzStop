package com.please.stop.app.features.export.data

/** Non-secret configuration persisted for a Google Sheets export. */
internal data class ExportWorkRequest(
    val exportId: Long,
    val tabLayout: String,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val spreadsheetTitle: String,
    val folderName: String,
) {
    fun toPersistedValues(): Map<String, Any> = mapOf(
        KEY_EXPORT_ID to exportId,
        KEY_TAB_LAYOUT to tabLayout,
        KEY_START_DATE to startDateMillis,
        KEY_END_DATE to endDateMillis,
        KEY_SPREADSHEET_TITLE to spreadsheetTitle,
        KEY_FOLDER_NAME to folderName,
    )

    companion object {
        const val KEY_START_DATE = "startDate"
        const val KEY_END_DATE = "endDate"
        const val KEY_EXPORT_ID = "exportId"
        const val KEY_TAB_LAYOUT = "tabLayout"
        const val KEY_SPREADSHEET_TITLE = "spreadsheetTitle"
        const val KEY_FOLDER_NAME = "folderName"
    }
}
