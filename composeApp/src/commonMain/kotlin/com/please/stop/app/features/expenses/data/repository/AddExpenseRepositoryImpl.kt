package com.please.stop.app.features.expenses.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.core.featureflags.FeatureFlags
import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import com.please.stop.app.features.expenses.domain.model.ExpenseCategory
import com.please.stop.app.features.expenses.domain.model.ExpenseDetail
import com.please.stop.app.features.expenses.domain.model.ExpenseSubcategory
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.model.Subcategory
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import com.please.stop.app.features.onboarding.domain.repository.SubcategoryRepository
import com.please.stop.app.utils.date.nowMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

class AddExpenseRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val currencyRepository: CurrencyRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val featureFlags: FeatureFlags,
    private val ioDispatcher: CoroutineDispatcher,
) : AddExpenseRepository {

    private var currencyCache: List<Currency>? = null

    override fun observeFormData(): Flow<AddExpenseFormData> {
        return categoryDao.observeAll()
            .map { categories ->
                buildFormData(
                    categories = categories,
                    subcategories = resolveSubcategories(),
                    currencyConversionEnabled = featureFlags.currencyConversionEnabled(),
                )
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getFormData(): Result<AddExpenseFormData> = runCatching {
        buildFormData(
            categories = categoryDao.observeAll().first(),
            subcategories = resolveSubcategories(),
            currencyConversionEnabled = featureFlags.currencyConversionEnabled(),
        )
    }

    private suspend fun resolveSubcategories(): List<ExpenseSubcategory> {
        return if (featureFlags.subcategoriesEnabled()) {
            subcategoryRepository.ensureSeeded().map { it.toExpenseSubcategory() }
        } else {
            emptyList()
        }
    }

    private suspend fun buildFormData(
        categories: List<CategoryEntity>,
        subcategories: List<ExpenseSubcategory>,
        currencyConversionEnabled: Boolean,
    ): AddExpenseFormData {
        val profile = userProfileDao.get()
        val currency = resolveCurrency(profile?.currencyCode)

        return AddExpenseFormData(
            currencyCode = currency?.code ?: "",
            currencySymbol = currency?.symbol ?: "$",
            decimalPlaces = currency?.decimalPlaces ?: 2,
            categories = categories.map { entity ->
                ExpenseCategory(
                    id = entity.id,
                    name = entity.name,
                    iconKey = entity.iconKey,
                )
            },
            subcategories = subcategories,
            currencyConversionEnabled = currencyConversionEnabled,
        )
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
                subcategoryId = entity.subcategoryId,
                originalAmountMinorUnits = entity.originalAmountMinorUnits,
                originalCurrencyCode = entity.originalCurrencyCode,
                conversionRate = entity.conversionRate,
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
        subcategoryId: Long?,
        originalAmountMinorUnits: Long?,
        originalCurrencyCode: String?,
        conversionRate: Double?,
    ): Result<Long> = runCatching {
        expenseDao.insert(
            ExpenseEntity(
                amountMinorUnits = amountMinorUnits,
                title = title,
                categoryId = categoryId,
                dateEpochMillis = dateEpochMillis,
                notes = notes?.takeIf { it.isNotBlank() },
                createdAtEpochMillis = nowMillis(),
                subcategoryId = subcategoryId,
                originalAmountMinorUnits = originalAmountMinorUnits,
                originalCurrencyCode = originalCurrencyCode,
                conversionRate = conversionRate,
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
        subcategoryId: Long?,
        originalAmountMinorUnits: Long?,
        originalCurrencyCode: String?,
        conversionRate: Double?,
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
                subcategoryId = subcategoryId,
                originalAmountMinorUnits = originalAmountMinorUnits,
                originalCurrencyCode = originalCurrencyCode,
                conversionRate = conversionRate,
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

private fun Subcategory.toExpenseSubcategory() = ExpenseSubcategory(
    id = id,
    parentCategoryId = parentCategoryId,
    name = name,
    iconKey = iconKey,
)
