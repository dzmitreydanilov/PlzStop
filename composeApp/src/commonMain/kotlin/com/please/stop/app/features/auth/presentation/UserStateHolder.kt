package com.please.stop.app.features.auth.presentation

import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.auth.apple.AppleAuthProvider
import com.please.stop.app.features.auth.domain.model.FirebaseReauthenticationCredential
import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import com.please.stop.app.features.auth.domain.usecase.DeleteAccountUseCase
import com.please.stop.app.features.auth.domain.usecase.GetCurrentSignInProviderUseCase
import com.please.stop.app.features.auth.domain.usecase.LogoutResult
import com.please.stop.app.features.auth.domain.usecase.LogoutUseCase
import com.please.stop.app.features.auth.domain.usecase.ObserveAuthStateUseCase
import com.please.stop.app.features.auth.domain.usecase.SignInResult
import com.please.stop.app.features.auth.domain.usecase.SignInWithAppleUseCase
import com.please.stop.app.features.auth.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.please.stop.app.core.models.domain.Result as DomainResult

class UserStateHolder(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithAppleUseCase: SignInWithAppleUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getCurrentSignInProviderUseCase: GetCurrentSignInProviderUseCase,
    appleAuthProvider: AppleAuthProvider,
) : StateHolder<UserState, UserEvent>() {

    override val tag: String = "UserStateHolder"
    private val isAppleSignInSupported = appleAuthProvider.isSupported

    override fun getInitial(): UserState = UserState.Content(
        isAuthenticated = false,
        isAppleSignInSupported = isAppleSignInSupported
    )

    override fun collectWhileSubscribed(): Flow<DomainResult> =
        observeAuthStateUseCase().map { isAuthenticated ->
            AuthenticationChanged(
                isAuthenticated = isAuthenticated,
                signInProvider = if (isAuthenticated) getCurrentSignInProviderUseCase() else null,
            )
        }

    override fun resolveEventResult(event: UserEvent): Flow<DomainResult> = when (event) {
        is UserEvent.GoogleSignInCompleted -> signInWithGoogleUseCase(event.credential)
        is UserEvent.AppleSignInCompleted -> signInWithAppleUseCase(event.user)
        UserEvent.DeleteAccount -> deleteAccount()
        is UserEvent.DeleteAccountWithGoogle -> deleteAccount(
            FirebaseReauthenticationCredential.Google(idToken = event.credential.idToken),
        )
        is UserEvent.DeleteAccountWithApple -> deleteAccount(
            FirebaseReauthenticationCredential.Apple(
                identityToken = event.user.identityToken,
                nonce = event.user.nonce,
            ),
        )
        is UserEvent.Logout -> logoutUseCase()
        UserEvent.DismissDeleteReauthentication -> flowOf(DeleteReauthenticationDismissed)
        UserEvent.SignInCancelled,
        UserEvent.DismissError -> flowOf(Idle)
    }

    override fun getStateByResult(previous: UserState, result: DomainResult): UserState =
        when (result) {
            is LogoutResult.Loading, is SignInResult.Loading, DeleteAccountLoading -> {
                UserState.Content(
                    isAuthenticated = previous.isAuthenticated,
                    signInProvider = previous.signInProvider,
                    isAppleSignInSupported = previous.isAppleSignInSupported,
                    isLoading = true,
                )
            }

            is AuthenticationChanged -> UserState.Content(
                isAuthenticated = result.isAuthenticated,
                signInProvider = result.signInProvider,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            Idle -> UserState.Content(
                isAuthenticated = previous.isAuthenticated,
                signInProvider = previous.signInProvider,
                isAppleSignInSupported = previous.isAppleSignInSupported,
                isDeleteReauthenticationRequired = previous.isDeleteReauthenticationRequired,
            )

            is SignInResult.Success -> UserState.Content(
                isAuthenticated = true,
                signInProvider = previous.signInProvider,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            is LogoutResult.Success -> UserState.Content(
                isAuthenticated = false,
                signInProvider = null,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            DeleteAccountUseCase.DeleteResult.Success -> UserState.Content(
                isAuthenticated = false,
                signInProvider = null,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            DeleteAccountUseCase.DeleteResult.NeedsReauthentication ->
                previous.signInProvider?.let {
                    UserState.Content(
                        isAuthenticated = previous.isAuthenticated,
                        signInProvider = it,
                        isAppleSignInSupported = previous.isAppleSignInSupported,
                        isDeleteReauthenticationRequired = true,
                    )
                } ?: previous.toError(ErrorType.Authentication(message = null))

            DeleteReauthenticationDismissed -> UserState.Content(
                isAuthenticated = previous.isAuthenticated,
                signInProvider = previous.signInProvider,
                isAppleSignInSupported = previous.isAppleSignInSupported,
            )

            else -> super.getStateByResult(previous, result)
        }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType
    ): UserState = state.value.toError(errorType)

    private fun deleteAccount(): Flow<DomainResult> = flow {
        emit(DeleteAccountLoading)
        emit(deleteAccountUseCase())
    }

    private fun deleteAccount(
        credential: FirebaseReauthenticationCredential,
    ): Flow<DomainResult> = flow {
        emit(DeleteAccountLoading)
        emit(deleteAccountUseCase(credential))
    }
}

data class AuthenticationChanged(
    val isAuthenticated: Boolean,
    val signInProvider: FirebaseSignInProvider?,
) : DomainResult
data object Idle : DomainResult
private data object DeleteAccountLoading : DomainResult
private data object DeleteReauthenticationDismissed : DomainResult
