package com.please.stop.app.features.analytics.di

import com.please.stop.app.features.analytics.presentation.AnalyticsStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val analyticsModule = module {

    viewModel {
        AnalyticsStateHolder(
            observeHomeDataUseCase = get(),
        )
    }
}
