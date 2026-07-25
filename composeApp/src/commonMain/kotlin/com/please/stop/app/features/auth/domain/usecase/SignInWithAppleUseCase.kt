package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.flow.flowFromSuspend
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class SignInWithAppleUseCase(
    private val repository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(appleUser: AppleUser): Flow<SignInResult> {
        return flowFromSuspend { repository.signInWithApple(appleUser) }
            .map { signInResult ->
                signInResult.fold(
                    onSuccess = { SignInResult.Success },
                    onFailure = { SignInResult.Failure(it.toErrorType()) }
                )
            }
            .onStart { emit(SignInResult.Loading) }
            .flowOn(ioDispatcher)
    }
}
