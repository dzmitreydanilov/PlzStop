package com.please.stop.app.network.authentication

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider

class BearerTokenClearer(
    private val httpClient: HttpClient
) {

    fun clear() {
        val authProvider = httpClient.authProvider<BearerAuthProvider>()
        authProvider?.clearToken()
    }
}
