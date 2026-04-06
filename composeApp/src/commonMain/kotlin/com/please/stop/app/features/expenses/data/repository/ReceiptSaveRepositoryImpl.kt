package com.please.stop.app.features.expenses.data.repository

import com.please.stop.app.core.db.dao.ReceiptDao
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.core.db.entity.ReceiptEntity
import com.please.stop.app.features.expenses.domain.model.ReceiptExpenseItem
import com.please.stop.app.features.expenses.domain.repository.ReceiptSaveRepository
import com.please.stop.app.utils.date.nowMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ReceiptSaveRepositoryImpl(
    private val receiptDao: ReceiptDao,
    private val ioDispatcher: CoroutineDispatcher,
) : ReceiptSaveRepository {

    override suspend fun saveReceiptWithExpenses(
        merchantName: String?,
        currency: String?,
        dateEpochMillis: Long?,
        items: List<ReceiptExpenseItem>,
        defaultCategoryId: Long,
        dateMillis: Long,
        conversionRate: Double?,
        originalCurrencyCode: String?,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val now = nowMillis()
            val receipt = ReceiptEntity(
                merchantName = merchantName,
                dateEpochMillis = dateEpochMillis,
                currency = currency,
                createdAtEpochMillis = now,
            )
            val expenses = items.map { item ->
                val categoryId = item.categoryId ?: defaultCategoryId
                val originalAmount = if (conversionRate != null && originalCurrencyCode != null) {
                    item.amountMinorUnits
                } else {
                    null
                }
                val amountToSave = if (conversionRate != null) {
                    (item.amountMinorUnits * conversionRate).toLong()
                } else {
                    item.amountMinorUnits
                }
                ExpenseEntity(
                    amountMinorUnits = amountToSave,
                    title = item.name,
                    categoryId = categoryId,
                    dateEpochMillis = dateMillis,
                    notes = null,
                    createdAtEpochMillis = now,
                    subcategoryId = item.subcategoryId,
                    originalAmountMinorUnits = originalAmount,
                    originalCurrencyCode = originalCurrencyCode,
                    conversionRate = conversionRate,
                )
            }
            receiptDao.insertReceiptWithExpenses(receipt, expenses)
            Unit
        }
    }
}
