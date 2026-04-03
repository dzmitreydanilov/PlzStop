package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState.Content
import com.please.stop.app.features.onboarding.presentation.OnboardingStep
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingCenteredContent
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingPrimaryButton
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingSecondaryTextButton
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingStepIndicator
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_budget_hint
import plzstop.composeapp.generated.resources.onboarding_budget_label
import plzstop.composeapp.generated.resources.onboarding_back
import plzstop.composeapp.generated.resources.onboarding_set_budget

@Composable
fun BudgetStep(
    state: Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val decimalPlaces = state.decimalPlaces

    OnboardingCenteredContent {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.onboarding_set_budget),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
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
                                    Text(
                                        text = state.currencySymbol,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                            label = { Text(stringResource(Res.string.onboarding_budget_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = Color.White.copy(alpha = 0.24f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.24f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.White.copy(alpha = 0.52f),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.82f),
                                    shape = RoundedCornerShape(16.dp),
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(Res.string.onboarding_budget_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(54.dp))

            OnboardingStepIndicator(currentStep = OnboardingStep.BUDGET)

            Spacer(modifier = Modifier.height(16.dp))

            OnboardingPrimaryButton(
                currentStep = OnboardingStep.BUDGET,
                isEnabled = state.isNextEnabled,
                isSaving = state.isSaving,
                onClick = { onEvent(OnboardingEvent.NextTapped) },
            )

            OnboardingSecondaryTextButton(
                text = stringResource(Res.string.onboarding_back),
                onClick = { onEvent(OnboardingEvent.BackTapped) },
            )
        }
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
