package com.please.stop.app.features.export.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.flow.flowFromSuspend
import com.please.stop.app.core.models.data.GoogleAccountLink
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.core.runSuspendCatching
import com.please.stop.app.features.auth.domain.repository.GoogleAccountRepository
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableErrorReason
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class CheckGoogleAccountLinkageUseCase(
    private val googleAccountStorage: IGoogleAccountStorage,
    private val googleAccountRepository: GoogleAccountRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    operator fun invoke(): Flow<HasGoogleAccountLinkageResult> {
        return flowFromSuspend {
            val cachedLink = runSuspendCatching { googleAccountStorage.read() }.getOrNull()
            if (cachedLink?.isConnected == true) {
                kotlin.Result.success(true)
            } else {
                googleAccountRepository.isLinked()
                    .onSuccess { isLinked ->
                        runSuspendCatching {
                            if (isLinked) {
                                googleAccountStorage.write(
                                    GoogleAccountLink(
                                        email = "",
                                        isConnected = true,
                                    )
                                )
                            } else {
                                googleAccountStorage.delete()
                            }
                        }
                    }
            }
        }.map { linkageResult ->
            linkageResult.fold(
                onSuccess = { isLinked ->
                    if (isLinked) {
                        HasGoogleAccountLinkageResult.GoogleAccountLinked
                    } else {
                        HasGoogleAccountLinkageResult.GoogleAccountNotLinked
                    }
                },
                onFailure = { error ->
                    if (
                        (error as? FirebaseCallableException)?.reason ==
                        FirebaseCallableErrorReason.FirebaseSignInRequired
                    ) {
                        HasGoogleAccountLinkageResult.AuthenticationRequired
                    } else {
                        HasGoogleAccountLinkageResult.Failure(errorType = error.toErrorType())
                    }
                },
            )
        }.flowOn(dispatcher)
    }
}

sealed interface HasGoogleAccountLinkageResult : Result {
    data object GoogleAccountLinked : HasGoogleAccountLinkageResult
    data object GoogleAccountNotLinked : HasGoogleAccountLinkageResult
    data object AuthenticationRequired : HasGoogleAccountLinkageResult
    data class Failure(override val errorType: ErrorType) :
        HasGoogleAccountLinkageResult,
        ErrorResult
}
