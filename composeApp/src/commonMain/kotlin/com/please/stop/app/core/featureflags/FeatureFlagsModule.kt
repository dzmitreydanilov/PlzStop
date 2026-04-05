package com.please.stop.app.core.featureflags

import androidx.lifecycle.DefaultLifecycleObserver
import com.please.stop.app.core.DataWarmUpObserver
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val featureFlagsModule = module {
    single<FeatureFlags> {
        FeatureFlagsImpl(
            remoteConfigDataSource = get(),
        )
    }
    single {
        FeatureFlagsLifecycleObserver(
            featureFlags = get(),
            scopeProvider = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    } bind DefaultLifecycleObserver::class
    single {
        DataWarmUpObserver(
            currencyRepository = get(),
            scopeProvider = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    } bind DefaultLifecycleObserver::class
}
