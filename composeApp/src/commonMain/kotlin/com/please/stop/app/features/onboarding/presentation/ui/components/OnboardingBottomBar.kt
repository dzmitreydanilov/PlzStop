package com.please.stop.app.features.onboarding.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.onboarding.presentation.OnboardingStep
import com.please.stop.app.theme.LocalAppColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_get_started
import plzstop.composeapp.generated.resources.onboarding_next
import plzstop.composeapp.generated.resources.onboarding_bg_network

@Composable
fun OnboardingCenteredContent(
    content: @Composable () -> Unit,
) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAFF)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(Res.drawable.onboarding_bg_network),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.24f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            appColors.blue400.copy(alpha = 0.58f),
                            Color(0xFFEAF3FF).copy(alpha = 0.82f),
                            Color.White.copy(alpha = 0.92f),
                        ),
                    )
                ),
        )
        Box(
            modifier = Modifier.fillMaxWidth(0.66f),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun OnboardingStepIndicator(
    currentStep: OnboardingStep,
) {
    val appColors = LocalAppColors.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingStep.entries.forEach { step ->
            val isActive = step.ordinal <= currentStep.ordinal
            val isCurrent = step == currentStep

            val width by animateDpAsState(
                targetValue = if (isCurrent) 24.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            val color by animateColorAsState(
                targetValue = if (isActive) appColors.teal500 else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            )

            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
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
    Button(
        onClick = onClick,
        enabled = isEnabled && !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = appColors.teal500,
            contentColor = Color.White,
        ),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(
                text = if (currentStep == OnboardingStep.CATEGORIES) {
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
