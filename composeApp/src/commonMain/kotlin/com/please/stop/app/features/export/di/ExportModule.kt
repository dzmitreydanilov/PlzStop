package com.please.stop.app.features.export.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.export.data.repository.ExportRepositoryImpl
import com.please.stop.app.features.export.domain.repository.ExportRepository
import com.please.stop.app.features.export.domain.usecase.ExportToSheetsUseCase
import com.please.stop.app.features.export.presentation.ExportStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val exportModule = module {

    single<ExportRepository> {
        ExportRepositoryImpl(
            expenseDao = get<AppDatabase>().expenseDao(),
            exportHistoryDao = get<AppDatabase>().exportHistoryDao(),
            notificationPermission = get(),
            exportWorkerScheduler = get(),
        )
    }

    factory {
        ExportToSheetsUseCase(
            repository = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel {
        ExportStateHolder(
            exportToSheetsUseCase = get(),
            connectGoogleAccountUseCase = get(),
        )
    }
}
