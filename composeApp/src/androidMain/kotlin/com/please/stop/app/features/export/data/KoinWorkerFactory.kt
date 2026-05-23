package com.please.stop.app.features.export.data

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.koin.java.KoinJavaComponent

class KoinWorkerFactory : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return when (workerClassName) {
            ExportWorker::class.java.name -> {
                ExportWorker(
                    context = appContext,
                    params = workerParameters,
                    exportWorkRunner = KoinJavaComponent.get(ExportWorkRunner::class.java),
                )
            }
            else -> null
        }
    }
}
