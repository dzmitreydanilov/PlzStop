package com.please.stop.app.features.home.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.home.data.repository.HomeRepositoryImpl
import com.please.stop.app.features.home.domain.repository.HomeRepository
import com.please.stop.app.features.home.domain.usecase.AddCategoryUseCase
import com.please.stop.app.features.home.domain.usecase.ObserveHomeDataUseCase
import com.please.stop.app.features.home.presentation.HomeStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val homeModule = module {

    single<HomeRepository> {
        HomeRepositoryImpl(
            userProfileDao = get<AppDatabase>().userProfileDao(),
            categoryDao = get<AppDatabase>().categoryDao(),
            expenseDao = get<AppDatabase>().expenseDao(),
            currencyRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        ObserveHomeDataUseCase(
            homeRepository = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        AddCategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel {
        HomeStateHolder(
            observeHomeDataUseCase = get(),
            addCategoryUseCase = get(),
        )
    }
}
