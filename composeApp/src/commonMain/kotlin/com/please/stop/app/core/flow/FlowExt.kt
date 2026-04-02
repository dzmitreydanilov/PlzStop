package com.please.stop.app.core.flow

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.network.RestResponse
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

fun <T> concat(flow1: Flow<T>, flow2: Flow<T>): Flow<T> = flow {
    emitAll(flow1)
    emitAll(flow2)
}

fun <S, R> Flow<Result<S>>.mapToDomainResult(
    onFailure: (ErrorType) -> R
): Flow<R> where S : R {
    return this.map { result ->
        result.fold(
            onSuccess = { successValue -> successValue },
            onFailure = { throwable -> onFailure(throwable.toErrorType()) }
        )
    }
}

/**
 * Combines three flows of Results. If any result is a failure, the combination returns a failure.
 * Otherwise, it transforms the successful values into a new success result.
 */
fun <T1, T2, T3, R> combineResults(
    flow1: Flow<Result<T1>>,
    flow2: Flow<Result<T2>>,
    flow3: Flow<Result<T3>>,
    transform: (T1, T2, T3) -> R
): Flow<Result<R>> {
    return combine(flow1, flow2, flow3) { r1, r2, r3 ->
        // Find the first exception, if any
        val firstException = listOf(r1, r2, r3).firstNotNullOfOrNull { it.exceptionOrNull() }

        if (firstException != null) {
            Result.failure(firstException)
        } else {
            // All results are successful, so getOrThrow is safe
            Result.success(transform(r1.getOrThrow(), r2.getOrThrow(), r3.getOrThrow()))
        }
    }
}

/**
 * Overloaded version for two flows.
 */
fun <T1, T2, R> combineResults(
    flow1: Flow<Result<T1>>,
    flow2: Flow<Result<T2>>,
    transform: (T1, T2) -> R
): Flow<Result<R>> {
    return combine(flow1, flow2) { r1, r2 ->
        val firstException = r1.exceptionOrNull() ?: r2.exceptionOrNull()

        if (firstException != null) {
            Result.failure(firstException)
        } else {
            Result.success(transform(r1.getOrThrow(), r2.getOrThrow()))
        }
    }
}

inline fun <reified T1, reified T2, reified R> combineRequests(
    flow1: Flow<RestResponse<T1>>,
    flow2: Flow<RestResponse<T2>>,
    crossinline transform: (T1, T2) -> R
): Flow<Result<R>> {
    return combine(flow1, flow2) { response1, response2 ->
        if (response1 is RestResponse.Error) {
            return@combine Result.failure(response1.throwable)
        }

        if (response2 is RestResponse.Error) {
            return@combine Result.failure(response2.throwable)
        }

        val transformation = transform(
            (response1 as RestResponse.Success<T1>).body,
            (response2 as RestResponse.Success<T2>).body
        )

        Result.success(transformation)
    }
}

fun <T> flowFromSuspend(function: suspend () -> T): Flow<T> = FlowFromSuspend(function)

private class FlowFromSuspend<T>(private val function: suspend () -> T) : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        val value = function()
        currentCoroutineContext().ensureActive()
        collector.emit(value)
    }
}
