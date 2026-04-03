package com.please.stop.app.features.onboarding.presentation.ui

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState
import com.please.stop.app.features.onboarding.presentation.OnboardingStep
import com.please.stop.app.features.onboarding.presentation.ui.steps.BudgetStep
import com.please.stop.app.features.onboarding.presentation.ui.steps.CategoriesStep
import com.please.stop.app.features.onboarding.presentation.ui.steps.CurrencyStep
import com.please.stop.app.features.onboarding.presentation.ui.steps.WelcomeStep

@Composable
fun OnboardingContent(
    state: OnboardingState.Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentStep.ordinal,
        pageCount = { OnboardingStep.entries.size },
    )

    LaunchedEffect(state.currentStep) {
        pagerState.animateScrollToPage(state.currentStep.ordinal)
    }

    HorizontalPager(
        modifier = Modifier,
        userScrollEnabled = false,
        state = pagerState
    ) { page ->
        when (OnboardingStep.entries[page]) {
            OnboardingStep.WELCOME -> WelcomeStep(state = state, onEvent = onEvent)
            OnboardingStep.CURRENCY -> CurrencyStep(state = state, onEvent = onEvent)
            OnboardingStep.BUDGET -> BudgetStep(state = state, onEvent = onEvent)
            OnboardingStep.CATEGORIES -> CategoriesStep(state = state, onEvent = onEvent)
        }
    }
}
