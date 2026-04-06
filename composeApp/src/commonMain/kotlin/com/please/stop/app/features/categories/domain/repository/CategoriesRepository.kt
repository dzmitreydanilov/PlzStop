package com.please.stop.app.features.categories.domain.repository

import com.please.stop.app.features.categories.domain.model.CategoryWithSubcategories
import com.please.stop.app.features.categories.domain.model.ExpenseDeletionAction
import kotlinx.coroutines.flow.Flow

interface CategoriesRepository {
    fun observeCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>>
    suspend fun addCategory(name: String, iconKey: String, comment: String?): Result<Unit>
    suspend fun addSubcategory(parentCategoryId: Long, name: String, comment: String?): Result<Unit>
    suspend fun updateCategory(id: Long, name: String, iconKey: String, comment: String?): Result<Unit>
    suspend fun countExpensesForCategory(categoryId: Long): Result<Int>
    suspend fun deleteCategoryWithExpenses(categoryId: Long, action: ExpenseDeletionAction): Result<Unit>
    suspend fun deleteCategory(id: Long): Result<Unit>
    suspend fun deleteSubcategory(id: Long): Result<Unit>
    suspend fun countSubcategories(parentCategoryId: Long): Result<Int>
}
