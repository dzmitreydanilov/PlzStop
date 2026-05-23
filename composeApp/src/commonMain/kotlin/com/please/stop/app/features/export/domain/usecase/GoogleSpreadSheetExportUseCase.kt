package com.please.stop.app.features.export.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.export.data.repository.GoogleSheetExportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GoogleSpreadSheetExportUseCase(
    private val hasGoogleAccountLinkageResult: CheckGoogleAccountLinkageUseCase,
    private val hasNotificationPermissionResult: CheckNotificationPermissionUseCase,
    private val googleSheetExportRepository: GoogleSheetExportRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<GoogleSpreadSheetExportResult> {
        return combine(
            hasGoogleAccountLinkageResult(),
            hasNotificationPermissionResult(),
        ) { googleAccountLinkageResult, hasPermissionResult ->
            checkRequirementsForExport(
                googleAccountLinkageResult = googleAccountLinkageResult,
                hasPermissionResult = hasPermissionResult,
            )
        }.flatMapLatest { checkResult ->
            if (checkResult is GoogleSpreadSheetExportResult.Enqueued) {
                googleSheetExportRepository.enqueExport(
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                ).map { result ->
                    result.fold(
                        onSuccess = { checkResult },
                        onFailure = {
                            GoogleSpreadSheetExportResult.Failure(errorType = it.toErrorType())
                        }
                    )
                }
            } else {
                flowOf(checkResult)
            }
        }
            .flowOn(dispatcher)
            .onStart { emit(GoogleSpreadSheetExportResult.Loading) }
    }

    private fun checkRequirementsForExport(
        googleAccountLinkageResult: HasGoogleAccountLinkageResult,
        hasPermissionResult: HasNotificationPermissionResult,
    ): GoogleSpreadSheetExportResult {
        return when (googleAccountLinkageResult) {
            is HasGoogleAccountLinkageResult.GoogleAccountLinked -> {
                hasNotification(hasPermissionResult)
            }

            is HasGoogleAccountLinkageResult.GoogleAccountNotLinked -> {
                GoogleSpreadSheetExportResult.GoogleAccountNotLinked
            }
        }
    }

    private fun hasNotification(
        notificationPermissionResult: HasNotificationPermissionResult
    ): GoogleSpreadSheetExportResult {
        return when (notificationPermissionResult) {
            is HasNotificationPermissionResult.HasPermission -> {
                GoogleSpreadSheetExportResult.Enqueued
            }

            is HasNotificationPermissionResult.NoPermission -> {
                GoogleSpreadSheetExportResult.NoNotificationPermission
            }
        }
    }
}

sealed interface GoogleSpreadSheetExportResult : Result {
    data object Enqueued : GoogleSpreadSheetExportResult

    data object Loading : GoogleSpreadSheetExportResult

    object GoogleAccountNotLinked : GoogleSpreadSheetExportResult
    data object NoNotificationPermission : GoogleSpreadSheetExportResult
    data class Failure(override val errorType: ErrorType) : GoogleSpreadSheetExportResult,
        ErrorResult
}
