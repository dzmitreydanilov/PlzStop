package com.please.stop.app.features.onboarding.di

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.please.stop.app.features.onboarding.data.repository.FirebaseRemoteConfigDataSource
import com.please.stop.app.features.onboarding.data.repository.RemoteConfigDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val remoteConfigModule: Module = module {
    single { Firebase.remoteConfig }
    single<RemoteConfigDataSource> { FirebaseRemoteConfigDataSource(remoteConfig = get()) }
}
