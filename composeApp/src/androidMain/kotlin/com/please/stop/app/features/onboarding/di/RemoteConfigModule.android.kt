package com.please.stop.app.features.onboarding.di

import com.please.stop.app.features.onboarding.data.repository.FirebaseRemoteConfigDataSource
import com.please.stop.app.features.onboarding.data.repository.RemoteConfigDataSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val remoteConfigModule: Module = module {
    single { Firebase.remoteConfig }
    single<RemoteConfigDataSource> { FirebaseRemoteConfigDataSource(remoteConfig = get()) }
}
