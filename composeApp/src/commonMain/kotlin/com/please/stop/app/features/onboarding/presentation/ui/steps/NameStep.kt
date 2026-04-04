package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState.Content
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_name_hint
import plzstop.composeapp.generated.resources.onboarding_name_label
import plzstop.composeapp.generated.resources.onboarding_whats_your_name

@Composable
fun NameStep(
    state: Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    Spacer(modifier = Modifier.height(48.dp))

    Text(
        text = stringResource(Res.string.onboarding_whats_your_name),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(Res.string.onboarding_name_hint),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = state.displayName,
        onValueChange = { name -> onEvent(OnboardingEvent.DisplayNameChanged(name)) },
        label = { Text(stringResource(Res.string.onboarding_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

}
