package com.please.stop.app.network

import com.please.stop.app.core.ITokensStorage
import com.please.stop.app.core.buildSettings.DeviceIdReader
import com.please.stop.app.core.logger.logDebug
import com.please.stop.app.core.logger.logError
import com.please.stop.app.core.models.data.AuthToken
import com.please.stop.app.network.BaseUrlConfig.BASE_URL
import com.please.stop.app.network.BaseUrlConfig.REFRESH_ACCESS_TOKEN_END_POINT
import com.please.stop.app.network.authentication.RefreshTokenRequestBody
import com.please.stop.app.network.authentication.Token
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Workaround for post requests
// https://github.com/ktorio/ktor/issues/1793
internal fun HttpClientConfig<*>.configureRedirection() {
    install(HttpRedirect) {
        checkHttpMethod = false
    }
}

internal fun HttpClientConfig<*>.configureContent(json: Json) {
    install(ContentNegotiation) {
        json(json)
    }
}

internal fun HttpClientConfig<*>.contentEncoding() {
    install(ContentEncoding)
}

internal fun HttpClientConfig<*>.configureTimeOut(configuration: HttpClientDataConfig) {
    with(configuration) {
        if (retryEnabled) {
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = maxRetry)
                retryOnException(maxRetries = maxRetry, retryOnTimeout = retryOnTimeOut)
                exponentialDelay()
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = httpRequestTimeoutMillis
            connectTimeoutMillis = httpConnectTimeoutMillis
            socketTimeoutMillis = httpSocketTimeoutMillis
        }
    }
}

internal fun HttpClientConfig<*>.configureDefaultRequest(
    port: Int? = null
) {
    defaultRequest {
        url(BASE_URL)
        port?.let {
            url {
                this.port = it
            }
        }
    }
}

internal fun HttpClientConfig<*>.bearerAuth(tokenStorage: ITokensStorage) {
    install(Auth) {
        bearer {
            loadTokens {
                val token = tokenStorage.readAuthTokenBlocking()
                if (token?.accessToken != null) {
                    BearerTokens(token.accessToken, token.refreshToken)
                } else {
                    logError(message = "Access token is null")
                    null
                }
            }

            refreshTokens {
                val token = client.post {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    url(REFRESH_ACCESS_TOKEN_END_POINT)
                    setBody(
                        RefreshTokenRequestBody(
                            refreshToken = tokenStorage.readAuthTokenBlocking()?.refreshToken.orEmpty()
                        )
                    )
                }.body<Token>()
                tokenStorage.writeAuthToken(
                    AuthToken(
                        accessToken = token.access,
                        refreshToken = token.refresh
                    )
                )
                BearerTokens(
                    accessToken = token.access,
                    refreshToken = token.refresh
                )
            }
        }
    }
}

internal fun HttpClientConfig<*>.logging() {
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                logDebug(message)
            }
        }
        level = LogLevel.ALL
    }
}
