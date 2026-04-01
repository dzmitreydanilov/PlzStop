package com.please.stop.app.core.models.data

import com.please.stop.app.core.logger.logError
import com.please.stop.app.network.RestResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

inline fun <T, R> Flow<RestResponse<T>>.mapOnSuccess(
    crossinline transform: suspend (T) -> Result<R>
): Flow<Result<R>> = map { response ->
    when (response) {
        is RestResponse.Success -> transform(response.body)
        is RestResponse.Error -> Result.failure(response.throwable)
    }
}.onError { logError(throwable = it, message = it.message.orEmpty()) }

fun <T> Flow<RestResponse<T>>.mapResponseToResult(): Flow<Result<T>> {
    return map { response ->
        when (response) {
            is RestResponse.Success -> Result.success(response.body)
            is RestResponse.Error -> Result.failure(response.throwable)
        }
    }.catch { emit(Result.failure(it)) }
}

inline fun <T, R> Flow<RestResponse<T>>.mapToResult(
    crossinline transform: suspend (T) -> R
): Flow<Result<R>> = map { response ->
    when (response) {
        is RestResponse.Success -> Result.success(transform(response.body))
        is RestResponse.Error -> Result.failure(response.throwable)
    }
}.onError { logError(throwable = it, message = it.message.orEmpty()) }

inline fun <T, R> Flow<RestResponse<T>>.mapToResult(
    crossinline transform: suspend (T, Int) -> R
): Flow<Result<R>> = map { response ->
    when (response) {
        is RestResponse.Success -> Result.success(transform(response.body, response.statusCode))
        is RestResponse.Error -> Result.failure(response.throwable)
    }
}.onError { logError(throwable = it, message = it.message.orEmpty()) }

fun <T> Flow<RestResponse<T>>.mapToResult(): Flow<Result<Unit>> = map { response ->
    when (response) {
        is RestResponse.Success -> Result.success(Unit)
        is RestResponse.Error -> Result.failure(response.throwable)
    }
}

fun <T> Flow<Result<T>>.onSuccess(action: suspend (value: T) -> Unit): Flow<Result<T>> {
    return map { result ->
        result.onSuccess { value ->
            action(value)
        }
        return@map result
    }
}

fun <T> RestResponse<T>.toResult(): Result<Unit> {
    return when (this) {
        is RestResponse.Success -> Result.success(Unit)
        is RestResponse.Error -> Result.failure(this.throwable)
    }
}

fun <T> Flow<Result<T>>.onError(action: suspend (value: Throwable) -> Unit): Flow<Result<T>> {
    return map { result ->
        result.onFailure { error ->
            action(error)
        }
        return@map result
    }
}

