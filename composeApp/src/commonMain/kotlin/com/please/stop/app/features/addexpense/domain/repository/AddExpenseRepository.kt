package com.please.stop.app.features.addexpense.domain.repository

import com.please.stop.app.features.addexpense.domain.model.AddExpenseFormData
import com.please.stop.app.features.addexpense.domain.model.ExpenseDetail
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
    ): Result<Long>
    suspend fun updateExpense(
        id: Long,
        amountMinorUnits: Long,
        title: String,
        categoryId: Long,
        dateEpochMillis: Long,
        notes: String?,
    ): Result<Unit>
    suspend fun deleteExpense(id: Long): Result<Unit>
}
