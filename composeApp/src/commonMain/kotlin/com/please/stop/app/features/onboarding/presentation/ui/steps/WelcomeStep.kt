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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import plzstop.composeapp.generated.resources.onboarding_app_name
import plzstop.composeapp.generated.resources.onboarding_name_label
import plzstop.composeapp.generated.resources.onboarding_tagline
import plzstop.composeapp.generated.resources.skip

@Composable
fun WelcomeStep(
    state: Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val tagline = stringResource(Res.string.onboarding_tagline)
    val heading = tagline.substringBefore('\n').ifBlank { tagline }
    val description = tagline.substringAfter('\n', "")

    OnboardingCenteredContent {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.onboarding_app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uD83D\uDE80",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = heading,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (description.isNotBlank()) description else tagline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = { name -> onEvent(OnboardingEvent.DisplayNameChanged(name)) },
                    label = { Text(stringResource(Res.string.onboarding_name_label)) },
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = Color.White.copy(alpha = 0.24f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.24f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(54.dp))

            OnboardingStepIndicator(currentStep = OnboardingStep.WELCOME)

            Spacer(modifier = Modifier.height(16.dp))

            OnboardingPrimaryButton(
                currentStep = OnboardingStep.WELCOME,
                isEnabled = true,
                isSaving = false,
                onClick = { onEvent(OnboardingEvent.NextTapped) },
            )

            OnboardingSecondaryTextButton(
                text = stringResource(Res.string.skip),
                onClick = { onEvent(OnboardingEvent.NextTapped) },
            )
        }
    }
}
