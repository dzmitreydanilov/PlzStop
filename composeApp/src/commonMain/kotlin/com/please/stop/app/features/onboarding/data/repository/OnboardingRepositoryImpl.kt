package com.please.stop.app.features.onboarding.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.db.entity.UserProfileEntity
import com.please.stop.app.features.onboarding.domain.model.OnboardingData
import com.please.stop.app.features.onboarding.domain.repository.CategoryRepository
import com.please.stop.app.features.onboarding.domain.repository.OnboardingRepository
import com.please.stop.app.features.onboarding.presentation.CategoryUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OnboardingRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val categoryDao: CategoryDao,
    private val categoryRepository: CategoryRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : OnboardingRepository {

    override fun observeIsOnboardingCompleted(): Flow<Boolean> =
        userProfileDao.observeOnboardingCompleted()
            .map { it ?: false }
            .flowOn(ioDispatcher)

    override suspend fun completeOnboarding(data: OnboardingData): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            userProfileDao.upsert(data.toUserProfileEntity())

            val defaultCategories = categoryRepository.getDefaultCategories().getOrThrow()
            categoryDao.insertAll(
                defaultCategories.mapIndexed { index, category ->
                    category.toCategoryEntity(sortOrder = index)
                }
            )
        }
    }
}

private fun OnboardingData.toUserProfileEntity() = UserProfileEntity(
    id = 1,
    displayName = displayName?.takeIf { it.isNotBlank() },
    currencyCode = currency.code,
    monthlyBudget = monthlyBudgetMinorUnits,
    onboardingCompleted = true,
)

private fun CategoryUiModel.toCategoryEntity(sortOrder: Int) = CategoryEntity(
    id = 0, // autoGenerate
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
    sortOrder = sortOrder,
)
