package com.please.stop.app.core.coroutines

import kotlinx.coroutines.CoroutineScope

interface ICoroutineScopeProvider {
    fun getScope(): CoroutineScope
    fun cancelChildren()
    fun cancelScope()
}
