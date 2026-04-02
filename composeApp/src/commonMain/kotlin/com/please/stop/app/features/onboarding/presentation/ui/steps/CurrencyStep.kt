package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState.Content
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_check
import plzstop.composeapp.generated.resources.onboarding_all_currencies
import plzstop.composeapp.generated.resources.onboarding_choose_currency
import plzstop.composeapp.generated.resources.onboarding_popular
import plzstop.composeapp.generated.resources.onboarding_search_currencies
import plzstop.composeapp.generated.resources.onboarding_selected

@Composable
fun CurrencyStep(
    state: Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.onboarding_choose_currency),
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.currencySearchQuery,
            onValueChange = { onEvent(OnboardingEvent.CurrencySearchQueryChanged(it)) },
            placeholder = { Text(stringResource(Res.string.onboarding_search_currencies)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.currencies.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val popular = state.popularCurrencies
                if (popular.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.onboarding_popular),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp),
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp),
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
}

@Composable
private fun CurrencyRow(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currency.code,
            style = MaterialTheme.typography.titleMedium,
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
