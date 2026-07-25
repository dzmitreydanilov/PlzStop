package com.please.stop.app.core.models.data

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAccountLink(
    val email: String,
    val isConnected: Boolean,
)
