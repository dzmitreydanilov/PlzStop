package com.please.stop.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.please.stop.app.core.db.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface SubcategoryDao {

    @Query(
        "SELECT parentCategoryId, COUNT(*) AS count FROM subcategory " +
            "WHERE isArchived = 0 GROUP BY parentCategoryId",
    )
    fun observeActiveCounts(): Flow<List<SubcategoryCount>>

    @Query("SELECT * FROM subcategory WHERE parentCategoryId = :parentId AND isArchived = 0 ORDER BY sortOrder ASC")
    fun observeByParent(parentId: Long): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategory WHERE parentCategoryId = :parentId AND isArchived = 0 ORDER BY sortOrder ASC")
    suspend fun getByParent(parentId: Long): List<SubcategoryEntity>

    @Query("SELECT * FROM subcategory WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategory ORDER BY sortOrder ASC")
    fun observeAllIncludingArchived(): Flow<List<SubcategoryEntity>>

    @Query("UPDATE subcategory SET isArchived = 1 WHERE id = :id")
    suspend fun archiveById(id: Long)

    @Query("UPDATE subcategory SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveById(id: Long)

    @Query("UPDATE subcategory SET isArchived = 1 WHERE parentCategoryId = :categoryId")
    suspend fun archiveByParentCategoryId(categoryId: Long)

    @Query("UPDATE subcategory SET isArchived = 0 WHERE parentCategoryId = :categoryId")
    suspend fun unarchiveByParentCategoryId(categoryId: Long)

    @Insert
    suspend fun insertAll(subcategories: List<SubcategoryEntity>)

    @Insert
    suspend fun insert(subcategory: SubcategoryEntity): Long

    @Update
    suspend fun update(subcategory: SubcategoryEntity)

    @Query("DELETE FROM subcategory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM subcategory WHERE parentCategoryId = :parentId")
    suspend fun getNextSortOrder(parentId: Long): Int

    @Query("SELECT COUNT(*) FROM subcategory WHERE parentCategoryId = :parentId")
    suspend fun countByParent(parentId: Long): Int

    @Query("SELECT COUNT(*) FROM subcategory")
    suspend fun count(): Int

    @Query("DELETE FROM subcategory WHERE isDefault = 0")
    suspend fun deleteAllNonDefault()
}

data class SubcategoryCount(val parentCategoryId: Long, val count: Int)
