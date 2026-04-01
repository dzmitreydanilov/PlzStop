package com.please.stop.app.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.please.stop.app.core.models.domain.Result

/**
 * Extension function to safely handle retry with proper type casting.
 * This addresses the type casting issue when working with sealed interfaces.
 */
inline fun <reified R : LastAction> ILastActionTracker<*>.withOperation(
    crossinline block: (R) -> Flow<Result>
): Flow<Result> {
    val operation = lastAction
    return if (operation is R) {
        block(operation)
    } else {
        emptyFlow()
    }
}

/**
 * Extension function to handle retry with a when expression that properly handles type casting.
 */
inline fun <T : LastAction> ILastActionTracker<T>.handleRetryWithWhen(
    crossinline block: (T?) -> Flow<Result>
): Flow<Result> {
    return block(lastAction)
}
