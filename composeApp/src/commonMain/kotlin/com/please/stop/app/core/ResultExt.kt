package com.please.stop.app.core

import kotlinx.coroutines.CancellationException

inline fun <T> runSuspendCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
        Result.failure(error)
    }
}
