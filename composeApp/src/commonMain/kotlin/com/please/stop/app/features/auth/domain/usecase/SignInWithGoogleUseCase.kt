package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.flow.flowFromSuspend
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.auth.domain.repository.AuthRepository
import com.please.stop.app.features.auth.google.GoogleSignInCredential
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class SignInWithGoogleUseCase(
    private val repository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(credential: GoogleSignInCredential): Flow<SignInResult> {
        return flowFromSuspend { repository.signInWithGoogle(credential) }
            .map { signInResult ->
                signInResult.fold(
                    onSuccess = { SignInResult.Success },
                    onFailure = { SignInResult.Failure(it.toErrorType()) },
                )
            }
            .onStart { emit(SignInResult.Loading) }
            .flowOn(ioDispatcher)
    }
}

sealed interface SignInResult : Result {
    data object Success : SignInResult
    data object Loading : SignInResult
    data class Failure(override val errorType: ErrorType) : SignInResult, ErrorResult
}
