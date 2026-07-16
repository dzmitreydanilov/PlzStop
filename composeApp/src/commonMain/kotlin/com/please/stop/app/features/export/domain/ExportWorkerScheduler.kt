package com.please.stop.app.features.export.domain

interface ExportWorkerScheduler {
    fun enqueue(
        exportId: Long,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
        spreadsheetTitle: String,
        folderName: String,
    )
}
