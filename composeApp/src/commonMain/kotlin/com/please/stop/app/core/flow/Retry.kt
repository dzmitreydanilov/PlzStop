package com.please.stop.app.core.flow

import com.please.stop.app.core.logger.logError
import com.please.stop.app.network.RestResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Retries a suspending block of code that returns a [RestResponse] with exponential backoff.
 * The retry operation is triggered only if the block returns a [RestResponse.Error].
 *
 * @param T The type of the success body in the [RestResponse].
 * @param times The total number of attempts (default is Int.MAX_VALUE for unlimited retries).
 * @param initialDelay The initial delay in milliseconds before the first retry (default is 100ms).
 * @param maxDelay The maximum delay in milliseconds between retries (default is 1000ms).
 * @param factor The multiplicative factor for increasing the delay (default is 2.0).
 * @param block The suspending lambda function to execute, which returns a [RestResponse<T>].
 * @return The first [RestResponse.Success] received, or the [RestResponse.Error] from the final attempt.
 */
suspend fun <T> retryRest(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> RestResponse<T>
): RestResponse<T> {
    var currentDelay = initialDelay
    repeat(times - 1) {
        when (val response = block()) {
            is RestResponse.Success -> return response
            is RestResponse.Error -> {
                log(throwable = response.throwable, currentDelay = currentDelay)
            }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()
}

/**
 * Returns a Flow that executes a suspending block of code and retries with exponential backoff
 * if the block returns a [RestResponse.Error].
 * The Flow will emit one [RestResponse] (either the first Success or the final Error) and then complete.
 *
 * @param T The type of the success body in the [RestResponse].
 * @param times The total number of attempts (default is Int.MAX_VALUE for unlimited retries).
 * @param initialDelay The initial delay in milliseconds before the first retry (default is 100ms).
 * @param maxDelay The maximum delay in milliseconds between retries (default is 1000ms).
 * @param factor The multiplicative factor for increasing the delay (default is 2.0).
 * @param block The suspending lambda function to execute, which returns a [RestResponse<T>].
 * @return A [Flow] that, when collected, will execute the retry logic and emit a single [RestResponse<T>].
 */
fun <T> retryRestAsFlow(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> RestResponse<T>
): Flow<RestResponse<T>> = flow {
    var currentDelay = initialDelay
    repeat(times - 1) {
        when (val response = block()) {
            is RestResponse.Success -> {
                emit(response)
                return@flow
            }

            is RestResponse.Error -> {
                log(throwable = response.throwable, currentDelay = currentDelay)
            }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    emit(block())
}

private fun log(throwable: Throwable, currentDelay: Long) {
    logError(
        throwable,
        "Attempt failed, retrying in ${currentDelay}ms: ${throwable.message}"
    )
}
