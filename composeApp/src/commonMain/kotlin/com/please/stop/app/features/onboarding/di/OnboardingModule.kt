package com.please.stop.app.features.onboarding.di

import com.please.stop.app.core.StateSaverImpl
import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.onboarding.data.repository.CategoryRepositoryImpl
import com.please.stop.app.features.onboarding.data.repository.CurrencyRepositoryImpl
import com.please.stop.app.features.onboarding.data.repository.OnboardingRepositoryImpl
import com.please.stop.app.features.onboarding.data.repository.SubcategoryRepositoryImpl
import com.please.stop.app.features.onboarding.domain.repository.CategoryRepository
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import com.please.stop.app.features.onboarding.domain.repository.OnboardingRepository
import com.please.stop.app.features.onboarding.domain.repository.SubcategoryRepository
import com.please.stop.app.features.onboarding.domain.usecase.BackfillCurrencyProfileUseCase
import com.please.stop.app.features.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.please.stop.app.features.onboarding.domain.usecase.DetectDeviceCurrencyUseCase
import com.please.stop.app.features.onboarding.domain.usecase.LoadOnboardingDataUseCase
import com.please.stop.app.features.onboarding.domain.usecase.ObserveOnboardingCompletedUseCase
import com.please.stop.app.features.onboarding.presentation.OnboardingState
import com.please.stop.app.features.onboarding.presentation.OnboardingStateHolder
import com.please.stop.app.presentation.RootStateHolder
import com.please.stop.app.uicomponents.sheets.currency.CurrencyPickerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal expect val remoteConfigModule: Module

val onboardingModule = module {
    includes(remoteConfigModule)

    single<CurrencyRepository> {
        CurrencyRepositoryImpl(
            remoteConfigDataSource = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    single<CategoryRepository> {
        CategoryRepositoryImpl(
            remoteConfigDataSource = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    single<OnboardingRepository> {
        OnboardingRepositoryImpl(
            userProfileDao = get<AppDatabase>().userProfileDao(),
            categoryDao = get<AppDatabase>().categoryDao(),
            categoryRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    single<SubcategoryRepository> {
        SubcategoryRepositoryImpl(
            subcategoryDao = get<AppDatabase>().subcategoryDao(),
            remoteConfigDataSource = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        LoadOnboardingDataUseCase(
            currencyRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    factory {
        DetectDeviceCurrencyUseCase(
            currencyRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    factory {
        CompleteOnboardingUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    factory { ObserveOnboardingCompletedUseCase(repository = get()) }
    factory {
        BackfillCurrencyProfileUseCase(
            userProfileDao = get<AppDatabase>().userProfileDao(),
            currencyRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel {
        CurrencyPickerViewModel(
            currencyRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
    viewModel { params ->
        OnboardingStateHolder(
            loadOnboardingDataUseCase = get(),
            detectDeviceCurrencyUseCase = get(),
            completeOnboardingUseCase = get(),
            stateSaver = StateSaverImpl(
                savedStateHandle = params.get(),
                serializer = OnboardingState.serializer(),
            ),
        )
    }
    viewModel {
        RootStateHolder(
            observeOnboardingCompletedUseCase = get(),
            backfillCurrencyProfileUseCase = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
}
