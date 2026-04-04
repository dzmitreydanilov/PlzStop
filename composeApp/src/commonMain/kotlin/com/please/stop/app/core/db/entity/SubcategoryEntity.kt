package com.please.stop.app.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategory",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parentCategoryId")],
)
data class SubcategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentCategoryId: Long,
    val name: String,
    val iconKey: String,
    val isDefault: Boolean,
    val sortOrder: Int,
)
