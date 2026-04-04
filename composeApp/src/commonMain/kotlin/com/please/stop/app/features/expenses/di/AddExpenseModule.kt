package com.please.stop.app.features.expenses.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.expenses.data.repository.AddExpenseRepositoryImpl
import com.please.stop.app.features.expenses.data.repository.ReceiptRepositoryImpl
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import com.please.stop.app.features.expenses.domain.repository.ReceiptRepository
import com.please.stop.app.features.expenses.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.DeleteExpenseUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.GetExpenseByIdUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.features.expenses.create.presentation.CreateExpenseStateHolder
import com.please.stop.app.features.expenses.edit.presentation.EditExpenseStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val addExpenseModule = module {

    single<ReceiptRepository> {
        ReceiptRepositoryImpl(
            callableFunctions = get(),
        )
    }

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

    factory {
        AnalyzeReceiptUseCase(
            receiptRepository = get(),
            addExpenseRepository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel { params ->
        CreateExpenseStateHolder(
            preselectedCategoryId = params.getOrNull(),
            observeFormDataUseCase = get(),
            saveExpenseUseCase = get(),
            analyzeReceiptUseCase = get(),
        )
    }

    viewModel { params ->
        EditExpenseStateHolder(
            expenseId = params.get(),
            observeFormDataUseCase = get(),
            getExpenseByIdUseCase = get(),
            saveExpenseUseCase = get(),
            deleteExpenseUseCase = get(),
            analyzeReceiptUseCase = get(),
        )
    }
}
