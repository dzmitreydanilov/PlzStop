package com.please.stop.app.core.featureflags

import org.koin.dsl.module

val featureFlagsModule = module {
    single<FeatureFlags> {
        FeatureFlagsImpl(
            remoteConfigDataSource = get(),
        )
    }
}
