package com.please.stop.app.features.auth.google

import kotlinx.coroutines.CompletableDeferred

/** Holds one foreground authorization result and forgets it when the caller is cancelled. */
internal class PendingAuthorizationResult<T> {

    private var pendingResult: CompletableDeferred<T>? = null

    suspend fun launchAndAwait(launch: () -> Unit): T {
        check(pendingResult == null) { "An authorization request is already pending" }

        val result = CompletableDeferred<T>()
        pendingResult = result
        return try {
            launch()
            result.await()
        } finally {
            if (pendingResult === result) {
                pendingResult = null
            }
            result.cancel()
        }
    }

    fun complete(value: T): Boolean = pendingResult?.complete(value) ?: false

    internal fun hasPendingResult(): Boolean = pendingResult != null
}
