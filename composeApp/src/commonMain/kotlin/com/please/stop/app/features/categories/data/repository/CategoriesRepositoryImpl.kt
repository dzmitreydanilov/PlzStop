package com.please.stop.app.features.categories.data.repository

import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.db.entity.SubcategoryEntity
import com.please.stop.app.core.featureflags.FeatureFlags
import com.please.stop.app.features.categories.domain.model.CategoryWithSubcategories
import com.please.stop.app.features.categories.domain.repository.CategoriesRepository
import com.please.stop.app.features.onboarding.domain.model.Category
import com.please.stop.app.features.onboarding.domain.model.Subcategory
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

@Suppress("TooManyFunctions")
class CategoriesRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val featureFlags: FeatureFlags,
    private val ioDispatcher: CoroutineDispatcher,
) : CategoriesRepository {

    override fun observeCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>> =
        combine(
            categoryDao.observeAll(),
            subcategoryDao.observeAll(),
            featureFlags.observeSubcategoriesEnabled(),
        ) { categories, subcategories, subcategoriesEnabled ->
            val subcategoryMap = subcategories.groupBy { it.parentCategoryId }
            categories.map { entity ->
                CategoryWithSubcategories(
                    category = entity.toDomain(),
                    subcategories = if (subcategoriesEnabled) {
                        subcategoryMap[entity.id]
                            .orEmpty()
                            .map { it.toDomain() }
                            .toImmutableList()
                    } else {
                        null
                    },
                )
            }
        }.flowOn(ioDispatcher)

    override suspend fun addCategory(
        name: String,
        iconKey: String,
        comment: String?,
    ): Result<Unit> = runCatching {
        val sortOrder = categoryDao.getNextSortOrder()
        categoryDao.insert(
            CategoryEntity(
                name = name,
                iconKey = iconKey,
                isDefault = false,
                sortOrder = sortOrder,
                comment = comment,
            ),
        )
    }

    override suspend fun updateCategory(
        id: Long,
        name: String,
        iconKey: String,
        comment: String?,
    ): Result<Unit> = runCatching {
        val existing = categoryDao.getById(id) ?: error("Category $id not found")
        categoryDao.update(existing.copy(name = name, iconKey = iconKey, comment = comment))
    }

    override suspend fun countSubcategories(parentCategoryId: Long): Result<Int> = runCatching {
        subcategoryDao.countByParent(parentCategoryId)
    }

    override suspend fun archiveCategory(id: Long): Result<Unit> = runCatching {
        categoryDao.archiveById(id)
        subcategoryDao.archiveByParentCategoryId(id)
    }

    override suspend fun unarchiveCategory(id: Long): Result<Unit> = runCatching {
        categoryDao.unarchiveById(id)
        subcategoryDao.unarchiveByParentCategoryId(id)
    }

    override suspend fun archiveSubcategory(id: Long): Result<Unit> = runCatching {
        subcategoryDao.archiveById(id)
    }

    override suspend fun unarchiveSubcategory(id: Long): Result<Unit> = runCatching {
        subcategoryDao.unarchiveById(id)
    }

    override suspend fun deleteSubcategory(id: Long): Result<Unit> = runCatching {
        subcategoryDao.deleteById(id)
    }

    override fun observeArchivedCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>> =
        combine(
            categoryDao.observeArchived(),
            subcategoryDao.observeAllIncludingArchived(),
            featureFlags.observeSubcategoriesEnabled(),
        ) { categories, subcategories, subcategoriesEnabled ->
            val subcategoryMap = subcategories.groupBy { it.parentCategoryId }
            categories.map { entity ->
                CategoryWithSubcategories(
                    category = entity.toDomain(),
                    subcategories = if (subcategoriesEnabled) {
                        subcategoryMap[entity.id]
                            .orEmpty()
                            .map { it.toDomain() }
                            .toImmutableList()
                    } else {
                        null
                    },
                )
            }
        }.flowOn(ioDispatcher)

    override suspend fun addSubcategory(
        parentCategoryId: Long,
        name: String,
        comment: String?,
    ): Result<Long> {
        if (!featureFlags.subcategoriesEnabled()) {
            return Result.failure(IllegalStateException("Subcategories feature is disabled"))
        }
        val currentCount = subcategoryDao.countByParent(parentCategoryId)
        if (currentCount >= MAX_SUBCATEGORIES_PER_CATEGORY) {
            return Result.failure(
                IllegalStateException("Maximum $MAX_SUBCATEGORIES_PER_CATEGORY subcategories per category"),
            )
        }
        return runCatching {
            val sortOrder = subcategoryDao.getNextSortOrder(parentCategoryId)
            subcategoryDao.insert(
                SubcategoryEntity(
                    parentCategoryId = parentCategoryId,
                    name = name,
                    iconKey = "ic_other",
                    isDefault = false,
                    sortOrder = sortOrder,
                    comment = comment,
                ),
            )
        }
    }
}

private const val MAX_SUBCATEGORIES_PER_CATEGORY = 10

private fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
    comment = comment,
)

private fun SubcategoryEntity.toDomain() = Subcategory(
    id = id,
    parentCategoryId = parentCategoryId,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
    sortOrder = sortOrder,
    comment = comment,
)
