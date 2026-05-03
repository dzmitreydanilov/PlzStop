package com.please.stop.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.please.stop.app.core.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE isArchived = 1 ORDER BY name ASC")
    fun observeArchived(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY sortOrder ASC")
    fun observeAllIncludingArchived(): Flow<List<CategoryEntity>>

    @Query("UPDATE category SET isArchived = 1 WHERE id = :id")
    suspend fun archiveById(id: Long)

    @Query("UPDATE category SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveById(id: Long)

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM category")
    suspend fun getNextSortOrder(): Int

    @Query("DELETE FROM category")
    suspend fun deleteAll()
}
