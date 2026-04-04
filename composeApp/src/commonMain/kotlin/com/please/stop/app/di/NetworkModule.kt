package com.please.stop.app.di

import com.please.stop.app.core.buildSettings.AppBuildSettingsProvider
import com.please.stop.app.network.HttpClientDataConfig
import com.please.stop.app.network.JsonSerializer
import com.please.stop.app.network.createAuthFreeHttpClient
import com.please.stop.app.network.httpEngine
import org.koin.dsl.module

internal val networkModule = module {
    single { httpEngine }

    single { JsonSerializer }

    factory {
        val applicationBuildSettings: AppBuildSettingsProvider = get()
        createAuthFreeHttpClient(
            config = HttpClientDataConfig(
                engine = get(),
                json = get(),
                networkLoggingEnabled = true,
                retryEnabled = false,
                isDebug = applicationBuildSettings.isDebug()
            ),
        )
    }
}
