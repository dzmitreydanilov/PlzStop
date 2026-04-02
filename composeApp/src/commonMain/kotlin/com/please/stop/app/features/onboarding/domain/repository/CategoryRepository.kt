package com.please.stop.app.features.onboarding.domain.repository

import com.please.stop.app.features.onboarding.presentation.CategoryUiModel

interface CategoryRepository {
    suspend fun getDefaultCategories(): Result<List<CategoryUiModel>>
}
