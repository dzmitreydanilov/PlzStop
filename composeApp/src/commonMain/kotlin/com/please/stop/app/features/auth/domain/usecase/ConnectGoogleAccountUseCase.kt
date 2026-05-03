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
    sealed interface ConnectResult : Result {
        data object Success : ConnectResult
        data class Failure(override val errorType: ErrorType) : ConnectResult, ErrorResult
    }

    suspend operator fun invoke(googleUser: GoogleUser): ConnectResult = withContext(ioDispatcher) {
        runCatching {
            googleAccountStorage.write(GoogleAccountLink(email = "", isConnected = true))
        }.fold(
            onSuccess = { ConnectResult.Success },
            onFailure = { ConnectResult.Failure(it.toErrorType()) },
        )
    }
}
