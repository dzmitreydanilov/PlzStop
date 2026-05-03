package com.please.stop.app.features.auth.apple

data class AppleUser(
    val identityToken: String,
    val nonce: String,
    val email: String?,
)
