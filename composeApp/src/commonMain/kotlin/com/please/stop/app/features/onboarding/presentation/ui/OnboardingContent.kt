package com.please.stop.app.features.onboarding.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState
import com.please.stop.app.features.onboarding.presentation.OnboardingStep
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingPrimaryButton
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingSecondaryTextButton
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingStepIndicator
import com.please.stop.app.features.onboarding.presentation.ui.steps.BudgetStep
import com.please.stop.app.features.onboarding.presentation.ui.steps.CurrencyStep
import com.please.stop.app.features.onboarding.presentation.ui.steps.NameStep
import com.please.stop.app.features.onboarding.presentation.ui.steps.WelcomeStep
import com.please.stop.app.navigation.nav3.HandleNavigationBack
import com.please.stop.app.theme.LocalAppDimens
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.skip

private val CONTENT_MAX_WIDTH = 460.dp
private const val CONTENT_WIDTH_FRACTION = 0.85f

@Composable
fun OnboardingContent(
    state: OnboardingState.Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val dimens = LocalAppDimens.current
    val pagerState = rememberPagerState(
        initialPage = state.currentStep.ordinal,
        pageCount = { OnboardingStep.entries.size },
    )

    LaunchedEffect(state.currentStep) {
        pagerState.animateScrollToPage(state.currentStep.ordinal)
    }

    HandleNavigationBack(enabled = state.currentStep != OnboardingStep.WELCOME) {
        onEvent(OnboardingEvent.BackTapped)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = dimens.onboardingSectionSpacing)
                    .padding(bottom = dimens.onboardingSectionSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OnboardingStepIndicator(currentStep = state.currentStep)
                Spacer(modifier = Modifier.height(dimens.small2))
                OnboardingPrimaryButton(
                    currentStep = state.currentStep,
                    isEnabled = state.isNextEnabled,
                    isSaving = state.isSaving,
                    onClick = { onEvent(OnboardingEvent.NextTapped) },
                )
                if (state.currentStep.isSkippable) {
                    OnboardingSecondaryTextButton(
                        text = stringResource(Res.string.skip),
                        onClick = { onEvent(OnboardingEvent.SkipTapped) },
                    )
                }
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars),
            userScrollEnabled = false,
            state = pagerState,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = CONTENT_MAX_WIDTH)
                        .fillMaxWidth(CONTENT_WIDTH_FRACTION),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (OnboardingStep.entries[page]) {
                        OnboardingStep.WELCOME -> WelcomeStep()
                        OnboardingStep.CURRENCY -> CurrencyStep(state = state, onEvent = onEvent)
                        OnboardingStep.NAME -> NameStep(state = state, onEvent = onEvent)
                        OnboardingStep.BUDGET -> BudgetStep(state = state, onEvent = onEvent)
                    }
                }
            }
        }
    }

    if (state.showCurrencySheet) {
        CurrencyBottomSheet(
            state = state,
            onEvent = onEvent,
        )
    }
}
