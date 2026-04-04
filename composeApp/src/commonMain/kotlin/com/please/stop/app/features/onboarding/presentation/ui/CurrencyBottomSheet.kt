package com.please.stop.app.features.onboarding.presentation.ui

import androidx.compose.runtime.Composable
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState
import com.please.stop.app.uicomponents.sheets.CurrencyPickerSheet

@Composable
fun CurrencyBottomSheet(
    state: OnboardingState.Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    CurrencyPickerSheet(
        selectedCurrencyCode = state.selectedCurrency?.code,
        deviceCurrencyCode = state.deviceCurrencyCode,
        onCurrencySelected = { currency: Currency ->
            onEvent(OnboardingEvent.CurrencySelected(currency))
        },
        onDismiss = { onEvent(OnboardingEvent.CurrencySheetDismissed) },
    )
}
