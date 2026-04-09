package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState.Content
import com.please.stop.app.theme.LocalAppDimens
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_budget_hint
import plzstop.composeapp.generated.resources.onboarding_budget_label
import plzstop.composeapp.generated.resources.onboarding_set_budget

@Composable
fun BudgetStep(
    state: Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val dimens = LocalAppDimens.current
    val decimalPlaces = state.decimalPlaces

    Spacer(modifier = Modifier.height(dimens.onboardingTopSpacing))

    Text(
        text = stringResource(Res.string.onboarding_set_budget),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(dimens.onboardingSectionSpacing))

    OutlinedTextField(
        value = state.monthlyBudgetInput,
        onValueChange = { input ->
            val filtered = filterBudgetInput(input, decimalPlaces)
            if (filtered != null) {
                onEvent(OnboardingEvent.BudgetInputChanged(filtered))
            }
        },
        prefix = {
            if (state.currencySymbol.isNotEmpty()) {
                Text(text = state.currencySymbol)
            }
        },
        label = { Text(stringResource(Res.string.onboarding_budget_label)) },
        supportingText = {
            Text(text = stringResource(Res.string.onboarding_budget_hint))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun filterBudgetInput(input: String, decimalPlaces: Int): String? {
    if (input.isEmpty()) return input

    if (input.count { it == '.' } > 1) return null
    if (input.any { it != '.' && !it.isDigit() }) return null

    if (decimalPlaces == 0 && '.' in input) return null

    val dotIndex = input.indexOf('.')
    if (dotIndex >= 0 && input.length - dotIndex - 1 > decimalPlaces) return null

    return input
}
