package com.please.stop.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.please.stop.app.core.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("SELECT * FROM expense WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("UPDATE expense SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT COUNT(*) FROM expense WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun countActiveByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM expense WHERE categoryId = :categoryId")
    suspend fun countAllByCategory(categoryId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM expense WHERE isDeleted = 0 LIMIT 1)")
    suspend fun hasActiveExpenses(): Boolean

    @Query("UPDATE expense SET categoryId = :targetCategoryId WHERE categoryId = :sourceCategoryId")
    suspend fun reassignCategory(sourceCategoryId: Long, targetCategoryId: Long)

    @Query("DELETE FROM expense WHERE categoryId = :categoryId")
    suspend fun hardDeleteByCategory(categoryId: Long)

    @Query(
        """
        SELECT categoryId, SUM(amountMinorUnits) AS totalMinorUnits
        FROM expense
        WHERE isDeleted = 0
          AND dateEpochMillis >= :fromEpochMillis
          AND dateEpochMillis < :toEpochMillis
        GROUP BY categoryId
        """
    )
    fun observeSpendingByCategory(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Flow<List<CategorySpending>>

    @Query(
        """
        SELECT categoryId, subcategoryId, SUM(amountMinorUnits) AS totalMinorUnits
        FROM expense
        WHERE isDeleted = 0
          AND dateEpochMillis >= :fromEpochMillis
          AND dateEpochMillis < :toEpochMillis
        GROUP BY categoryId, subcategoryId
        """
    )
    fun observeSpendingByCategoryAndSubcategory(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Flow<List<CategorySubcategorySpending>>

    @Query(
        """
        SELECT SUM(amountMinorUnits)
        FROM expense
        WHERE isDeleted = 0
          AND dateEpochMillis >= :fromEpochMillis
          AND dateEpochMillis < :toEpochMillis
        """
    )
    fun observeTotalSpending(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Flow<Long?>

    @Query(
        """
        SELECT * FROM expense
        WHERE isDeleted = 0
          AND dateEpochMillis >= :fromEpochMillis
          AND dateEpochMillis < :toEpochMillis
        ORDER BY dateEpochMillis DESC
        """
    )
    fun observeExpensesInRange(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expense
        WHERE isDeleted = 0
          AND dateEpochMillis >= :fromEpochMillis
          AND dateEpochMillis < :toEpochMillis
        ORDER BY dateEpochMillis DESC
        """
    )
    suspend fun getExpensesInRange(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): List<ExpenseEntity>

    @Query("DELETE FROM expense")
    suspend fun deleteAll()
}

data class CategorySpending(
    val categoryId: Long,
    val totalMinorUnits: Long,
)

data class CategorySubcategorySpending(
    val categoryId: Long,
    val subcategoryId: Long?,
    val totalMinorUnits: Long,
)
