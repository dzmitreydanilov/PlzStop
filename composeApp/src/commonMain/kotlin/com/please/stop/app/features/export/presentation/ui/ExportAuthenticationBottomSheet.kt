package com.please.stop.app.features.export.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.auth.apple.AppleAuthProvider
import com.please.stop.app.features.auth.apple.AppleButtonUiContainer
import com.please.stop.app.features.auth.google.GoogleButtonUiContainer
import com.please.stop.app.features.auth.presentation.AuthEvent
import com.please.stop.app.features.auth.presentation.AuthNavigation
import com.please.stop.app.features.auth.presentation.AuthState
import com.please.stop.app.features.auth.presentation.AuthStateHolder
import com.please.stop.app.features.auth.presentation.asOverlay
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.theme.LocalAppDimens
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.sheets.AppModalBottomSheet
import com.please.stop.app.uicomponents.sheets.rememberFullyExpandedAppModalBottomSheetState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.content_desc_loading
import plzstop.composeapp.generated.resources.export_sign_in_body_google
import plzstop.composeapp.generated.resources.export_sign_in_body_google_apple
import plzstop.composeapp.generated.resources.export_sign_in_title
import plzstop.composeapp.generated.resources.user_continue_apple
import plzstop.composeapp.generated.resources.user_continue_google

@Composable
internal fun ExportAuthenticationBottomSheet(
    onAuthenticationComplete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateHolder = koinViewModel<AuthStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()
    val appleSignInSupported = koinInject<AppleAuthProvider>().isSupported
    val sheetState = rememberFullyExpandedAppModalBottomSheetState()

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { navigation ->
        when (navigation) {
            AuthNavigation.NavigateToHome -> onAuthenticationComplete()
        }
    }

    AppModalBottomSheet(
        state = sheetState,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        ScreenOverlayContainer(
            overlay = state.asOverlay,
            onDismiss = { stateHolder.processEvent(AuthEvent.DismissError) },
        ) {
            ExportAuthenticationContent(
                state = state,
                isAppleSignInSupported = appleSignInSupported,
                onEvent = stateHolder::processEvent,
            )
        }
    }
}

@Composable
private fun ExportAuthenticationContent(
    state: AuthState,
    isAppleSignInSupported: Boolean,
    onEvent: (AuthEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val loadingDescription = stringResource(Res.string.content_desc_loading)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.small2, vertical = dimens.small1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.export_sign_in_title),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(dimens.extraSmall))
        Text(
            text = stringResource(
                if (isAppleSignInSupported) {
                    Res.string.export_sign_in_body_google_apple
                } else {
                    Res.string.export_sign_in_body_google
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimens.small2))

        GoogleButtonUiContainer(
            modifier = Modifier.fillMaxWidth(),
            filterByAuthorizedAccounts = false,
            onGoogleSignInResult = { credential ->
                if (credential == null) {
                    onEvent(AuthEvent.GoogleSignInCancelled)
                } else {
                    onEvent(AuthEvent.GoogleSignInCompleted(credential))
                }
            },
        ) {
            Button(
                onClick = ::onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is AuthState.Loading,
            ) {
                Text(stringResource(Res.string.user_continue_google))
            }
        }

        if (isAppleSignInSupported) {
            Spacer(Modifier.height(dimens.small1))
            AppleButtonUiContainer(
                modifier = Modifier.fillMaxWidth(),
                onAppleSignInResult = { user ->
                    if (user == null) {
                        onEvent(AuthEvent.AppleSignInCancelled)
                    } else {
                        onEvent(AuthEvent.AppleSignInCompleted(user))
                    }
                },
            ) {
                OutlinedButton(
                    onClick = ::onClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state !is AuthState.Loading,
                ) {
                    Text(stringResource(Res.string.user_continue_apple))
                }
            }
        }

        if (state is AuthState.Loading) {
            Spacer(Modifier.height(dimens.small2))
            CircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = loadingDescription
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
    }
}
