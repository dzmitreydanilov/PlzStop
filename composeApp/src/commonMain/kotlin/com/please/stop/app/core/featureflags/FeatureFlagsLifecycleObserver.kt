package com.please.stop.app.core.featureflags

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.please.stop.app.core.coroutines.ICoroutineScopeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class FeatureFlagsLifecycleObserver(
    private val featureFlags: FeatureFlags,
    private val scopeProvider: ICoroutineScopeProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        refresh()
    }

    fun refresh() {
        scopeProvider.getScope().launch(ioDispatcher) {
            featureFlags.refresh()
        }
    }
}
