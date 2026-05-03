package com.please.stop.app.features.auth.google

data class GoogleUser(
    val idToken: String,
    val accessToken: String? = null
)
