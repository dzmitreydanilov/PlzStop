package com.please.stop.app.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "export_history")
data class ExportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
    val status: ExportStatus,
    val spreadsheetUrl: String? = null,
    val errorMessage: String? = null,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)

enum class ExportStatus { PENDING, SUCCESS, FAILED }
