package com.please.stop.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.please.stop.app.core.db.entity.ExportHistoryEntity
import com.please.stop.app.core.db.entity.ExportStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportHistoryDao {

    @Insert
    suspend fun insert(entity: ExportHistoryEntity): Long

    @Query(
        "UPDATE export_history SET status = :status, spreadsheetUrl = :url, " +
            "completedAtEpochMillis = :completedAt WHERE id = :id",
    )
    suspend fun updateResult(id: Long, status: ExportStatus, url: String?, completedAt: Long)

    @Query(
        "UPDATE export_history SET status = :status, errorMessage = :error, " +
            "completedAtEpochMillis = :completedAt WHERE id = :id",
    )
    suspend fun updateError(id: Long, status: ExportStatus, error: String?, completedAt: Long)

    @Query("SELECT * FROM export_history ORDER BY createdAtEpochMillis DESC LIMIT 1")
    fun observeLatest(): Flow<ExportHistoryEntity?>

    @Query("DELETE FROM export_history")
    suspend fun deleteAll()
}
