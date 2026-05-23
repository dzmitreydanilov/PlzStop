package com.please.stop.app.features.export.data.repository

import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.entity.ExportHistoryEntity
import com.please.stop.app.core.db.entity.ExportStatus
import com.please.stop.app.features.export.domain.ExportWorkerScheduler
import com.please.stop.app.features.export.domain.repository.ExportRepository
import com.please.stop.app.utils.date.now
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class GoogleSheetExportRepository(
    private val exportHistoryDao: ExportHistoryDao,
    private val exportWorkerScheduler: ExportWorkerScheduler,
) : ExportRepository {

    override fun enqueExport(
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<Result<Unit>> {
        return flow {
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
                googleAccessToken = "",
                tabLayout = "",
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
            )

            emit(Result.success(Unit))
        }.catch {
            emit(Result.failure(it))
        }
    }
}
