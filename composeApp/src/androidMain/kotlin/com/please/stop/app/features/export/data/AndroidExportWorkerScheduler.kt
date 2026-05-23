package com.please.stop.app.features.export.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.please.stop.app.features.export.domain.ExportWorkerScheduler

internal class AndroidExportWorkerScheduler(
    private val context: Context,
) : ExportWorkerScheduler {

    override fun enqueue(
        exportId: Long,
        googleAccessToken: String,
        tabLayout: String,
        startDateMillis: Long,
        endDateMillis: Long,
    ) {
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(
                workDataOf(
                    ExportWorker.KEY_EXPORT_ID to exportId,
                    ExportWorker.KEY_ACCESS_TOKEN to googleAccessToken,
                    ExportWorker.KEY_TAB_LAYOUT to tabLayout,
                    ExportWorker.KEY_START_DATE to startDateMillis,
                    ExportWorker.KEY_END_DATE to endDateMillis,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("export_sheets", ExistingWorkPolicy.KEEP, request)
    }
}
