package com.please.stop.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.core.db.entity.ReceiptEntity

@Dao
abstract class ReceiptDao {

    @Transaction
    open suspend fun insertReceiptWithExpenses(
        receipt: ReceiptEntity,
        expenses: List<ExpenseEntity>,
    ): Long {
        val receiptId = insert(receipt)
        expenses.forEach { insertExpense(it.copy(receiptId = receiptId)) }
        return receiptId
    }

    @Insert
    abstract suspend fun insert(receipt: ReceiptEntity): Long

    @Insert
    abstract suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM receipt WHERE id IN (:ids)")
    abstract suspend fun getByIds(ids: List<Long>): List<ReceiptEntity>
}
