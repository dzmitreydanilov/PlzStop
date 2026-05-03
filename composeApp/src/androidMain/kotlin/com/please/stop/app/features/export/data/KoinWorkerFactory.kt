package com.please.stop.app.features.export.data

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.please.stop.app.core.IFcmTokenProvider
import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions
import org.koin.java.KoinJavaComponent

class KoinWorkerFactory : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return when (workerClassName) {
            ExportWorker::class.java.name -> {
                val db = KoinJavaComponent.get<AppDatabase>(AppDatabase::class.java)
                ExportWorker(
                    context = appContext,
                    params = workerParameters,
                    expenseDao = db.expenseDao(),
                    categoryDao = db.categoryDao(),
                    subcategoryDao = db.subcategoryDao(),
                    userProfileDao = db.userProfileDao(),
                    callableFunctions = KoinJavaComponent.get(FirebaseCallableFunctions::class.java),
                    fcmTokenProvider = KoinJavaComponent.get(IFcmTokenProvider::class.java),
                    exportHistoryDao = db.exportHistoryDao(),
                )
            }
            else -> null
        }
    }
}
