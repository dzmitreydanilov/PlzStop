package com.please.stop.app.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String?,
    val currencyCode: String,
    val monthlyBudget: Long,
    val onboardingCompleted: Boolean,
)
