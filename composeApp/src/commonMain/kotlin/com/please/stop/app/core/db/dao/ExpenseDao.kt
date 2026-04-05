package com.please.stop.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.please.stop.app.core.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

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
}

data class CategorySpending(
    val categoryId: Long,
    val totalMinorUnits: Long,
)
