package com.please.stop.app.features.auth.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.auth.apple.AppleButtonUiContainer
import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import com.please.stop.app.features.auth.google.GoogleButtonUiContainer
import com.please.stop.app.features.auth.google.GoogleSignInCredential
import com.please.stop.app.features.auth.presentation.UserEvent
import com.please.stop.app.features.auth.presentation.UserState
import com.please.stop.app.features.auth.presentation.UserStateHolder
import com.please.stop.app.features.auth.presentation.asOverlay
import com.please.stop.app.theme.LocalAppDimens
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.icons.ArrowBackIconButton
import com.please.stop.app.uicomponents.previews.ApplicationPreviewThemeWrapper
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.user_account_connected
import plzstop.composeapp.generated.resources.user_account_description
import plzstop.composeapp.generated.resources.user_account_not_connected
import plzstop.composeapp.generated.resources.user_continue_apple
import plzstop.composeapp.generated.resources.user_continue_google
import plzstop.composeapp.generated.resources.user_delete_account
import plzstop.composeapp.generated.resources.user_delete_account_message
import plzstop.composeapp.generated.resources.user_delete_account_reauthenticate_apple
import plzstop.composeapp.generated.resources.user_delete_account_reauthenticate_google
import plzstop.composeapp.generated.resources.user_delete_account_reauthenticate_title
import plzstop.composeapp.generated.resources.user_delete_account_title
import plzstop.composeapp.generated.resources.user_delete_cancel
import plzstop.composeapp.generated.resources.user_delete_with_apple
import plzstop.composeapp.generated.resources.user_delete_with_google
import plzstop.composeapp.generated.resources.user_logout
import plzstop.composeapp.generated.resources.user_title

@Composable
fun UserScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateHolder = koinViewModel<UserStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(UserEvent.DismissError) },
    ) {
        UserContent(
            state = state,
            onNavigateBack = onNavigateBack,
            onEvent = stateHolder::processEvent,
            modifier = modifier,
        )
        DisplayFullScreenProgress(showProgress = state.isLoading)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun UserContent(
    state: UserState,
    onNavigateBack: () -> Unit,
    onEvent: (UserEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val dimens = LocalAppDimens.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.user_title)) },
                navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = stringResource(
                    if (state.isAuthenticated) {
                        Res.string.user_account_connected
                    } else {
                        Res.string.user_account_not_connected
                    }
                ),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.user_account_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            if (state.isAuthenticated) {
                OutlinedButton(
                    onClick = { onEvent(UserEvent.Logout) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                ) {
                    Text(stringResource(Res.string.user_logout))
                }
                Spacer(Modifier.height(dimens.small1))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.user_delete_account))
                }
            } else {
                GoogleButtonUiContainer(
                    modifier = Modifier.fillMaxWidth(),
                    filterByAuthorizedAccounts = false,
                    onGoogleSignInResult = { user ->
                        if (user == null) {
                            onEvent(UserEvent.SignInCancelled)
                        } else {
                            onEvent(UserEvent.GoogleSignInCompleted(user))
                        }
                    },
                ) {
                    Button(
                        onClick = ::onClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(Res.string.user_continue_google))
                    }
                }

                if (state.isAppleSignInSupported) {
                    Spacer(Modifier.height(12.dp))
                    AppleButtonUiContainer(
                        modifier = Modifier.fillMaxWidth(),
                        onAppleSignInResult = { user ->
                            if (user == null) {
                                onEvent(UserEvent.SignInCancelled)
                            } else {
                                onEvent(UserEvent.AppleSignInCompleted(user))
                            }
                        },
                    ) {
                        OutlinedButton(
                            onClick = ::onClick,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading,
                        ) {
                            Text(stringResource(Res.string.user_continue_apple))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && state.isAuthenticated) {
        DeleteAccountDialog(
            isLoading = state.isLoading,
            onConfirm = {
                showDeleteDialog = false
                onEvent(UserEvent.DeleteAccount)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    val signInProvider = state.signInProvider
    if (
        state.isAuthenticated &&
        state.isDeleteReauthenticationRequired &&
        signInProvider != null
    ) {
        DeleteAccountReauthenticationDialog(
            signInProvider = signInProvider,
            isLoading = state.isLoading,
            onGoogleCredential = { credential ->
                onEvent(UserEvent.DeleteAccountWithGoogle(credential))
            },
            onAppleCredential = { user ->
                onEvent(UserEvent.DeleteAccountWithApple(user))
            },
            onDismiss = { onEvent(UserEvent.DismissDeleteReauthentication) },
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(Res.string.user_delete_account_title)) },
        text = { Text(stringResource(Res.string.user_delete_account_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.user_delete_account))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text(stringResource(Res.string.user_delete_cancel))
            }
        },
    )
}

@Composable
private fun DeleteAccountReauthenticationDialog(
    signInProvider: FirebaseSignInProvider,
    isLoading: Boolean,
    onGoogleCredential: (GoogleSignInCredential) -> Unit,
    onAppleCredential: (AppleUser) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(Res.string.user_delete_account_reauthenticate_title)) },
        text = {
            Text(
                stringResource(
                    when (signInProvider) {
                        FirebaseSignInProvider.GOOGLE -> {
                            Res.string.user_delete_account_reauthenticate_google
                        }

                        FirebaseSignInProvider.APPLE -> {
                            Res.string.user_delete_account_reauthenticate_apple
                        }
                    }
                )
            )
        },
        confirmButton = {
            when (signInProvider) {
                FirebaseSignInProvider.GOOGLE -> GoogleButtonUiContainer(
                    filterByAuthorizedAccounts = true,
                    isAutoSelectEnabled = true,
                    onGoogleSignInResult = { credential ->
                        credential?.let(onGoogleCredential) ?: onDismiss()
                    },
                ) {
                    TextButton(
                        onClick = ::onClick,
                        enabled = !isLoading,
                    ) {
                        Text(stringResource(Res.string.user_delete_with_google))
                    }
                }

                FirebaseSignInProvider.APPLE -> AppleButtonUiContainer(
                    onAppleSignInResult = { user ->
                        user?.let(onAppleCredential) ?: onDismiss()
                    },
                ) {
                    TextButton(
                        onClick = ::onClick,
                        enabled = !isLoading,
                    ) {
                        Text(stringResource(Res.string.user_delete_with_apple))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text(stringResource(Res.string.user_delete_cancel))
            }
        },
    )
}


@Preview(showBackground = true, showSystemUi = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun UserContentNotAuthenticatedPreview() {
    UserContent(
        state = UserState.Content(
            isAuthenticated = false,
            isAppleSignInSupported = true,
        ),
        onNavigateBack = {},
        onEvent = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun UserContentAuthenticatedPreview() {
    UserContent(
        state = UserState.Content(
            isAuthenticated = true,
            isAppleSignInSupported = true,
        ),
        onNavigateBack = {},
        onEvent = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun UserContentAuthenticatedHasApplePreview() {
    UserContent(
        state = UserState.Content(
            isAuthenticated = true,
            isAppleSignInSupported = true,
        ),
        onNavigateBack = {},
        onEvent = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun UserContentAuthenticatedNoApplePreview() {
    UserContent(
        state = UserState.Content(
            isAuthenticated = true,
            isAppleSignInSupported = true,
        ),
        onNavigateBack = {},
        onEvent = {},
    )
}
