package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.models.data.GoogleAccountLink
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.auth.domain.repository.GoogleAccountRepository
import com.please.stop.app.features.auth.google.GoogleSheetsAuthorizationCode
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableErrorReason
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ConnectGoogleAccountUseCase(
    private val googleAccountStorage: IGoogleAccountStorage,
    private val googleAccountRepository: GoogleAccountRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(authorizationCode: GoogleSheetsAuthorizationCode): ConnectGoogleAccount =
        withContext(ioDispatcher) {
            googleAccountRepository.link(authorizationCode).map {
                googleAccountStorage.write(
                    GoogleAccountLink(
                        email = "",
                        isConnected = true,
                    )
                )
            }.fold(
                onSuccess = { ConnectGoogleAccount.Success },
                onFailure = { error -> error.toConnectGoogleAccountResult() },
            )
        }
}

sealed interface ConnectGoogleAccount : Result {
    data object Success : ConnectGoogleAccount
    data object ReconnectRequired : ConnectGoogleAccount
    data class Failure(override val errorType: ErrorType) : ConnectGoogleAccount, ErrorResult
}

private fun Throwable.toConnectGoogleAccountResult(): ConnectGoogleAccount {
    val callableReason = (this as? FirebaseCallableException)?.reason
    return when (callableReason) {
        FirebaseCallableErrorReason.GoogleRefreshTokenMissing,
        FirebaseCallableErrorReason.GoogleReconnectRequired,
        FirebaseCallableErrorReason.GoogleScopesMissing,
        -> ConnectGoogleAccount.ReconnectRequired

        else -> ConnectGoogleAccount.Failure(toErrorType())
    }
}
