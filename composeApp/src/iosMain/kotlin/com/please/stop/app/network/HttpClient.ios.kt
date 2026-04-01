package com.please.stop.app.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

private const val NS_URL_ERROR_CANNOT_CONNECT_TO_HOST = -1004L

actual val httpEngine: HttpClientEngine = Darwin.create()

actual fun isNoConnectionError(e: Throwable): Boolean {
    val nsError = (e as? DarwinHttpRequestException)?.origin
    if (nsError != null) {
        return nsError.domain == NSURLErrorDomain && (
            nsError.code == NSURLErrorNotConnectedToInternet ||
                nsError.code == NSURLErrorTimedOut || nsError.code == NS_URL_ERROR_CANNOT_CONNECT_TO_HOST
            )
    }
    return when (e) {
        is UnresolvedAddressException -> true
        is SocketTimeoutException -> true
        else -> false
    }
}

