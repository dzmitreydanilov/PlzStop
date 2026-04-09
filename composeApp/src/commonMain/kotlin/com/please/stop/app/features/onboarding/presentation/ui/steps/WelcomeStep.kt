package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.theme.LocalAppDimens
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_app_name
import plzstop.composeapp.generated.resources.onboarding_tagline

@Composable
fun WelcomeStep() {
    val appColors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val tagline = stringResource(Res.string.onboarding_tagline)
    val heading = tagline.substringBefore('\n').ifBlank { tagline }
    val description = tagline.substringAfter('\n', "")

    Text(
        text = stringResource(Res.string.onboarding_app_name),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(dimens.onboardingSectionSpacing))

    Box(
        modifier = Modifier
            .size(dimens.onboardingLogoSize)
            .clip(RoundedCornerShape(dimens.onboardingSectionSpacing))
            .background(brush = appColors.primaryGradient)
            .border(1.dp, appColors.cardGlassBorder, RoundedCornerShape(dimens.onboardingSectionSpacing)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\uD83D\uDE80",
            style = MaterialTheme.typography.headlineMedium,
        )
    }

    Spacer(modifier = Modifier.height(dimens.onboardingSectionSpacing))

    Text(
        text = heading,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(dimens.extraSmall))

    Text(
        text = description.ifBlank { tagline },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
