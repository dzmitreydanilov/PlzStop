package com.please.stop.app.features.addexpense.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.features.addexpense.domain.model.AddExpenseFormData
import com.please.stop.app.features.addexpense.domain.model.ExpenseCategory
import com.please.stop.app.features.addexpense.domain.model.ExpenseDetail
import com.please.stop.app.features.addexpense.domain.repository.AddExpenseRepository
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AddExpenseRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val currencyRepository: CurrencyRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : AddExpenseRepository {

    private var currencyCache: List<Currency>? = null

    override fun observeFormData(): Flow<AddExpenseFormData> {
        return categoryDao.observeAll()
            .map { categories ->
                val profile = userProfileDao.get()
                val currency = resolveCurrency(profile?.currencyCode)

                AddExpenseFormData(
                    currencySymbol = currency?.symbol ?: "$",
                    decimalPlaces = currency?.decimalPlaces ?: 2,
                    categories = categories.map { entity ->
                        ExpenseCategory(
                            id = entity.id,
                            name = entity.name,
                            iconKey = entity.iconKey,
                        )
                    },
                )
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getExpenseById(id: Long): Result<ExpenseDetail?> = runCatching {
        expenseDao.getById(id)?.let { entity ->
            ExpenseDetail(
                id = entity.id,
                amountMinorUnits = entity.amountMinorUnits,
                title = entity.title,
                categoryId = entity.categoryId,
                dateEpochMillis = entity.dateEpochMillis,
                notes = entity.notes,
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun saveExpense(
        amountMinorUnits: Long,
        title: String,
        categoryId: Long,
        dateEpochMillis: Long,
        notes: String?,
    ): Result<Long> = runCatching {
        expenseDao.insert(
            ExpenseEntity(
                amountMinorUnits = amountMinorUnits,
                title = title,
                categoryId = categoryId,
                dateEpochMillis = dateEpochMillis,
                notes = notes?.takeIf { it.isNotBlank() },
                createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
            )
        )
    }

    override suspend fun updateExpense(
        id: Long,
        amountMinorUnits: Long,
        title: String,
        categoryId: Long,
        dateEpochMillis: Long,
        notes: String?,
    ): Result<Unit> = runCatching {
        val existing = expenseDao.getById(id) ?: return Result.failure(
            IllegalStateException("Expense with id=$id not found")
        )
        expenseDao.update(
            existing.copy(
                amountMinorUnits = amountMinorUnits,
                title = title,
                categoryId = categoryId,
                dateEpochMillis = dateEpochMillis,
                notes = notes?.takeIf { it.isNotBlank() },
            )
        )
    }

    override suspend fun deleteExpense(id: Long): Result<Unit> = runCatching {
        expenseDao.softDelete(id)
    }

    private suspend fun resolveCurrency(code: String?): Currency? {
        if (code == null) return null
        val currencies = currencyCache ?: currencyRepository.getAllCurrencies()
            .getOrNull()
            ?.also { currencyCache = it }
            ?: return null
        return currencies.find { it.code == code }
    }
}
