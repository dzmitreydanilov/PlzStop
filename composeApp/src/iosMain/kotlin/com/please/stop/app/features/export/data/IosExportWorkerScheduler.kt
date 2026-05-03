package com.please.stop.app.features.export.data

import com.please.stop.app.features.export.domain.ExportWorkerScheduler

internal class IosExportWorkerScheduler(
    private val bridge: IosExportWorkerBridge,
) : ExportWorkerScheduler {

    override fun enqueue(
        exportId: Long,
        googleAccessToken: String,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
    ) {
        bridge.enqueueExport(
            exportId = exportId,
            googleAccessToken = googleAccessToken,
            tabLayout = tabLayout,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
        )
    }
}
