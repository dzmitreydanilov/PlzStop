package com.please.stop.app.features.export.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.export.data.CsvExportBuilder
import com.please.stop.app.features.export.data.ExportWorkRunner
import com.please.stop.app.features.export.data.repository.CSVExportRepository
import com.please.stop.app.features.export.data.repository.GoogleSheetExportRepository
import com.please.stop.app.features.export.domain.usecase.CheckGoogleAccountLinkageUseCase
import com.please.stop.app.features.export.domain.usecase.CheckNotificationPermissionUseCase
import com.please.stop.app.features.export.domain.usecase.ExportCsvUseCase
import com.please.stop.app.features.export.domain.usecase.GoogleSpreadSheetExportUseCase
import com.please.stop.app.features.export.domain.usecase.HasExpensesToExportUseCase
import com.please.stop.app.features.export.presentation.ExportStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val exportModule = module {

    factory {
        val db = get<AppDatabase>()
        CSVExportRepository(
            expenseDao = db.expenseDao(),
            categoryDao = db.categoryDao(),
            subcategoryDao = db.subcategoryDao(),
            userProfileDao = db.userProfileDao(),
            documentSharer = get(),
            csvExportBuilder = get(),
        )
    }

    factory {
        val db = get<AppDatabase>()
        GoogleSheetExportRepository(
            exportHistoryDao = db.exportHistoryDao(),
            exportWorkerScheduler = get(),
        )
    }

    factory { CsvExportBuilder() }

    factory {
        val database = get<AppDatabase>()
        ExportWorkRunner(
            expenseDao = database.expenseDao(),
            categoryDao = database.categoryDao(),
            subcategoryDao = database.subcategoryDao(),
            userProfileDao = database.userProfileDao(),
            callableFunctions = get(),
            fcmTokenProvider = get(),
            exportHistoryDao = database.exportHistoryDao(),
        )
    }

    factory {
        CheckGoogleAccountLinkageUseCase(
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        CheckNotificationPermissionUseCase(
            notificationPermission = get(),
        )
    }

    factory {
        ExportCsvUseCase(
            repository = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        val database = get<AppDatabase>()
        HasExpensesToExportUseCase(
            expenseDao = database.expenseDao(),
        )
    }

    factory {
        GoogleSpreadSheetExportUseCase(
            hasGoogleAccountLinkageResult = get(),
            hasNotificationPermissionResult = get(),
            googleSheetExportRepository = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }



    viewModel {
        ExportStateHolder(
            connectGoogleAccountUseCase = get(),
            exportToGoogleSheetsUseCase = get(),
            exportCSVUseCase = get(),
            hasExpensesToExportUseCase = get(),
        )
    }
}
