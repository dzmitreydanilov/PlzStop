package com.please.stop.app.features.expenses.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.SubcategoryUsage
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.core.featureflags.FeatureFlags
import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import com.please.stop.app.features.expenses.domain.model.ExpenseCategory
import com.please.stop.app.features.expenses.domain.model.ExpenseDetail
import com.please.stop.app.features.expenses.domain.model.ExpenseSubcategory
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import com.please.stop.app.features.onboarding.domain.model.Subcategory
import com.please.stop.app.features.onboarding.domain.repository.SubcategoryRepository
import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import com.please.stop.app.utils.date.nowMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.ExperimentalTime

class AddExpenseRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val subcategoryRepository: SubcategoryRepository,
    private val featureFlags: FeatureFlags,
    private val ioDispatcher: CoroutineDispatcher,
) : AddExpenseRepository {

    override fun observeFormData(): Flow<AddExpenseFormData> {
        val subcategories = flow {
            subcategoryRepository.ensureSeeded()
            emitAll(subcategoryRepository.observeAll())
        }

        return combine(
            categoryDao.observeAll(),
            subcategories,
            expenseDao.observeSubcategoryUsage(),
        ) { categories, observedSubcategories, subcategoryUsage ->
            val resolvedSubcategories = if (featureFlags.subcategoriesEnabled()) {
                observedSubcategories.map { it.toExpenseSubcategory() }
            } else {
                emptyList()
            }
            val frequentSubcategoryIds = subcategoryUsage
                .groupBy { it.categoryId }
                .mapValues { (_, usage) ->
                    usage
                        .sortedWith(
                            compareByDescending<SubcategoryUsage> { it.useCount }
                                .thenByDescending { it.lastUsedEpochMillis },
                        )
                        .map { it.subcategoryId }
                }

            buildFormData(
                categories = categories,
                subcategories = resolvedSubcategories,
                frequentSubcategoryIdsByCategory = frequentSubcategoryIds,
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
        frequentSubcategoryIdsByCategory: Map<Long, List<Long>> = emptyMap(),
        currencyConversionEnabled: Boolean,
    ): AddExpenseFormData {
        val profile = userProfileDao.get()

        return AddExpenseFormData(
            currencyCode = profile?.currencyCode.orEmpty(),
            currencySymbol = profile?.currencySymbol.orEmpty(),
            decimalPlaces = profile?.decimalPlaces ?: DEFAULT_CURRENCY_DECIMAL_PLACES,
            categories = categories.map { entity ->
                ExpenseCategory(
                    id = entity.id,
                    name = entity.name,
                    iconKey = entity.iconKey,
                )
            },
            subcategories = subcategories,
            frequentSubcategoryIdsByCategory = frequentSubcategoryIdsByCategory,
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
}

private fun Subcategory.toExpenseSubcategory() = ExpenseSubcategory(
    id = id,
    parentCategoryId = parentCategoryId,
    name = name,
    iconKey = iconKey,
)
