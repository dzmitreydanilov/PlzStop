package com.please.stop.app.network.authentication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequestBody(

    @SerialName("refresh")
    val refreshToken: String
)
