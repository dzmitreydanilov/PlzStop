package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import com.please.stop.app.network.authentication.BearerTokenClearer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class LogoutUseCase(
    private val googleAccountStorage: IGoogleAccountStorage,
    private val bearerTokenClearer: BearerTokenClearer,
    private val firebaseAuthProvider: FirebaseAuthProvider,
    private val googleAuthProvider: GoogleAuthProvider,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface LogoutResult : Result {
        data object Success : LogoutResult
        data class Failure(override val errorType: ErrorType) : LogoutResult, ErrorResult
    }

    suspend operator fun invoke(): LogoutResult = withContext(ioDispatcher) {
        runCatching {
            googleAccountStorage.delete()
            bearerTokenClearer.clear()
            firebaseAuthProvider.signOut()
            googleAuthProvider.signOut()
        }.fold(
            onSuccess = { LogoutResult.Success },
            onFailure = { LogoutResult.Failure(it.toErrorType()) },
        )
    }
}
