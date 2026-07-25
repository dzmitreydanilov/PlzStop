package com.please.stop.app.features.home.presentation

import kotlinx.serialization.Serializable

@Serializable
data class HomeCategoryUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
    val spentFormatted: String,
    val hasSpending: Boolean,
)
