package com.please.stop.app.features.export.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.please.stop.app.features.export.domain.ExportWorkerScheduler
import java.util.concurrent.TimeUnit

internal class AndroidExportWorkerScheduler(
    private val context: Context,
) : ExportWorkerScheduler {

    override fun enqueue(
        exportId: Long,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
        spreadsheetTitle: String,
        folderName: String,
    ) {
        val input = ExportWorkRequest(
            exportId = exportId,
            tabLayout = tabLayout,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            spreadsheetTitle = spreadsheetTitle,
            folderName = folderName,
        )
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(
                workDataOf(
                    ExportWorkRequest.KEY_EXPORT_ID to input.exportId,
                    ExportWorkRequest.KEY_TAB_LAYOUT to input.tabLayout,
                    ExportWorkRequest.KEY_START_DATE to input.startDateMillis,
                    ExportWorkRequest.KEY_END_DATE to input.endDateMillis,
                    ExportWorkRequest.KEY_SPREADSHEET_TITLE to input.spreadsheetTitle,
                    ExportWorkRequest.KEY_FOLDER_NAME to input.folderName,
                ),
            )
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelay = MIN_BACKOFF_SECONDS,
                timeUnit = TimeUnit.SECONDS,
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workerName, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val workerName = "export_sheets"
        const val MIN_BACKOFF_SECONDS = 30L
    }
}
