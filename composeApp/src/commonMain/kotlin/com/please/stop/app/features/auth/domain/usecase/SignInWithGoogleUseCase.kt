package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.auth.domain.repository.AuthRepository
import com.please.stop.app.features.auth.google.GoogleUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SignInWithGoogleUseCase(
    private val repository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface SignInResult : Result {
        data object Success : SignInResult
        data class Failure(override val errorType: ErrorType) : SignInResult, ErrorResult
    }

    suspend operator fun invoke(googleUser: GoogleUser): SignInResult = withContext(ioDispatcher) {
        repository.signInWithGoogle(googleUser).fold(
            onSuccess = { SignInResult.Success },
            onFailure = { SignInResult.Failure(it.toErrorType()) },
        )
    }
}
