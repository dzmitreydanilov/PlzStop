package com.please.stop.app.features.onboarding.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingNavigation
import com.please.stop.app.features.onboarding.presentation.OnboardingState
import com.please.stop.app.features.onboarding.presentation.OnboardingStateHolder
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit,
) {
    val stateHolder = koinViewModel<OnboardingStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { nav ->
        when (nav) {
            OnboardingNavigation.NavigateToHome -> onNavigateToHome()
        }
    }

    when (val s = state) {
        is OnboardingState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is OnboardingState.Content -> {
            ScreenOverlayContainer(
                overlay = asOverlay(state = s),
                onDismiss = { stateHolder.processEvent(OnboardingEvent.ErrorDismissed) },
                onRetry = { stateHolder.processEvent(OnboardingEvent.RetryLoadCurrencies) },
            ) {
                OnboardingContent(
                    state = s,
                    onEvent = stateHolder::processEvent,
                )
            }
        }

        is OnboardingState.Error -> {
            ScreenOverlayContainer(
                overlay = ScreenOverlay.Error(type = s.error.errorType),
                onDismiss = { stateHolder.processEvent(OnboardingEvent.ErrorDismissed) },
                onRetry = { stateHolder.processEvent(OnboardingEvent.RetryLoadCurrencies) },
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun asOverlay(state: OnboardingState.Content): ScreenOverlay? {
    val error = state.error ?: return null
    return ScreenOverlay.Error(type = error.errorType)
}
