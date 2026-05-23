package com.please.stop.app.features.auth.google

import androidx.compose.runtime.Composable

@Suppress("TopLevelComposableFunctions")
interface GoogleAuthProvider {

    @Composable
    fun getUiProvider(): GoogleAuthUiProvider

    suspend fun signOut()
}
