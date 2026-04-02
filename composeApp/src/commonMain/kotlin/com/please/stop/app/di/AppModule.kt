package com.please.stop.app.di

import com.please.stop.app.di.dispatchers.dispatchersModule
import com.please.stop.app.features.onboarding.di.onboardingModule
import org.koin.dsl.module

internal val appModule = module {
    includes(
        platformModule,
        dispatchersModule,
        databaseModule,
        onboardingModule,
    )
}
