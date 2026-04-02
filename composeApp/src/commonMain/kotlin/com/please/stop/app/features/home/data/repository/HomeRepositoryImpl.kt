package com.please.stop.app.features.home.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.features.home.domain.model.HomeData
import com.please.stop.app.features.home.domain.model.HomeCategoryItem
import com.please.stop.app.features.home.domain.repository.HomeRepository
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class HomeRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val currencyRepository: CurrencyRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : HomeRepository {

    @OptIn(ExperimentalTime::class)
    override fun observeHomeData(): Flow<HomeData> {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(timeZone)
        val monthStart = LocalDate(today.year, today.monthNumber, 1)
        val nextMonthStart = monthStart.plus(1, DateTimeUnit.MONTH)
        val fromMillis = monthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val toMillis = nextMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()

        return combine(
            categoryDao.observeAll(),
            expenseDao.observeSpendingByCategory(fromMillis, toMillis),
            expenseDao.observeTotalSpending(fromMillis, toMillis),
        ) { categories, spendingList, totalSpent ->
            val spendingMap = spendingList.associate { it.categoryId to it.totalMinorUnits }
            val profile = userProfileDao.get()
            val currency = resolveCurrency(profile?.currencyCode)

            HomeData(
                displayName = profile?.displayName,
                currencyCode = currency?.code ?: (profile?.currencyCode ?: "USD"),
                currencySymbol = currency?.symbol ?: "$",
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
            com.please.stop.app.core.db.entity.CategoryEntity(
                name = name,
                iconKey = iconKey,
                isDefault = false,
                sortOrder = sortOrder,
            )
        )
    }

    private var currencyCache: List<Currency>? = null

    private suspend fun resolveCurrency(code: String?): Currency? {
        if (code == null) return null
        val currencies = currencyCache ?: currencyRepository.getAllCurrencies()
            .getOrNull()
            ?.also { currencyCache = it }
            ?: return null
        return currencies.find { it.code == code }
    }
}
