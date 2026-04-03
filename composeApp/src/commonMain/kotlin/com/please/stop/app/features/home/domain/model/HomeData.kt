package com.please.stop.app.features.home.domain.model

import com.please.stop.app.core.models.domain.Currency

data class HomeData(
    val displayName: String?,
    val currency: Currency,
    val decimalPlaces: Int,
    val totalSpentMinorUnits: Long,
    val categories: List<HomeCategoryItem>,
)

data class HomeCategoryItem(
    val id: Long,
    val name: String,
    val iconKey: String,
    val spentMinorUnits: Long,
    val sortOrder: Int,
)
