package com.please.stop.app.features.onboarding.domain.model

data class Subcategory(
    val id: Long,
    val parentCategoryId: Long,
    val name: String,
    val iconKey: String,
    val isDefault: Boolean,
    val sortOrder: Int,
)
