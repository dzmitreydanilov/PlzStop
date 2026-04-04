package com.please.stop.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.please.stop.app.core.db.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubcategoryDao {

    @Query("SELECT * FROM subcategory WHERE parentCategoryId = :parentId ORDER BY sortOrder ASC")
    fun observeByParent(parentId: Long): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategory ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<SubcategoryEntity>>

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
}
