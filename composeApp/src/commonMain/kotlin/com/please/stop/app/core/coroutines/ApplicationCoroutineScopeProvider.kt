package com.please.stop.app.core.coroutines

import com.please.stop.app.core.logger.logError
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren

private const val APPLICATION_COROUTINE_SCOPE_MESSAGE =
    "Application Coroutine Scope Uncaught Exceptions"

class ApplicationCoroutineScopeProvider : ICoroutineScopeProvider {

    private val handler = CoroutineExceptionHandler { _, exception ->
        logError(exception, APPLICATION_COROUTINE_SCOPE_MESSAGE)
    }

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + handler)

    override fun getScope(): CoroutineScope = scope

    override fun cancelChildren() {
        scope.coroutineContext.cancelChildren()
    }

    override fun cancelScope() {
        scope.cancel()
    }
}
