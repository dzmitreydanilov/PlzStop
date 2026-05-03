package com.please.stop.app.features.export.data.repository

import com.please.stop.app.core.INotificationPermission
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.entity.ExportHistoryEntity
import com.please.stop.app.core.db.entity.ExportStatus
import com.please.stop.app.features.export.domain.ExportWorkerScheduler
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.features.export.domain.repository.ExportRepository
import com.please.stop.app.features.export.domain.repository.ExportValidationResult
import com.please.stop.app.utils.date.now
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

internal class ExportRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val exportHistoryDao: ExportHistoryDao,
    private val notificationPermission: INotificationPermission,
    private val exportWorkerScheduler: ExportWorkerScheduler,
) : ExportRepository {

    override fun validateAndEnqueueExport(
        googleAccessToken: String,
        spreadSheetFormat: SpreadSheetFormat,
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<Result<ExportValidationResult>> {
        return flow {
            if (!notificationPermission.isGranted()) {
                val granted = notificationPermission.request()
                if (!granted) {
                    emit(Result.success(ExportValidationResult.NotificationPermissionDenied))
                    return@flow
                }
            }

            val expenses = expenseDao.getExpensesInRange(startDateMillis, endDateMillis)
            if (expenses.isEmpty()) {
                emit(Result.success(ExportValidationResult.NoExpenses))
                return@flow
            }

            val exportId = exportHistoryDao.insert(
                ExportHistoryEntity(
                    startDateEpochMillis = startDateMillis,
                    endDateEpochMillis = endDateMillis,
                    status = ExportStatus.PENDING,
                    createdAtEpochMillis = now().toEpochMilliseconds(),
                ),
            )

            exportWorkerScheduler.enqueue(
                exportId = exportId,
                googleAccessToken = googleAccessToken,
                tabLayout = spreadSheetFormat.name.lowercase(),
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
            )

            emit(Result.success(ExportValidationResult.Enqueued(expenseCount = expenses.size)))
        }.catch {
            Result.failure<Exception>(it)
        }
    }
}
