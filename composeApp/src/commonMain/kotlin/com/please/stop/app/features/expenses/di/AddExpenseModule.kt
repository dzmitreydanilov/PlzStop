package com.please.stop.app.features.expenses.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.expenses.create.presentation.CreateExpenseStateHolder
import com.please.stop.app.features.expenses.data.remote.ExchangeRateApiService
import com.please.stop.app.features.expenses.data.repository.AddExpenseRepositoryImpl
import com.please.stop.app.features.expenses.data.repository.ExchangeRateRepositoryImpl
import com.please.stop.app.features.expenses.data.repository.ReceiptRepositoryImpl
import com.please.stop.app.features.expenses.data.repository.ReceiptSaveRepositoryImpl
import com.please.stop.app.features.expenses.domain.repository.AddExpenseRepository
import com.please.stop.app.features.expenses.domain.repository.ExchangeRateRepository
import com.please.stop.app.features.expenses.domain.repository.ReceiptRepository
import com.please.stop.app.features.expenses.domain.repository.ReceiptSaveRepository
import com.please.stop.app.features.expenses.domain.usecase.AnalyzeReceiptUseCase
import com.please.stop.app.features.expenses.domain.usecase.FetchExchangeRateUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveExpenseUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveReceiptExpensesUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.DeleteExpenseUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.GetExpenseByIdUseCase
import com.please.stop.app.features.expenses.edit.presentation.EditExpenseStateHolder
import com.please.stop.app.features.expenses.receiptitems.ReceiptItemsArgsHolder
import com.please.stop.app.features.expenses.receiptitems.presentation.ReceiptItemsStateHolder
import com.please.stop.app.network.configureContent
import com.please.stop.app.network.contentEncoding
import com.please.stop.app.network.httpEngine
import com.please.stop.app.network.logging
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val addExpenseModule = module {

    single { ReceiptItemsArgsHolder() }

    single<ReceiptRepository> {
        ReceiptRepositoryImpl(
            callableFunctions = get(),
        )
    }

    single<ReceiptSaveRepository> {
        ReceiptSaveRepositoryImpl(
            receiptDao = get<AppDatabase>().receiptDao(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    single<AddExpenseRepository> {
        AddExpenseRepositoryImpl(
            userProfileDao = get<AppDatabase>().userProfileDao(),
            categoryDao = get<AppDatabase>().categoryDao(),
            expenseDao = get<AppDatabase>().expenseDao(),
            currencyRepository = get(),
            subcategoryRepository = get(),
            featureFlags = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    single(named("exchangeRate")) {
        HttpClient(httpEngine) {
            expectSuccess = true
            logging()
            contentEncoding()
            configureContent(json = get())
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
            defaultRequest {
                url("https://api.frankfurter.app/")
            }
        }
    }

    factory {
        ExchangeRateApiService(httpClient = get(named("exchangeRate")))
    }

    factory<ExchangeRateRepository> {
        ExchangeRateRepositoryImpl(apiService = get())
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

    factory {
        FetchExchangeRateUseCase(
            repository = get(),
            featureFlags = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        SaveReceiptExpensesUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel { params ->
        CreateExpenseStateHolder(
            preselectedCategoryId = params.getOrNull(),
            observeFormDataUseCase = get(),
            saveExpenseUseCase = get(),
            analyzeReceiptUseCase = get(),
            fetchExchangeRateUseCase = get(),
            receiptItemsArgsHolder = get(),
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
            fetchExchangeRateUseCase = get(),
            receiptItemsArgsHolder = get(),
        )
    }

    viewModel { params ->
        ReceiptItemsStateHolder(
            route = params.get(),
            argsHolder = get(),
            saveReceiptExpensesUseCase = get(),
            observeFormDataUseCase = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }
}
