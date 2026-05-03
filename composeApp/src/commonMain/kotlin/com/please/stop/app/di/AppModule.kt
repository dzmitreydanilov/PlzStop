package com.please.stop.app.di

import com.please.stop.app.core.featureflags.featureFlagsModule
import com.please.stop.app.di.dispatchers.dispatchersModule
import com.please.stop.app.di.kvs.dataStoreModule
import com.please.stop.app.features.analytics.di.analyticsModule
import com.please.stop.app.features.analytics.monthly.di.monthlyExpensesModule
import com.please.stop.app.features.auth.di.authModule
import com.please.stop.app.features.categories.di.categoriesModule
import com.please.stop.app.features.expenses.di.addExpenseModule
import com.please.stop.app.features.export.di.exportModule
import com.please.stop.app.features.home.di.homeModule
import com.please.stop.app.features.onboarding.di.onboardingModule
import com.please.stop.app.features.settings.di.settingsModule
import com.please.stop.app.features.subscriptions.di.subscriptionPromotionModule
import org.koin.dsl.module

internal val appModule = module {
    includes(
        platformModule,
        dispatchersModule,
        dataStoreModule,
        networkModule,
        databaseModule,
        featureFlagsModule,
        authModule,
        onboardingModule,
        homeModule,
        analyticsModule,
        addExpenseModule,
        monthlyExpensesModule,
        categoriesModule,
        exportModule,
        subscriptionPromotionModule,
        settingsModule,
    )
}
