package com.please.stop.app.features.export.data

import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("IosExportWorkerBridge", exact = true)
interface IosExportWorkerBridge {
    fun enqueueExport(
        exportId: Long,
        googleAccessToken: String,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
    )
}
