package com.please.stop.app.features.auth.apple

interface AppleAuthProvider {
    val isSupported: Boolean
    suspend fun signIn(): AppleUser?
    suspend fun signOut()
}
