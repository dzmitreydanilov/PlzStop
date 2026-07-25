package com.please.stop.app.features.categories.domain.model

import com.please.stop.app.features.onboarding.domain.model.Category

data class CategorySummary(
    val category: Category,
    val subcategoryCount: Int?,
)
