package com.please.stop.app.network

/**
 * A sealed class representing network responses that can be either a successful response or an error response.
 *
 * @param <T> The type of the successful response body.
 * @param <E> The type of the error response.
 */
sealed interface RestResponse<out T> {

    data class Success<T>(val body: T, val statusCode: Int) : RestResponse<T>

    data class Error(val throwable: Throwable) : RestResponse<Nothing>
}
