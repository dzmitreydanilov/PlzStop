package com.please.stop.app.network

import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

private const val TIMEOUT_DURATION = 30_000L

data class HttpClientDataConfig(
    val engine: HttpClientEngine,
    val json: Json,
    val networkLoggingEnabled: Boolean,
    val isDebug: Boolean = false,
    val port: Int? = null,
    val httpRequestTimeoutMillis: Long = TIMEOUT_DURATION,
    val httpConnectTimeoutMillis: Long = TIMEOUT_DURATION,
    val httpSocketTimeoutMillis: Long = TIMEOUT_DURATION,
    val retryEnabled: Boolean = false,
    val maxRetry: Int = 3,
    val retryOnTimeOut: Boolean = false
)
