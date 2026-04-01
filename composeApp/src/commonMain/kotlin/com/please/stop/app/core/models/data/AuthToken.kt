package com.please.stop.app.core.models.data

import kotlinx.serialization.Serializable

@Serializable
data class AuthToken(
    val accessToken: String? = null,
    val refreshToken: String? = null
)
