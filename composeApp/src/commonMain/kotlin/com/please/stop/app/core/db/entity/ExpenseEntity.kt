package com.please.stop.app.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index("dateEpochMillis"), Index("subcategoryId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinorUnits: Long,
    val title: String,
    val categoryId: Long,
    val dateEpochMillis: Long,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val isDeleted: Boolean = false,
    val subcategoryId: Long? = null,
    val originalAmountMinorUnits: Long? = null,
    val originalCurrencyCode: String? = null,
    val conversionRate: Double? = null,
)
