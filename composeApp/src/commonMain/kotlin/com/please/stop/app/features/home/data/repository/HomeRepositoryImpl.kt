package com.please.stop.app.features.home.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.models.domain.Currency
import com.please.stop.app.core.runSuspendCatching
import com.please.stop.app.features.home.domain.model.HomeCategoryItem
import com.please.stop.app.features.home.domain.model.HomeData
import com.please.stop.app.features.home.domain.repository.HomeRepository
import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import com.please.stop.app.utils.date.localDateToday
import com.please.stop.app.utils.date.monthMillisRange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.number

class HomeRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val ioDispatcher: CoroutineDispatcher,
) : HomeRepository {

    override fun observeHomeData(): Flow<HomeData> {
        val today = localDateToday()
        val monthRange = monthMillisRange(today.year, today.month.number)

        return combine(
            categoryDao.observeAll(),
            expenseDao.observeSpendingByCategory(monthRange.fromMillis, monthRange.toMillis),
            expenseDao.observeTotalSpending(monthRange.fromMillis, monthRange.toMillis),
        ) { categories, spendingList, totalSpent ->
            val spendingMap = spendingList.associate { it.categoryId to it.totalMinorUnits }
            val profile = userProfileDao.get()

            HomeData(
                displayName = profile?.displayName,
                currency = Currency(
                    code = profile?.currencyCode.orEmpty(),
                    symbol = profile?.currencySymbol.orEmpty(),
                    name = "",
                    decimalPlaces = profile?.decimalPlaces ?: DEFAULT_CURRENCY_DECIMAL_PLACES,
                ),
                decimalPlaces = profile?.decimalPlaces ?: DEFAULT_CURRENCY_DECIMAL_PLACES,
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

    override suspend fun addCategory(name: String, iconKey: String): Result<Unit> {
        return runSuspendCatching {
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
    }
}
