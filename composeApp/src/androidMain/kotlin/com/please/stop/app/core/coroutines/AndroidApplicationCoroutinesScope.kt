package com.please.stop.app.core.coroutines

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AndroidApplicationCoroutinesScope(
    private val delegate: ApplicationCoroutineScopeProvider = ApplicationCoroutineScopeProvider(),
) : DefaultLifecycleObserver,
    ICoroutineScopeProvider by delegate {

    override fun onDestroy(owner: LifecycleOwner) {
        super.onStop(owner)
        cancelScope()
    }
}
