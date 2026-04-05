package com.please.stop.app.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String?,
    val dateEpochMillis: Long?,
    val currency: String?,
    val createdAtEpochMillis: Long,
)
