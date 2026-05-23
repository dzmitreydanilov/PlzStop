package com.please.stop.app.features.auth.apple

interface AppleAuthProvider {
    suspend fun signIn(): AppleUser?
    suspend fun signOut()
}
