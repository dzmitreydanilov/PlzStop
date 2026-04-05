package com.please.stop.app.features.expenses.monthly.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.expenses.data.repository.MonthlyExpensesRepositoryImpl
import com.please.stop.app.features.expenses.domain.repository.MonthlyExpensesRepository
import com.please.stop.app.features.expenses.domain.usecase.ObserveMonthlyExpensesUseCase
import com.please.stop.app.features.expenses.monthly.presentation.MonthlyExpensesStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val monthlyExpensesModule = module {

    single<MonthlyExpensesRepository> {
        MonthlyExpensesRepositoryImpl(
            expenseDao = get<AppDatabase>().expenseDao(),
            categoryDao = get<AppDatabase>().categoryDao(),
            receiptDao = get<AppDatabase>().receiptDao(),
            userProfileDao = get<AppDatabase>().userProfileDao(),
            currencyRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        ObserveMonthlyExpensesUseCase(
            repository = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel {
        MonthlyExpensesStateHolder(
            observeMonthlyExpensesUseCase = get(),
        )
    }
}
