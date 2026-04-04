package com.please.stop.app.features.home.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.ANIMATION_DURATION_MS
import com.please.stop.app.uicomponents.animation.rememberShimmerOffset
import com.please.stop.app.uicomponents.animation.shimmerOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.home_greeting_default
import plzstop.composeapp.generated.resources.home_spent_this_month
import plzstop.composeapp.generated.resources.home_total_spent
import plzstop.composeapp.generated.resources.home_welcome_back

private const val CARD_STAGGER_DELAY_MS = 200L
private const val CARD_INITIAL_SCALE = 0.92f
private const val HEADER_SLIDE_OFFSET_PX = 20
private const val SUBTITLE_ALPHA = 0.8f
private const val CARD_LABEL_ALPHA = 0.7f
private const val SHIMMER_ALPHA = 0.08f
private const val AVATAR_BG_ALPHA = 0.25f

@Composable
internal fun HomeHeader(
    displayName: String?,
    totalSpentFormatted: String,
    onProfileClicked: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val shimmerOffset = rememberShimmerOffset()

    val headerProgress = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(CARD_INITIAL_SCALE) }

    LaunchedEffect(Unit) {
        launch { headerProgress.animateTo(1f, tween(ANIMATION_DURATION_MS)) }
        delay(CARD_STAGGER_DELAY_MS)
        launch { cardAlpha.animateTo(1f, tween(ANIMATION_DURATION_MS)) }
        cardScale.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(appColors.headerGradient)
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = headerProgress.value
                    translationY = (1f - headerProgress.value) * HEADER_SLIDE_OFFSET_PX
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.home_welcome_back),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = SUBTITLE_ALPHA),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (!displayName.isNullOrBlank()) displayName
                    else stringResource(Res.string.home_greeting_default),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            InitialAvatar(name = displayName, onClick = onProfileClicked)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f),
            ),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .graphicsLayer {
                    alpha = cardAlpha.value
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                }
                .shimmerOverlay(shimmerOffset, Color.White.copy(alpha = SHIMMER_ALPHA)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(Res.string.home_total_spent),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = CARD_LABEL_ALPHA),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalSpentFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.home_spent_this_month, totalSpentFormatted),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = CARD_LABEL_ALPHA),
                )
            }
        }
    }
}

@Composable
private fun InitialAvatar(
    name: String?,
    onClick: () -> Unit,
) {
    val initial = name?.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = AVATAR_BG_ALPHA))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeHeaderPreview() {
    AppTheme {
        HomeHeader(
            displayName = "Dmitry",
            totalSpentFormatted = "$1,234.56",
            onProfileClicked = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeHeaderNoNamePreview() {
    AppTheme {
        HomeHeader(
            displayName = null,
            totalSpentFormatted = "$0.00",
            onProfileClicked = {},
        )
    }
}
