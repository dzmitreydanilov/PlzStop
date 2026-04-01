package com.please.stop.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js


actual val httpEngine: HttpClientEngine = Js.create()

actual fun isNoConnectionError(e: Throwable): Boolean {
    return false
}

