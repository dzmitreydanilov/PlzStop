package com.please.stop.app.features.onboarding.domain.model

data class Category(
    val id: Long,
    val name: String,
    val iconKey: String,
    val isDefault: Boolean,
)
