package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState.Content
import com.please.stop.app.features.onboarding.presentation.OnboardingStep
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingCenteredContent
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingPrimaryButton
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingSecondaryTextButton
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingStepIndicator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_check
import plzstop.composeapp.generated.resources.onboarding_all_currencies
import plzstop.composeapp.generated.resources.onboarding_choose_currency
import plzstop.composeapp.generated.resources.onboarding_popular
import plzstop.composeapp.generated.resources.onboarding_search_currencies
import plzstop.composeapp.generated.resources.onboarding_selected
import plzstop.composeapp.generated.resources.skip

@Composable
fun CurrencyStep(
    state: Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    OnboardingCenteredContent {
        BoxWithConstraints {
            val listHeight = maxHeight * 0.5f
            Column(
                modifier = Modifier.padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_choose_currency),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = state.currencySearchQuery,
                    onValueChange = { onEvent(OnboardingEvent.CurrencySearchQueryChanged(it)) },
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.onboarding_search_currencies),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = Color.White.copy(alpha = 0.28f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.28f),
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

                Spacer(modifier = Modifier.height(14.dp))

                val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
                val isScrollable = (state.popularCurrencies.size + state.otherCurrencies.size) > 6
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(listHeight)
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), clip = true)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(18.dp),
                        ),
                ) {
                    if (state.currencies.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                            val popular = state.popularCurrencies
                            if (popular.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(Res.string.onboarding_popular),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
                                    )
                                }
                                items(popular, key = { it.code }) { currency ->
                                    CurrencyRow(
                                        currency = currency,
                                        isSelected = currency == state.selectedCurrency,
                                        onClick = { onEvent(OnboardingEvent.CurrencySelected(currency)) },
                                    )
                                }
                            }

                            val other = state.otherCurrencies
                            if (other.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(Res.string.onboarding_all_currencies),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
                                    )
                                }
                                items(other, key = { it.code }) { currency ->
                                    CurrencyRow(
                                        currency = currency,
                                        isSelected = currency == state.selectedCurrency,
                                        onClick = { onEvent(OnboardingEvent.CurrencySelected(currency)) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (isScrollable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scroll to see more currencies",
                        style = MaterialTheme.typography.bodySmall,
                        color = hintColor,
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
                OnboardingStepIndicator(currentStep = OnboardingStep.CURRENCY)

                Spacer(modifier = Modifier.height(16.dp))

                OnboardingPrimaryButton(
                    currentStep = OnboardingStep.CURRENCY,
                    isEnabled = state.isNextEnabled,
                    isSaving = state.isSaving,
                    onClick = { onEvent(OnboardingEvent.NextTapped) },
                )

                OnboardingSecondaryTextButton(
                    text = stringResource(Res.string.skip),
                    onClick = { onEvent(OnboardingEvent.NextTapped) },
                )
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.background(bgColor, shape = RoundedCornerShape(12.dp))
                else Modifier
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currency.code,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = currency.symbol,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = currency.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        if (isSelected) {
            Icon(
                painter = painterResource(Res.drawable.ic_check),
                contentDescription = stringResource(Res.string.onboarding_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
