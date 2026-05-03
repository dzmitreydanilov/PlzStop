package com.please.stop.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.please.stop.app.core.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserProfileEntity)

    @Query("SELECT onboardingCompleted FROM user_profile WHERE id = 1")
    fun observeOnboardingCompleted(): Flow<Boolean?>

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
