package com.please.stop.app.features.auth.apple

internal class NoOpAppleAuthProvider : AppleAuthProvider {

    override suspend fun signIn(): AppleUser? {
        throw UnsupportedOperationException("Apple Sign-In is not supported on Android")
    }

    override suspend fun signOut() {
        // No-op on Android
    }
}
