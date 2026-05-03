package com.please.stop.app.features.export.presentation

import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.auth.domain.usecase.ConnectGoogleAccountUseCase
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.features.export.domain.usecase.ExportResult
import com.please.stop.app.features.export.domain.usecase.ExportToSheetsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import com.please.stop.app.core.models.domain.Result as DomainResult

class ExportStateHolder(
    private val exportToSheetsUseCase: ExportToSheetsUseCase,
    private val connectGoogleAccountUseCase: ConnectGoogleAccountUseCase,
) : StateHolder<ExportState, ExportEvent>() {

    override val tag = "ExportStateHolder"


    override fun getInitial(): ExportState = ExportState.Idle()

    override fun resolveEventResult(event: ExportEvent): Flow<DomainResult> = when (event) {
        is ExportEvent.ExportTapped -> handleExportTapped(
            event.startDateMillis,
            event.endDateMillis
        )

        is ExportEvent.TabLayoutSelected -> {
            flowOf(InternalResult.TabLayoutUpdated(event.spreadSheetFormat))
        }

        is ExportEvent.DateRangeSelected -> flowOf(
            InternalResult.DateRangeUpdated(
                startDateMillis = event.startDateMillis,
                endDateMillis = event.endDateMillis,
            )
        )

        is ExportEvent.ConfirmExport -> handleConfirmExport(event.googleAccessToken)
        is ExportEvent.GoogleAccountConnected -> flow {
            emit(connectGoogleAccountUseCase(event.googleUser))
        }

        ExportEvent.DismissError -> flowOf(InternalResult.Dismissed)
        ExportEvent.Dismiss -> flowOf(InternalResult.Dismissed)
    }

    private fun handleExportTapped(startDateMillis: Long, endDateMillis: Long): Flow<DomainResult> {
        return flow {
            emit(InternalResult.ShowConfirm(state.value.currentSpreadSheetFormat))
        }
    }

    private fun handleConfirmExport(googleAccessToken: String): Flow<DomainResult> {
        return exportToSheetsUseCase(
            googleAccessToken = googleAccessToken,
            spreadSheetFormat = state.value.currentSpreadSheetFormat,
            startDateMillis = state.value.currentStartDateMillis,
            endDateMillis = state.value.currentEndDateMillis,
        )
    }

    override fun getStateByResult(previous: ExportState, result: DomainResult): ExportState =
        when (result) {
            is InternalResult.ShowConfirm -> ExportState.Confirm(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis
            )

            is InternalResult.TabLayoutUpdated -> ExportState.Idle(
                currentSpreadSheetFormat = result.spreadSheetFormat,
                currentStartDateMillis = previous.currentStartDateMillis,
                currentEndDateMillis = previous.currentEndDateMillis,
            )

            is InternalResult.DateRangeUpdated -> ExportState.Idle(
                currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                currentStartDateMillis = result.startDateMillis,
                currentEndDateMillis = result.endDateMillis,
            )

            is InternalResult.Dismissed -> ExportState.Idle(currentSpreadSheetFormat = previous.currentSpreadSheetFormat)
            is ExportResult.Enqueued -> ExportState.Enqueued(
                previous.currentSpreadSheetFormat, previous.currentStartDateMillis,
                previous.currentEndDateMillis
            )

            is ExportResult.GoogleAccountNotLinked -> {
                ExportState.NeedsGoogleAccount(
                    previous.currentSpreadSheetFormat,
                    previous.currentStartDateMillis,
                    previous.currentEndDateMillis
                )
            }

            is ExportResult.NotificationPermissionDenied -> {
                ExportState.NeedsNotificationPermission(
                    previous.currentSpreadSheetFormat,
                    previous.currentStartDateMillis,
                    previous.currentEndDateMillis
                )
            }

            is ExportResult.NoExpenses -> {
                ExportState.NoExpenses(
                    previous.currentSpreadSheetFormat,
                    previous.currentStartDateMillis,
                    previous.currentEndDateMillis
                )
            }

            is ConnectGoogleAccountUseCase.ConnectResult.Success -> {
                ExportState.Confirm(
                    currentSpreadSheetFormat = previous.currentSpreadSheetFormat,
                    currentStartDateMillis = previous.currentStartDateMillis,
                    currentEndDateMillis = previous.currentEndDateMillis
                )
            }

            else -> super.getStateByResult(previous, result)
        }

    override fun getErrorStateByResult(result: DomainResult, errorType: ErrorType): ExportState =
        ExportState.Error(
            errorType,
            state.value.currentSpreadSheetFormat,
            state.value.currentStartDateMillis,
            state.value.currentEndDateMillis
        )
}

private sealed interface InternalResult : DomainResult {
    data class ShowConfirm(val spreadSheetFormat: SpreadSheetFormat) : InternalResult
    data object Dismissed : InternalResult
    data class TabLayoutUpdated(val spreadSheetFormat: SpreadSheetFormat) : InternalResult
    data class DateRangeUpdated(
        val startDateMillis: Long,
        val endDateMillis: Long,
    ) : InternalResult
}
