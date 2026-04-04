package com.please.stop.app.features.home.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.models.domain.Currency
import com.please.stop.app.features.home.domain.model.HomeCategoryItem
import com.please.stop.app.features.home.domain.model.HomeData
import com.please.stop.app.features.home.domain.repository.HomeRepository
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import com.please.stop.app.utils.date.currentMonthMillisRange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import com.please.stop.app.features.onboarding.domain.model.Currency as OnboardingCurrency

class HomeRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val currencyRepository: CurrencyRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : HomeRepository {

    override fun observeHomeData(): Flow<HomeData> {
        val monthRange = currentMonthMillisRange()

        return combine(
            categoryDao.observeAll(),
            expenseDao.observeSpendingByCategory(monthRange.fromMillis, monthRange.toMillis),
            expenseDao.observeTotalSpending(monthRange.fromMillis, monthRange.toMillis),
        ) { categories, spendingList, totalSpent ->
            val spendingMap = spendingList.associate { it.categoryId to it.totalMinorUnits }
            val profile = userProfileDao.get()
            val currency = resolveCurrency(profile?.currencyCode)

            HomeData(
                displayName = profile?.displayName,
                currency = currency?.toCoreCurrency() ?: Currency(
                    code = profile?.currencyCode.orEmpty(),
                    symbol = currency?.symbol.orEmpty(),
                    name = currency?.name.orEmpty(),
                ),
                decimalPlaces = currency?.decimalPlaces ?: 2,
                totalSpentMinorUnits = totalSpent ?: 0L,
                categories = categories.map { entity ->
                    HomeCategoryItem(
                        id = entity.id,
                        name = entity.name,
                        iconKey = entity.iconKey,
                        spentMinorUnits = spendingMap[entity.id] ?: 0L,
                        sortOrder = entity.sortOrder,
                    )
                },
            )
        }.flowOn(ioDispatcher)
    }

    override suspend fun addCategory(name: String, iconKey: String): Result<Unit> = runCatching {
        val sortOrder = categoryDao.getNextSortOrder()
        categoryDao.insert(
            CategoryEntity(
                name = name,
                iconKey = iconKey,
                isDefault = false,
                sortOrder = sortOrder,
            )
        )
    }

    private var currencyCache: List<OnboardingCurrency>? = null

    private suspend fun resolveCurrency(code: String?): OnboardingCurrency? {
        if (code == null) return null
        val currencies = currencyCache ?: currencyRepository.getAllCurrencies()
            .getOrNull()
            ?.also { currencyCache = it }
            ?: return null
        return currencies.find { it.code == code }
    }
}
