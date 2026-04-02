package com.please.stop.app.features.addexpense.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.addexpense.data.repository.AddExpenseRepositoryImpl
import com.please.stop.app.features.addexpense.domain.repository.AddExpenseRepository
import com.please.stop.app.features.addexpense.domain.usecase.DeleteExpenseUseCase
import com.please.stop.app.features.addexpense.domain.usecase.GetExpenseByIdUseCase
import com.please.stop.app.features.addexpense.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.addexpense.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.features.addexpense.presentation.AddExpenseStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val addExpenseModule = module {

    single<AddExpenseRepository> {
        AddExpenseRepositoryImpl(
            userProfileDao = get<AppDatabase>().userProfileDao(),
            categoryDao = get<AppDatabase>().categoryDao(),
            expenseDao = get<AppDatabase>().expenseDao(),
            currencyRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        ObserveAddExpenseFormDataUseCase(
            repository = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        GetExpenseByIdUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        SaveExpenseUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        DeleteExpenseUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel { params ->
        AddExpenseStateHolder(
            expenseId = params.get(),
            preselectedCategoryId = params.get(),
            observeFormDataUseCase = get(),
            getExpenseByIdUseCase = get(),
            saveExpenseUseCase = get(),
            deleteExpenseUseCase = get(),
        )
    }
}
