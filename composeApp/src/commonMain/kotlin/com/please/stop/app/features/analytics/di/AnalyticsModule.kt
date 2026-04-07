package com.please.stop.app.features.analytics.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.analytics.data.repository.AnalyticsRepositoryImpl
import com.please.stop.app.features.analytics.domain.repository.AnalyticsRepository
import com.please.stop.app.features.analytics.domain.usecase.LoadDayExpensesUseCase
import com.please.stop.app.features.analytics.domain.usecase.ObserveAnalyticsDataUseCase
import com.please.stop.app.features.analytics.presentation.AnalyticsStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val analyticsModule = module {

    single<AnalyticsRepository> {
        AnalyticsRepositoryImpl(
            userProfileDao = get<AppDatabase>().userProfileDao(),
            categoryDao = get<AppDatabase>().categoryDao(),
            subcategoryDao = get<AppDatabase>().subcategoryDao(),
            expenseDao = get<AppDatabase>().expenseDao(),
            featureFlags = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        ObserveAnalyticsDataUseCase(
            analyticsRepository = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        LoadDayExpensesUseCase(
            analyticsRepository = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel {
        AnalyticsStateHolder(
            observeAnalyticsDataUseCase = get(),
            loadDayExpensesUseCase = get(),
        )
    }
}
