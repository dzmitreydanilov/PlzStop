package com.please.stop.app.features.export.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

internal class ExportWorker(
    context: Context,
    params: WorkerParameters,
    private val exportWorkRunner: ExportWorkRunner,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val request = ExportWorkRequest(
            exportId = inputData.getLong(ExportWorkRequest.KEY_EXPORT_ID, 0),
            tabLayout = inputData.getString(ExportWorkRequest.KEY_TAB_LAYOUT) ?: "single_tab",
            startDateMillis = inputData.getLong(ExportWorkRequest.KEY_START_DATE, 0),
            endDateMillis = inputData.getLong(ExportWorkRequest.KEY_END_DATE, 0),
            spreadsheetTitle = inputData.getString(ExportWorkRequest.KEY_SPREADSHEET_TITLE).orEmpty(),
            folderName = inputData.getString(ExportWorkRequest.KEY_FOLDER_NAME).orEmpty(),
        )

        return when (exportWorkRunner.run(request)) {
            ExportWorkResult.Success -> Result.success()
            ExportWorkResult.Failure -> Result.failure()
            ExportWorkResult.Retry -> {
                if (runAttemptCount < MAX_ATTEMPTS - 1) {
                    Result.retry()
                } else {
                    exportWorkRunner.markRetryExhausted(exportId = request.exportId)
                    Result.failure()
                }
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
