package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.models.data.GoogleAccountLink
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.auth.google.GoogleUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ConnectGoogleAccountUseCase(
    private val googleAccountStorage: IGoogleAccountStorage,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(googleUser: GoogleUser): ConnectGoogleAccount = withContext(ioDispatcher) {
        runCatching {
            googleAccountStorage.write(GoogleAccountLink(email = "", isConnected = true))
        }.fold(
            onSuccess = { ConnectGoogleAccount.Success },
            onFailure = { ConnectGoogleAccount.Failure(it.toErrorType()) },
        )
    }
}

sealed interface ConnectGoogleAccount : Result {
    data object Success : ConnectGoogleAccount
    data class Failure(override val errorType: ErrorType) : ConnectGoogleAccount, ErrorResult
}
