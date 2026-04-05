package com.please.stop.app.features.onboarding.domain.repository

import com.please.stop.app.features.onboarding.domain.model.Subcategory
import kotlinx.coroutines.flow.Flow

interface SubcategoryRepository {

    fun observeByParent(parentCategoryId: Long): Flow<List<Subcategory>>

    fun observeAll(): Flow<List<Subcategory>>

    suspend fun getDefaultSubcategories(): Result<List<Subcategory>>

    suspend fun ensureSeeded(): List<Subcategory>
}
