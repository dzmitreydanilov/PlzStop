package com.please.stop.app.features.home.domain.model

data class HomeData(
    val displayName: String?,
    val currencyCode: String,
    val currencySymbol: String,
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
