package com.please.stop.app.features.auth.presentation

import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.auth.domain.usecase.SignInResult
import com.please.stop.app.features.auth.domain.usecase.SignInWithAppleUseCase
import com.please.stop.app.features.auth.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.reflect.KClass
import com.please.stop.app.core.models.domain.Result as DomainResult

class AuthStateHolder(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithAppleUseCase: SignInWithAppleUseCase,
) : StateHolder<AuthState, AuthEvent>() {

    override val tag = "AuthStateHolder"

    override fun getInitial(): AuthState = AuthState.Idle

    override fun resolveEventResult(event: AuthEvent): Flow<DomainResult> = when (event) {
        is AuthEvent.GoogleSignInCompleted -> signInWithGoogleUseCase(event.credential)
        is AuthEvent.AppleSignInCompleted -> signInWithAppleUseCase(event.appleUser)
        AuthEvent.GoogleSignInCancelled,
        AuthEvent.AppleSignInCancelled -> flowOf(InternalResult.Cancelled)
        AuthEvent.DismissError -> flowOf(InternalResult.Cancelled)
    }

    override fun getStateByResult(previous: AuthState, result: DomainResult): AuthState =
        when (result) {
            InternalResult.Loading -> AuthState.Loading
            InternalResult.Cancelled -> AuthState.Idle
            is SignInResult.Success -> AuthState.Idle
            else -> super.getStateByResult(previous, result)
        }

    override fun getErrorStateByResult(result: DomainResult, errorType: ErrorType): AuthState =
        AuthState.Error(errorType)

    override fun getNavigationResults(): Set<KClass<out DomainResult>> = setOf(
        SignInResult.Success::class,
        SignInResult.Success::class,
    )

    override fun getNavigationByResult(result: DomainResult): Navigation? = when (result) {
        is SignInResult.Success -> AuthNavigation.NavigateToHome
        else -> null
    }
}

private sealed interface InternalResult : DomainResult {
    data object Loading : InternalResult
    data object Cancelled : InternalResult
}
