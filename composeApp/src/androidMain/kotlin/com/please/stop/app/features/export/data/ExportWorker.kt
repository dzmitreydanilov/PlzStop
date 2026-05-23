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
        val accessToken = inputData.getString(KEY_ACCESS_TOKEN) ?: return Result.failure()
        val startDate = inputData.getLong(KEY_START_DATE, 0)
        val endDate = inputData.getLong(KEY_END_DATE, 0)
        val exportId = inputData.getLong(KEY_EXPORT_ID, 0)
        val tabLayout = inputData.getString(KEY_TAB_LAYOUT) ?: "single_tab"

        val success = exportWorkRunner.run(
            exportId = exportId,
            googleAccessToken = accessToken,
            tabLayout = tabLayout,
            startDateMillis = startDate,
            endDateMillis = endDate,
        )
        return if (success) Result.success() else Result.failure()
    }

    companion object {
        const val KEY_ACCESS_TOKEN = "accessToken"
        const val KEY_START_DATE = "startDate"
        const val KEY_END_DATE = "endDate"
        const val KEY_EXPORT_ID = "exportId"
        const val KEY_TAB_LAYOUT = "tabLayout"
    }
}
