package com.please.stop.app.di

import com.please.stop.app.core.DataWarmUpObserver
import com.please.stop.app.core.coroutines.ICoroutineScopeProvider
import com.please.stop.app.core.featureflags.FeatureFlagsLifecycleObserver
import com.please.stop.app.features.auth.data.IosFcmTokenBridge
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions
import com.please.stop.app.features.expenses.data.remote.IosFirebaseCallableFunctions
import com.please.stop.app.features.expenses.data.remote.IosFirebaseFunctionsCaller
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.module

fun createIosPlatformOverrides(
    firebaseFunctionsCaller: IosFirebaseFunctionsCaller,
    fcmTokenBridge: IosFcmTokenBridge,
): Module = module {
    single<FirebaseCallableFunctions> {
        IosFirebaseCallableFunctions(caller = firebaseFunctionsCaller)
    }
    single<IosFcmTokenBridge> { fcmTokenBridge }
}

class IosAppLifecycleHandler(
    private val featureFlagsObserver: FeatureFlagsLifecycleObserver,
    private val dataWarmUpObserver: DataWarmUpObserver,
    @Suppress("unused") private val scopeProvider: ICoroutineScopeProvider,
) {
    fun onAppBecameActive() {
        featureFlagsObserver.refresh()
        dataWarmUpObserver.warmUp()
    }

    fun onAppEnteredBackground() = Unit
}

val Koin.appLifecycleHandler: IosAppLifecycleHandler
    get() = get()
