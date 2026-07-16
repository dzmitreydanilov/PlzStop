package com.please.stop.app.features.export.data

import com.please.stop.app.core.coroutines.ICoroutineScopeProvider
import com.please.stop.app.features.export.domain.ExportWorkerScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class IosExportWorkerScheduler(
    private val exportWorkRunner: ExportWorkRunner,
    private val scopeProvider: ICoroutineScopeProvider,
) : ExportWorkerScheduler {

    override fun enqueue(
        exportId: Long,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
        spreadsheetTitle: String,
        folderName: String,
    ) {
        scopeProvider.getScope().launch {
            val request = ExportWorkRequest(
                exportId = exportId,
                tabLayout = tabLayout,
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                spreadsheetTitle = spreadsheetTitle,
                folderName = folderName,
            )
            repeat(MAX_ATTEMPTS) { attempt ->
                when (exportWorkRunner.run(request)) {
                    ExportWorkResult.Success,
                    ExportWorkResult.Failure -> return@launch
                    ExportWorkResult.Retry -> {
                        if (attempt < MAX_ATTEMPTS - 1) {
                            delay(INITIAL_RETRY_DELAY_MILLIS shl attempt)
                        }
                    }
                }
            }
            exportWorkRunner.markRetryExhausted(exportId = request.exportId)
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val INITIAL_RETRY_DELAY_MILLIS = 1_000L
    }
}
