package com.please.stop.app.network

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse

/**
 * Serves to signal a specific authentication failure scenario: when the refresh token has expired,
 * and the authentication process cannot proceed without a new login.
 */
class OAuthSessionExpirationException(
    response: HttpResponse,
    cachedResponseText: String,
) : ResponseException(response, cachedResponseText)
