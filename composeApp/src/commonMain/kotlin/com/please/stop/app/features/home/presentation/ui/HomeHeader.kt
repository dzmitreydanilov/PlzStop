package com.please.stop.app.features.home.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.animation.rememberShimmerOffset
import com.please.stop.app.uicomponents.animation.shimmerOverlay
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.home_greeting_default
import plzstop.composeapp.generated.resources.home_spent_this_month

@Composable
internal fun HomeHeader(
    displayName: String?,
    totalSpentFormatted: String,
    onProfileClicked: () -> Unit,
) {
    val appColors = LocalAppColors.current

    val headerAlpha = remember { Animatable(0f) }
    val headerOffsetY = remember { Animatable(20f) }
    val cardScale = remember { Animatable(0.92f) }
    val cardAlpha = remember { Animatable(0f) }
    val shimmerOffset = rememberShimmerOffset()

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(400))
    }
    LaunchedEffect(Unit) {
        headerOffsetY.animateTo(0f, tween(400))
    }
    LaunchedEffect(Unit) {
        delay(200)
        cardAlpha.animateTo(1f, tween(350))
    }
    LaunchedEffect(Unit) {
        delay(200)
        cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(appColors.headerGradient)
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = headerAlpha.value
                    translationY = headerOffsetY.value
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
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
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                    alpha = cardAlpha.value
                }
                .shimmerOverlay(shimmerOffset, Color.White.copy(alpha = 0.08f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
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
                    color = Color.White.copy(alpha = 0.7f),
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
            .background(Color.White.copy(alpha = 0.25f))
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
