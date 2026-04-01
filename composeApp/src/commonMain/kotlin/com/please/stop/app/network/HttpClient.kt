package com.please.stop.app.network

import io.ktor.client.engine.HttpClientEngine

expect val httpEngine: HttpClientEngine

expect fun isNoConnectionError(e: Throwable): Boolean
