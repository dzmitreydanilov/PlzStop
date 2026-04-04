package com.please.stop.app.di

import com.please.stop.app.di.dispatchers.dispatchersModule
import com.please.stop.app.features.expenses.di.addExpenseModule
import com.please.stop.app.features.analytics.di.analyticsModule
import com.please.stop.app.features.home.di.homeModule
import com.please.stop.app.features.onboarding.di.onboardingModule
import org.koin.dsl.module

internal val appModule = module {
    includes(
        platformModule,
        dispatchersModule,
        databaseModule,
        onboardingModule,
        homeModule,
        analyticsModule,
        addExpenseModule,
    )
}
