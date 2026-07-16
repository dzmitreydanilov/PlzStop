package com.please.stop.app.features.auth.google

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PendingAuthorizationResultTest {

    @Test
    fun completedResultIsDeliveredAndCleared() = runTest {
        val pendingResult = PendingAuthorizationResult<String>()
        val result = async {
            pendingResult.launchAndAwait {
                assertTrue(pendingResult.complete("authorization-result"))
            }
        }

        assertEquals("authorization-result", result.await())
        assertFalse(pendingResult.hasPendingResult())
    }

    @Test
    fun cancelledCallerClearsPendingResultAndRejectsLateCallback() = runTest {
        val pendingResult = PendingAuthorizationResult<String>()
        val result = async {
            pendingResult.launchAndAwait { }
        }
        runCurrent()

        assertTrue(pendingResult.hasPendingResult())
        result.cancelAndJoin()

        assertFalse(pendingResult.hasPendingResult())
        assertFalse(pendingResult.complete("stale-result"))
    }
}
