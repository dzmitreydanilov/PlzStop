package com.please.stop.app.core

import com.please.stop.app.core.models.domain.Result
import kotlinx.coroutines.flow.Flow

/**
 * A mixin trait that adds operation tracking capabilities to any StateHolder.
 * This allows for tracking the last operation and implementing retry functionality.
 *
 * @param S The state type
 * @param E The event type
 * @param T The operation type that extends LastOperation
 */
interface LatestOperationTracker<S, E, T : LastAction> : ILastActionTracker<T> {

    /**
     * Maps an event to an operation. Override this to define how events map to operations.
     * Return null if the event doesn't correspond to any operation.
     */
    fun mapEventToOperation(event: E): T?

    /**
     * Handles retry logic based on the last tracked operation.
     * Override this to define how retry should work for different operations.
     */
    fun handleRetry(action: T?): Flow<Result>
}
