package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState.Content
import com.please.stop.app.theme.LocalAppDimens
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_choose_currency
import plzstop.composeapp.generated.resources.onboarding_detecting_currency
import plzstop.composeapp.generated.resources.onboarding_select_currency

@Composable
fun CurrencyStep(
    state: Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val dimens = LocalAppDimens.current
    Spacer(modifier = Modifier.height(dimens.onboardingTopSpacing))

    Text(
        text = stringResource(Res.string.onboarding_choose_currency),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(dimens.onboardingSectionSpacing))

    CurrencySelector(
        selectedCurrency = state.selectedCurrency,
        onClick = { onEvent(OnboardingEvent.SelectCurrencyTapped) },
    )

    if (state.isDetectingCurrency) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimens.small2),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(dimens.onboardingTopSpacing))
                    Text(
                        text = stringResource(Res.string.onboarding_detecting_currency),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            },
        )
    }
}

@Composable
private fun CurrencySelector(
    selectedCurrency: Currency?,
    onClick: () -> Unit,
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(dimens.radiusMedium),
            )
            .padding(horizontal = dimens.small2, vertical = dimens.small2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectedCurrency != null) {
            Text(
                text = selectedCurrency.symbol,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(dimens.small1))
            Text(
                text = "${selectedCurrency.code} — ${selectedCurrency.name}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = stringResource(Res.string.onboarding_select_currency),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
