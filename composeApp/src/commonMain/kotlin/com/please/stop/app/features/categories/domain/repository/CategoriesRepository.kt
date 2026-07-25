package com.please.stop.app.features.categories.domain.repository

import com.please.stop.app.features.categories.domain.model.CategoryWithSubcategories
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface CategoriesRepository {
    fun observeCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>>
    fun observeArchivedCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>>
    suspend fun addCategory(name: String, iconKey: String, comment: String?): Result<Unit>
    suspend fun addSubcategory(parentCategoryId: Long, name: String, comment: String?): Result<Long>
    suspend fun updateCategory(id: Long, name: String, iconKey: String, comment: String?): Result<Unit>
    suspend fun countSubcategories(parentCategoryId: Long): Result<Int>
    suspend fun archiveCategory(id: Long): Result<Unit>
    suspend fun unarchiveCategory(id: Long): Result<Unit>
    suspend fun archiveSubcategory(id: Long): Result<Unit>
    suspend fun unarchiveSubcategory(id: Long): Result<Unit>
    suspend fun deleteSubcategory(id: Long): Result<Unit>
}
