package com.please.stop.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import java.net.ConnectException
import java.net.UnknownHostException

actual val httpEngine: HttpClientEngine = OkHttp.create()

actual fun isNoConnectionError(e: Throwable): Boolean {
    // 1. Unravel the exception to find the root cause (handles your UnknownException wrapper)
    val rootCause = e.cause ?: e
    val message = rootCause.message ?: ""

    return when (rootCause) {
        // DNS failure: Definitely no internet
        is UnknownHostException,
        is UnresolvedAddressException -> true

        is ConnectException -> {
            when {
                // Case A: ENETUNREACH (Your current trace) -> No Internet
                message.contains("ENETUNREACH", ignoreCase = true) ||
                    message.contains("Network is unreachable", ignoreCase = true) -> true

                // Case B: ECONNREFUSED (Your previous trace) -> Server is just down
                message.contains("ECONNREFUSED", ignoreCase = true) ||
                    message.contains("Connection refused", ignoreCase = true) -> false

                // Default for ConnectException (usually no route or timeout)
                else -> true
            }
        }

        // The request started but timed out; usually treated as a server/congestion issue
        is SocketTimeoutException -> false

        else -> message.contains("Unable to resolve host", ignoreCase = true)
    }
}
