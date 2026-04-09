package com.please.stop.app.features.onboarding.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.presentation.OnboardingStep
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.theme.LocalAppDimens
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_get_started
import plzstop.composeapp.generated.resources.onboarding_next

@Composable
fun OnboardingStepIndicator(
    currentStep: OnboardingStep,
) {
    val colorScheme = MaterialTheme.colorScheme
    val dimens = LocalAppDimens.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimens.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingStep.entries.forEach { step ->
            val isActive = step.ordinal <= currentStep.ordinal
            val isCurrent = step == currentStep

            val width by animateDpAsState(
                targetValue = if (isCurrent) dimens.onboardingSectionSpacing else dimens.extraSmall,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            val color by animateColorAsState(
                targetValue = if (isActive) colorScheme.primary else colorScheme.outlineVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            )

            Box(
                modifier = Modifier
                    .width(width)
                    .height(dimens.extraSmall)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
fun OnboardingPrimaryButton(
    currentStep: OnboardingStep,
    isEnabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    Button(
        onClick = onClick,
        enabled = isEnabled && !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.onboardingButtonHeight),
        shape = RoundedCornerShape(dimens.radiusLarge),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = appColors.ghostLine.copy(alpha = 0.35f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        ),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                strokeWidth = dimens.xxSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(
                text = if (currentStep == OnboardingStep.BUDGET) {
                    stringResource(Res.string.onboarding_get_started)
                } else {
                    stringResource(Res.string.onboarding_next)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun OnboardingSecondaryTextButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
