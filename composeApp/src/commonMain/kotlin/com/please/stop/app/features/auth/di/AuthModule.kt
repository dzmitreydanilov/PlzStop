package com.please.stop.app.features.auth.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.auth.data.repository.AuthRepositoryImpl
import com.please.stop.app.features.auth.domain.repository.AuthRepository
import com.please.stop.app.features.auth.domain.usecase.ConnectGoogleAccountUseCase
import com.please.stop.app.features.auth.domain.usecase.DeleteAccountUseCase
import com.please.stop.app.features.auth.domain.usecase.LogoutUseCase
import com.please.stop.app.features.auth.domain.usecase.ObserveAuthStateUseCase
import com.please.stop.app.features.auth.domain.usecase.SignInWithAppleUseCase
import com.please.stop.app.features.auth.domain.usecase.SignInWithGoogleUseCase
import com.please.stop.app.features.auth.presentation.AuthStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authModule = module {

    single<AuthRepository> {
        AuthRepositoryImpl(
            firebaseAuthProvider = get(),
            googleAccountStorage = get(),
        )
    }

    factory {
        SignInWithGoogleUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        SignInWithAppleUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory { ObserveAuthStateUseCase(repository = get()) }

    factory {
        ConnectGoogleAccountUseCase(
            googleAccountStorage = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        LogoutUseCase(
            googleAccountStorage = get(),
            bearerTokenClearer = get(),
            firebaseAuthProvider = get(),
            googleAuthProvider = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        DeleteAccountUseCase(
            firebaseAuthProvider = get(),
            googleAccountStorage = get(),
            bearerTokenClearer = get(),
            googleAuthProvider = get(),
            appDatabase = get<AppDatabase>(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel {
        AuthStateHolder(
            signInWithGoogleUseCase = get(),
            signInWithAppleUseCase = get(),
        )
    }
}
