package com.please.stop.app.network.authentication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Token(
    @SerialName("access")
    val access: String,

    @SerialName("refresh")
    val refresh: String
)
