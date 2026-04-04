package com.please.stop.app.features.expenses.domain.repository

import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import com.please.stop.app.features.expenses.domain.model.ExpenseDetail
import kotlinx.coroutines.flow.Flow

interface AddExpenseRepository {
    fun observeFormData(): Flow<AddExpenseFormData>
    suspend fun getFormData(): Result<AddExpenseFormData>
    suspend fun getExpenseById(id: Long): Result<ExpenseDetail?>
    suspend fun saveExpense(
        amountMinorUnits: Long,
        title: String,
        categoryId: Long,
        dateEpochMillis: Long,
        notes: String?,
        subcategoryId: Long? = null,
    ): Result<Long>
    suspend fun updateExpense(
        id: Long,
        amountMinorUnits: Long,
        title: String,
        categoryId: Long,
        dateEpochMillis: Long,
        notes: String?,
        subcategoryId: Long? = null,
    ): Result<Unit>
    suspend fun deleteExpense(id: Long): Result<Unit>
}
