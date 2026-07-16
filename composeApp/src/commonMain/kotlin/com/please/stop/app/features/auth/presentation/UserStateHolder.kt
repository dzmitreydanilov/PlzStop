package com.please.stop.app.features.auth.presentation

import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.auth.apple.AppleAuthProvider
import com.please.stop.app.features.auth.domain.usecase.LogoutResult
import com.please.stop.app.features.auth.domain.usecase.LogoutUseCase
import com.please.stop.app.features.auth.domain.usecase.ObserveAuthStateUseCase
import com.please.stop.app.features.auth.domain.usecase.SignInResult
import com.please.stop.app.features.auth.domain.usecase.SignInWithAppleUseCase
import com.please.stop.app.features.auth.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.please.stop.app.core.models.domain.Result as DomainResult

class UserStateHolder(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithAppleUseCase: SignInWithAppleUseCase,
    private val logoutUseCase: LogoutUseCase,
    appleAuthProvider: AppleAuthProvider,
) : StateHolder<UserState, UserEvent>() {

    override val tag: String = "UserStateHolder"
    private val isAppleSignInSupported = appleAuthProvider.isSupported

    override fun getInitial(): UserState = UserState.Content(
        isAuthenticated = false,
        isAppleSignInSupported = isAppleSignInSupported
    )

    override fun collectWhileSubscribed(): Flow<DomainResult> =
        observeAuthStateUseCase().map(::AuthenticationChanged)

    override fun resolveEventResult(event: UserEvent): Flow<DomainResult> = when (event) {
        is UserEvent.GoogleSignInCompleted -> signInWithGoogleUseCase(event.credential)
        is UserEvent.AppleSignInCompleted -> signInWithAppleUseCase(event.user)
        is UserEvent.Logout -> logoutUseCase()
        UserEvent.SignInCancelled,
        UserEvent.DismissError -> flowOf(Idle)
    }

    override fun getStateByResult(previous: UserState, result: DomainResult): UserState =
        when (result) {
            is LogoutResult.Loading, is SignInResult.Loading -> {
                UserState.Content(
                    isAuthenticated = previous.isAuthenticated,
                    isAppleSignInSupported = previous.isAppleSignInSupported,
                    isLoading = true
                )
            }

            is AuthenticationChanged -> UserState.Content(
                isAuthenticated = result.isAuthenticated,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            Idle -> UserState.Content(
                isAuthenticated = previous.isAuthenticated,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            is SignInResult.Success -> UserState.Content(
                isAuthenticated = true,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            is LogoutResult.Success -> UserState.Content(
                isAuthenticated = false,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            else -> super.getStateByResult(previous, result)
        }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType
    ): UserState = state.value.toError(errorType)
}

data class AuthenticationChanged(val isAuthenticated: Boolean) : DomainResult
data object Idle : DomainResult
