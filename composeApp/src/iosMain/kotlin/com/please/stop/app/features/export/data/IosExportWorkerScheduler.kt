package com.please.stop.app.features.export.data

import com.please.stop.app.core.coroutines.ICoroutineScopeProvider
import com.please.stop.app.features.export.domain.ExportWorkerScheduler
import kotlinx.coroutines.launch

internal class IosExportWorkerScheduler(
    private val exportWorkRunner: ExportWorkRunner,
    private val scopeProvider: ICoroutineScopeProvider,
) : ExportWorkerScheduler {

    override fun enqueue(
        exportId: Long,
        googleAccessToken: String,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
    ) {
        scopeProvider.getScope().launch {
            exportWorkRunner.run(
                exportId = exportId,
                googleAccessToken = googleAccessToken,
                tabLayout = tabLayout,
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
            )
        }
    }
}
