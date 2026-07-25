package com.please.stop.app.features.subscriptions.presentation.promotion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.please.stop.app.uicomponents.buttons.ApplicationButton
import com.please.stop.app.uicomponents.sheets.AnimatedAppModalBottomSheet
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_subscriptions
import plzstop.composeapp.generated.resources.subscription_promo_body
import plzstop.composeapp.generated.resources.subscription_promo_cta
import plzstop.composeapp.generated.resources.subscription_promo_title

@Composable
fun SubscriptionPromoBottomSheet(
    state: SubscriptionPromoState,
    onCtaClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state !is SubscriptionPromoState.Visible) return

    AnimatedAppModalBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
    ) { _ ->
        SubscriptionPromoContent(
            onCtaClick = onCtaClick,
        )
    }
}

@Composable
private fun SubscriptionPromoContent(
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_subscriptions),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.subscription_promo_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.subscription_promo_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ApplicationButton(
            onClick = onCtaClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(Res.string.subscription_promo_cta))
        }
    }
}
