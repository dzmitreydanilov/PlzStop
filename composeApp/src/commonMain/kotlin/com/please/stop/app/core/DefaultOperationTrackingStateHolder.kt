package com.please.stop.app.core

import kotlinx.coroutines.flow.Flow
import com.please.stop.app.core.models.domain.Result
import kotlinx.coroutines.flow.emptyFlow

/**
 * Default implementation of OperationTrackingStateHolder that can be used with delegation.
 */
class DefaultOperationTrackingStateHolder<S, E, T : LastAction>(
    private val operationTracker: ILastActionTracker<T> = ActionTracker(),
    private val eventMapper: (E) -> T? = { null },
    private val retryHandler: (T?) -> Flow<Result> = { emptyFlow() }
) : LatestOperationTracker<S, E, T>, ILastActionTracker<T> by operationTracker {

    override fun mapEventToOperation(event: E): T? = eventMapper(event)

    override fun handleRetry(action: T?): Flow<Result> = retryHandler(action)
}
