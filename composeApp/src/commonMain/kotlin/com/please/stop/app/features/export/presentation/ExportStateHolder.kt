package com.please.stop.app.features.export.presentation

import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.auth.domain.usecase.ConnectGoogleAccount
import com.please.stop.app.features.auth.domain.usecase.ConnectGoogleAccountUseCase
import com.please.stop.app.features.auth.google.GoogleSheetsAuthorizationCode
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.features.export.domain.usecase.ExportCsvUseCase
import com.please.stop.app.features.export.domain.usecase.ExportExpensesAvailabilityResult
import com.please.stop.app.features.export.domain.usecase.ExportResult
import com.please.stop.app.features.export.domain.usecase.GoogleSpreadSheetExportResult
import com.please.stop.app.features.export.domain.usecase.GoogleSpreadSheetExportUseCase
import com.please.stop.app.features.export.domain.usecase.HasExpensesToExportUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import com.please.stop.app.core.models.domain.Result as DomainResult

class ExportStateHolder(
    private val exportToGoogleSheetsUseCase: GoogleSpreadSheetExportUseCase,
    private val exportCSVUseCase: ExportCsvUseCase,
    private val connectGoogleAccountUseCase: ConnectGoogleAccountUseCase,
    private val hasExpensesToExportUseCase: HasExpensesToExportUseCase,
) : StateHolder<ExportState, ExportEvent>() {

    override val tag = "ExportStateHolder"

    override fun getInitial(): ExportState = ExportState.Idle()

    override suspend fun bootstrap(emit: suspend (DomainResult) -> Unit) {
        return hasExpensesToExportUseCase().collect { emit(it) }
    }

    override fun resolveEventResult(event: ExportEvent): Flow<DomainResult> {
        return when (event) {
            is ExportEvent.StartExport -> {
                handleExportTapped(
                    startDateMillis = event.startDateMillis,
                    endDateMillis = event.endDateMillis,
                )
            }

            is ExportEvent.FileNameEntered -> {
                flowOf(FileNameChange(event.fileName))
            }

            is ExportEvent.FolderNameEntered -> {
                flowOf(FolderNameChange(event.folderName))
            }

            is ExportEvent.DestinationSelected -> {
                flowOf(DestinationUpdated(event.destination))
            }

            is ExportEvent.TabLayoutSelected -> {
                flowOf(TabLayoutSelectedResult(event.spreadSheetFormat))
            }

            is ExportEvent.DateRangeSelected -> {
                flowOf(
                    DateRangeUpdated(
                        startDateMillis = event.startDateMillis,
                        endDateMillis = event.endDateMillis,
                    )
                )
            }

            is ExportEvent.GoogleAccountConnected -> {
                connectAndExport(event.authorizationCode)
            }

            ExportEvent.AuthenticationCompleted -> resumeExportAfterAuthentication()

            ExportEvent.DismissAuthentication,
            ExportEvent.DismissError,
            ExportEvent.Dismiss -> flowOf(Dismissed)
        }
    }

    private fun handleExportTapped(startDateMillis: Long, endDateMillis: Long): Flow<DomainResult> {
        return when (state.value.currentDestination) {
            ExportDestination.GOOGLE_SHEETS -> exportToGoogleSheetsUseCase(
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                spreadSheetFormat = state.value.currentSpreadSheetFormat,
                spreadsheetTitle = state.value.fileName.orEmpty(),
                folderName = state.value.folderName.orEmpty(),
            )

            ExportDestination.CSV ->
                exportCSVUseCase(
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                )
        }
    }

    private fun connectAndExport(
        authorizationCode: GoogleSheetsAuthorizationCode,
    ): Flow<DomainResult> = flow {
        val connectionResult = connectGoogleAccountUseCase(authorizationCode)
        emit(connectionResult)
        if (connectionResult is ConnectGoogleAccount.Success) {
            emitAll(
                exportToGoogleSheetsUseCase(
                    startDateMillis = state.value.currentStartDateMillis,
                    endDateMillis = state.value.currentEndDateMillis,
                    spreadSheetFormat = state.value.currentSpreadSheetFormat,
                    spreadsheetTitle = state.value.fileName.orEmpty(),
                    folderName = state.value.folderName.orEmpty(),
                )
            )
        }
    }

    private fun resumeExportAfterAuthentication(): Flow<DomainResult> = flow {
        emit(Dismissed)
        emitAll(
            exportToGoogleSheetsUseCase(
                startDateMillis = state.value.currentStartDateMillis,
                endDateMillis = state.value.currentEndDateMillis,
                spreadSheetFormat = state.value.currentSpreadSheetFormat,
                spreadsheetTitle = state.value.fileName.orEmpty(),
                folderName = state.value.folderName.orEmpty(),
            )
        )
    }

    override fun getStateByResult(previous: ExportState, result: DomainResult): ExportState =
        when (result) {
            ConnectGoogleAccount.Success -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = false,
            )

            ConnectGoogleAccount.ReconnectRequired -> ExportState.NeedsGoogleAccount(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = true,
            )

            GoogleSpreadSheetExportResult.GoogleAccountNotLinked -> {
                ExportState.NeedsGoogleAccount(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentDestination = previous.currentDestination,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis,
                    fileName = previous.fileName,
                    folderName = previous.folderName,
                    hasExpensesToExport = previous.hasExpensesToExport,
                    forceGoogleConsent = false,
                )
            }

            GoogleSpreadSheetExportResult.AuthenticationRequired -> {
                ExportState.AuthenticationRequired(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentDestination = previous.currentDestination,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis,
                    fileName = previous.fileName,
                    folderName = previous.folderName,
                    hasExpensesToExport = previous.hasExpensesToExport,
                    forceGoogleConsent = previous.forceGoogleConsent,
                )
            }

            is ExportExpensesAvailabilityResult -> {
                ExportState.Idle(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentDestination = previous.currentDestination,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis,
                    fileName = state.value.fileName,
                    folderName = state.value.folderName,
                    hasExpensesToExport = result is ExportExpensesAvailabilityResult.Available,
                    forceGoogleConsent = previous.forceGoogleConsent,
                )
            }

            is DestinationUpdated -> ExportState.Idle(
                currentSpreadSheetFormat = getSheetFormat(result, previous),
                currentDestination = result.destination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = previous.forceGoogleConsent,
            )

            is FileNameChange -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = result.name,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = previous.forceGoogleConsent,
            )

            is FolderNameChange -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                folderName = result.name,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = previous.forceGoogleConsent,
            )

            is TabLayoutSelectedResult -> ExportState.Idle(
                currentSpreadSheetFormat = result.spreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = previous.forceGoogleConsent,
            )

            is DateRangeUpdated -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = result.startDateMillis,
                currentEndDateMillis = result.endDateMillis,
                fileName = previous.fileName,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = previous.forceGoogleConsent,
            )

            Dismissed -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = false,
            )

            is ExportResult.Enqueued,
            GoogleSpreadSheetExportResult.Enqueued -> ExportState.Enqueued(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = false,
            )

            is ExportResult.CsvShareLaunched -> ExportState.CsvShareLaunched(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = ExportDestination.CSV,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                folderName = previous.folderName,
                hasExpensesToExport = previous.hasExpensesToExport,
                forceGoogleConsent = false,
            )

            is ExportResult.GoogleAccountNotLinked -> {
                ExportState.NeedsGoogleAccount(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentDestination = previous.currentDestination,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis,
                    fileName = previous.fileName,
                    folderName = previous.folderName,
                    hasExpensesToExport = previous.hasExpensesToExport,
                    forceGoogleConsent = false,
                )
            }

            else -> super.getStateByResult(previous, result)
        }

    private fun getSheetFormat(
        result: DestinationUpdated,
        previous: ExportState,
    ): SpreadSheetFormat = if (result.destination == ExportDestination.CSV) {
        SpreadSheetFormat.SINGLE_TAB
    } else {
        previous.currentSpreadSheetFormat
    }

    override fun getErrorStateByResult(result: DomainResult, errorType: ErrorType): ExportState =
        ExportState.Error(
            errorType = errorType,
            currentSpreadSheetFormat = state.value.currentSpreadSheetFormat,
            currentDestination = state.value.currentDestination,
            currentStartDateMillis = state.value.currentStartDateMillis,
            currentEndDateMillis = state.value.currentEndDateMillis,
            fileName = state.value.fileName,
            folderName = state.value.folderName,
            hasExpensesToExport = state.value.hasExpensesToExport,
            forceGoogleConsent = state.value.forceGoogleConsent,
        )
}

private data class DateRangeUpdated(val startDateMillis: Long, val endDateMillis: Long) :
    DomainResult

private data class TabLayoutSelectedResult(val spreadSheetFormat: SpreadSheetFormat) : DomainResult
private data object Dismissed : DomainResult
private data class DestinationUpdated(val destination: ExportDestination) : DomainResult
private data class FileNameChange(val name: String) : DomainResult
private data class FolderNameChange(val name: String) : DomainResult
