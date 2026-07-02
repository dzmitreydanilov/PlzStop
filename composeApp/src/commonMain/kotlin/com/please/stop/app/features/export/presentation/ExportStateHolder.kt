package com.please.stop.app.features.export.presentation

import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.auth.domain.usecase.ConnectGoogleAccountUseCase
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.features.export.domain.usecase.ExportCsvUseCase
import com.please.stop.app.features.export.domain.usecase.ExportExpensesAvailabilityResult
import com.please.stop.app.features.export.domain.usecase.ExportResult
import com.please.stop.app.features.export.domain.usecase.GoogleSpreadSheetExportResult
import com.please.stop.app.features.export.domain.usecase.GoogleSpreadSheetExportUseCase
import com.please.stop.app.features.export.domain.usecase.HasExpensesToExportUseCase
import kotlinx.coroutines.flow.Flow
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

    override fun collectWhileSubscribed(): Flow<DomainResult> {
        return hasExpensesToExportUseCase()
    }

    override fun resolveEventResult(event: ExportEvent): Flow<DomainResult> {
        return when (event) {
            is ExportEvent.StartExport -> {
                handleExportTapped(
                    event.startDateMillis,
                    event.endDateMillis
                )
            }

            is ExportEvent.FileNameEntered -> {
                flowOf(FileNameChange(event.fileName))
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

            ExportEvent.ShareCsvOptionSelected -> {
                flowOf(ExportCvsSelected)
            }

            is ExportEvent.GoogleAccountConnected -> {
                flow {
                    emit(connectGoogleAccountUseCase(event.googleUser))
                }
            }

            ExportEvent.DismissError, ExportEvent.Dismiss -> flowOf(Dismissed)
        }
    }

    private fun handleExportTapped(startDateMillis: Long, endDateMillis: Long): Flow<DomainResult> {
        return when (state.value.currentDestination) {
            ExportDestination.GOOGLE_SHEETS -> exportToGoogleSheetsUseCase(
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
            )

            ExportDestination.CSV ->
                exportCSVUseCase(
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                )
        }
    }

    override fun getStateByResult(previous: ExportState, result: DomainResult): ExportState =
        when (result) {
            is GoogleSpreadSheetExportResult.GoogleAccountNotLinked -> {
                ExportState.NeedsGoogleAccount(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentDestination = previous.currentDestination,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis,
                    fileName = previous.fileName,
                    hasExpensesToExport = previous.hasExpensesToExport
                )
            }

            is ExportExpensesAvailabilityResult -> {
                ExportState.Idle(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentDestination = previous.currentDestination,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis,
                    fileName = state.value.fileName,
                    hasExpensesToExport = result is ExportExpensesAvailabilityResult.Available
                )
            }


            is ShowConfirm -> ExportState.Confirm(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = result.startDateMillis,
                currentEndDateMillis = result.endDateMillis,
                fileName = previous.fileName,
                hasExpensesToExport = previous.hasExpensesToExport
            )

            is DestinationUpdated -> ExportState.Idle(
                currentSpreadSheetFormat = getSheetFormat(result, previous),
                currentDestination = result.destination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                hasExpensesToExport = previous.hasExpensesToExport
            )

            is FileNameChange -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = result.name,
                hasExpensesToExport = previous.hasExpensesToExport
            )

            ExportCvsSelected -> {
                ExportState.Idle(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentDestination = ExportDestination.CSV,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis,
                    fileName = previous.fileName,
                    hasExpensesToExport = previous.hasExpensesToExport
                )
            }

            is TabLayoutSelectedResult -> ExportState.Idle(
                currentSpreadSheetFormat = result.spreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                hasExpensesToExport = previous.hasExpensesToExport
            )

            is DateRangeUpdated -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = result.startDateMillis,
                currentEndDateMillis = result.endDateMillis,
                fileName = previous.fileName,
                hasExpensesToExport = previous.hasExpensesToExport
            )

            is Dismissed -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                fileName = previous.fileName,
                hasExpensesToExport = previous.hasExpensesToExport

            )

            is ExportResult.Enqueued -> ExportState.Enqueued(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = previous.currentDestination,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                hasExpensesToExport = previous.hasExpensesToExport
            )

            is ExportResult.CsvShareLaunched -> ExportState.CsvShareLaunched(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentDestination = ExportDestination.CSV,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
                fileName = previous.fileName,
                hasExpensesToExport = previous.hasExpensesToExport
            )

            is ExportResult.GoogleAccountNotLinked -> {
                ExportState.NeedsGoogleAccount(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentDestination = previous.currentDestination,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis,
                    fileName = previous.fileName,
                    hasExpensesToExport = previous.hasExpensesToExport

                )
            }

            else -> super.getStateByResult(previous, result)
        }

    private fun getSheetFormat(
        result: DestinationUpdated,
        previous: ExportState
    ): SpreadSheetFormat = if (result.destination == ExportDestination.CSV) {
        SpreadSheetFormat.SINGLE_TAB
    } else {
        previous.currentSpreadSheetFormat
    }

    override fun getErrorStateByResult(result: DomainResult, errorType: ErrorType): ExportState =
        ExportState.Error(
            errorType,
            currentSpreadSheetFormat = state.value.currentSpreadSheetFormat,
            currentDestination = state.value.currentDestination,
            currentStartDateMillis = state.value.currentStartDateMillis,
            currentEndDateMillis = state.value.currentEndDateMillis,
            fileName = state.value.fileName,
            hasExpensesToExport = state.value.hasExpensesToExport
        )
}

data class ShowConfirm(
    val startDateMillis: Long,
    val endDateMillis: Long,
) : DomainResult

data object Dismissed : DomainResult
data class DestinationUpdated(val destination: ExportDestination) : DomainResult
data class TabLayoutSelectedResult(val spreadSheetFormat: SpreadSheetFormat) : DomainResult
data class FileNameChange(val name: String) : DomainResult

data object ExportCvsSelected : DomainResult
data class DateRangeUpdated(
    val startDateMillis: Long,
    val endDateMillis: Long,
) : DomainResult
