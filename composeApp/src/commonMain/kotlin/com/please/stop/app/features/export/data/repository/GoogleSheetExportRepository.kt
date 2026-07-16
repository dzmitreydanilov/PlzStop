package com.please.stop.app.features.export.data.repository

import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.entity.ExportHistoryEntity
import com.please.stop.app.core.db.entity.ExportStatus
import com.please.stop.app.features.export.domain.ExportWorkerScheduler
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.utils.date.now
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class GoogleSheetExportRepository(
    private val exportHistoryDao: ExportHistoryDao,
    private val exportWorkerScheduler: ExportWorkerScheduler,
) {

    fun enqueExport(
        startDateMillis: Long,
        endDateMillis: Long,
        spreadSheetFormat: SpreadSheetFormat,
        spreadsheetTitle: String,
        folderName: String,
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
                tabLayout = spreadSheetFormat.name.lowercase(),
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                spreadsheetTitle = spreadsheetTitle,
                folderName = folderName,
            )

            emit(Result.success(Unit))
        }.catch {
            emit(Result.failure(it))
        }
    }
}
