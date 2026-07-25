package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.core.runSuspendCatching
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.domain.repository.GoogleAccountRepository
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart

class LogoutUseCase(
    private val googleAccountStorage: IGoogleAccountStorage,
    private val googleAccountRepository: GoogleAccountRepository,
    private val firebaseAuthProvider: FirebaseAuthProvider,
    private val googleAuthProvider: GoogleAuthProvider,
    private val ioDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(): Flow<LogoutResult> {
        return flow {
            // Logging out ends remote sessions but intentionally preserves the local database.
            val cleanupResults = listOf(
                googleAccountRepository.unlink(),
                runSuspendCatching { googleAccountStorage.delete() },
                runSuspendCatching { firebaseAuthProvider.signOut() },
                runSuspendCatching { googleAuthProvider.signOut() },
            )
            val firstFailure = cleanupResults.firstNotNullOfOrNull { result ->
                result.exceptionOrNull()
            }
            emit(
                firstFailure?.let { error -> LogoutResult.Failure(error.toErrorType()) }
                    ?: LogoutResult.Success
            )
        }
            .onStart { emit(LogoutResult.Loading) }
            .flowOn(ioDispatcher)
    }
}

sealed interface LogoutResult : Result {
    data object Success : LogoutResult
    data object Loading : LogoutResult
    data class Failure(override val errorType: ErrorType) : LogoutResult, ErrorResult
}
