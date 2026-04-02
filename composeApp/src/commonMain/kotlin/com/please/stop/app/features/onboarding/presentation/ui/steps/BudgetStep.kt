package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState.Content
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
    val decimalPlaces = state.decimalPlaces

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.onboarding_set_budget),
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                    Text(state.currencySymbol)
                }
            },
            label = { Text(stringResource(Res.string.onboarding_budget_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.onboarding_budget_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun filterBudgetInput(input: String, decimalPlaces: Int): String? {
    if (input.isEmpty()) return input

    // Allow only digits and at most one decimal point
    if (input.count { it == '.' } > 1) return null
    if (input.any { it != '.' && !it.isDigit() }) return null

    // For zero-decimal currencies, reject decimal input entirely
    if (decimalPlaces == 0 && '.' in input) return null

    // Enforce max decimal places
    val dotIndex = input.indexOf('.')
    if (dotIndex >= 0 && input.length - dotIndex - 1 > decimalPlaces) return null

    return input
}
