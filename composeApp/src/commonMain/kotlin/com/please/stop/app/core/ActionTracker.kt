package com.please.stop.app.core

import kotlinx.coroutines.flow.MutableStateFlow

interface ILastActionTracker<out T : LastAction> {

    val lastAction: T?
    fun trackAction(operation: LastAction?)
}


class ActionTracker<T : LastAction> : ILastActionTracker<T> {
    private val _lastAction = MutableStateFlow<T?>(null)

    // Make this a getter that always returns the current value
    override val lastAction: T?
        get() = _lastAction.value

    @Suppress("UNCHECKED_CAST")
    override fun trackAction(operation: LastAction?) {
        _lastAction.value = operation as T?
    }
}

interface LastAction

data object DefaultAction : LastAction
