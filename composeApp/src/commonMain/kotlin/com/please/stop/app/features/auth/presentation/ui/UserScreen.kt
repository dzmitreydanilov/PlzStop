package com.please.stop.app.features.auth.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.auth.apple.AppleButtonUiContainer
import com.please.stop.app.features.auth.google.GoogleButtonUiContainer
import com.please.stop.app.features.auth.presentation.UserEvent
import com.please.stop.app.features.auth.presentation.UserState
import com.please.stop.app.features.auth.presentation.UserStateHolder
import com.please.stop.app.features.auth.presentation.asOverlay
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
