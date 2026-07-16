package com.please.stop.app.features.export.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.auth.domain.usecase.ObserveAuthStateUseCase
import com.please.stop.app.features.export.data.repository.GoogleSheetExportRepository
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take

class GoogleSpreadSheetExportUseCase(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val googleAccountLinkedUseCase: CheckGoogleAccountLinkageUseCase,
    private val googleSheetExportRepository: GoogleSheetExportRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        startDateMillis: Long,
        endDateMillis: Long,
        spreadSheetFormat: SpreadSheetFormat,
        spreadsheetTitle: String,
        folderName: String,
    ): Flow<GoogleSpreadSheetExportResult> {
        return observeAuthStateUseCase().take(1).flatMapLatest { isAuthenticated ->
            if (isAuthenticated) {
                exportForAuthenticatedUser(
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                    spreadSheetFormat = spreadSheetFormat,
                    spreadsheetTitle = spreadsheetTitle,
                    folderName = folderName,
                )
            } else {
                flowOf(GoogleSpreadSheetExportResult.AuthenticationRequired)
            }
        }
            .flowOn(dispatcher)
            .onStart { emit(GoogleSpreadSheetExportResult.Loading) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun exportForAuthenticatedUser(
        startDateMillis: Long,
        endDateMillis: Long,
        spreadSheetFormat: SpreadSheetFormat,
        spreadsheetTitle: String,
        folderName: String,
    ): Flow<GoogleSpreadSheetExportResult> =
        googleAccountLinkedUseCase().flatMapLatest { linkageResult ->
            when (linkageResult) {
                HasGoogleAccountLinkageResult.GoogleAccountLinked -> {
                    googleSheetExportRepository.enqueExport(
                        startDateMillis = startDateMillis,
                        endDateMillis = endDateMillis,
                        spreadSheetFormat = spreadSheetFormat,
                        spreadsheetTitle = spreadsheetTitle,
                        folderName = folderName,
                    ).map { result ->
                        result.fold(
                            onSuccess = { GoogleSpreadSheetExportResult.Enqueued },
                            onFailure = {
                                GoogleSpreadSheetExportResult.Failure(errorType = it.toErrorType())
                            },
                        )
                    }
                }

                HasGoogleAccountLinkageResult.GoogleAccountNotLinked -> {
                    flowOf(GoogleSpreadSheetExportResult.GoogleAccountNotLinked)
                }

                HasGoogleAccountLinkageResult.AuthenticationRequired -> {
                    flowOf(GoogleSpreadSheetExportResult.AuthenticationRequired)
                }

                is HasGoogleAccountLinkageResult.Failure -> {
                    flowOf(GoogleSpreadSheetExportResult.Failure(linkageResult.errorType))
                }
            }
        }
}

sealed interface GoogleSpreadSheetExportResult : Result {
    data object Enqueued : GoogleSpreadSheetExportResult

    data object Loading : GoogleSpreadSheetExportResult

    data object GoogleAccountNotLinked : GoogleSpreadSheetExportResult
    data object AuthenticationRequired : GoogleSpreadSheetExportResult
    data class Failure(override val errorType: ErrorType) :
        GoogleSpreadSheetExportResult,
        ErrorResult
}
