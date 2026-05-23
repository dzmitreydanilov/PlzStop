package com.please.stop.app.features.auth.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.auth.google.GoogleButtonUiContainer
import com.please.stop.app.features.auth.presentation.AuthEvent
import com.please.stop.app.features.auth.presentation.AuthNavigation
import com.please.stop.app.features.auth.presentation.AuthState
import com.please.stop.app.features.auth.presentation.AuthStateHolder
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(onNavigateToHome: () -> Unit) {
    val stateHolder = koinViewModel<AuthStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { nav ->
        when (nav) {
            AuthNavigation.NavigateToHome -> onNavigateToHome()
        }
    }

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(AuthEvent.DismissError) },
    ) {
        DisplayFullScreenProgress(showProgress = state is AuthState.Loading)
        AuthContent(
            state = state,
            onEvent = stateHolder::processEvent,
        )
    }
}

@Composable
private fun AuthContent(
    state: AuthState,
    onEvent: (AuthEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sign in to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))

        GoogleButtonUiContainer(
            modifier = Modifier.fillMaxWidth(),
            filterByAuthorizedAccounts = false,
            onGoogleSignInResult = { user ->
                if (user != null) {
                    onEvent(AuthEvent.GoogleSignInCompleted(user))
                } else {
                    onEvent(AuthEvent.GoogleSignInCancelled)
                }
            },
        ) {
            Button(
                onClick = ::onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is AuthState.Loading,
            ) {
                Text("Continue with Google")
            }
        }

        // TODO: Apple Sign-In button — requires platform-specific AppleButtonUiContainer
    }
}

private val AuthState.asOverlay: ScreenOverlay?
    @Composable get() = when (this) {
        is AuthState.Error -> ScreenOverlay.Error(type = errorType)
        else -> null
    }
