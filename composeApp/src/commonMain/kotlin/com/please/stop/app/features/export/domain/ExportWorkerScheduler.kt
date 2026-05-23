package com.please.stop.app.features.export.domain

interface ExportWorkerScheduler {
    fun enqueue(
        exportId: Long,
        googleAccessToken: String,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
    )
}
