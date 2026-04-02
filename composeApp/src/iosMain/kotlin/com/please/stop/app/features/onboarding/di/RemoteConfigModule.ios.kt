package com.please.stop.app.features.onboarding.di

import com.please.stop.app.features.onboarding.data.repository.IosRemoteConfigDataSource
import com.please.stop.app.features.onboarding.data.repository.RemoteConfigDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val remoteConfigModule: Module = module {
    single<RemoteConfigDataSource> { IosRemoteConfigDataSource() }
}
