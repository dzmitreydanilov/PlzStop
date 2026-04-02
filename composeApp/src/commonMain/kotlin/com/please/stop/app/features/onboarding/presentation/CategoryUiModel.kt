package com.please.stop.app.features.onboarding.presentation

import kotlinx.serialization.Serializable

@Serializable
data class CategoryUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
    val isDefault: Boolean,
)
