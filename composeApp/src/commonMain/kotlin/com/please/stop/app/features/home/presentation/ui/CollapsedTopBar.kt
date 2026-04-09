package com.please.stop.app.features.home.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.theme.LocalAppDimens
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

internal val CollapsedBarHeight = 56.dp

@Composable
internal fun CollapsingHomeHeader(
    displayName: String?,
    totalSpentFormatted: String,
    onProfileClicked: () -> Unit,
    currentHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val isCollapsed = currentHeight != Dp.Unspecified && currentHeight <= CollapsedBarHeight
    val fraction = if (isCollapsed) 1f else 0f

    val heightModifier = if (currentHeight == Dp.Unspecified) {
        Modifier
    } else {
        Modifier.height(currentHeight)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CollapsedBarHeight)
            .then(heightModifier)
            .clipToBounds()
            .background(appColors.headerGradient),
    ) {
        // Expanded content
        ExpandedContent(
            displayName = displayName,
            totalSpentFormatted = totalSpentFormatted,
            onProfileClicked = onProfileClicked,
            avatarSize = dimens.homeHeaderAvatarExpandedSize,
            modifier = Modifier.graphicsLayer { alpha = 1f - fraction },
        )

        // Collapsed bar pinned to top
        CollapsedBar(
            totalSpentFormatted = totalSpentFormatted,
            displayName = displayName,
            onProfileClicked = onProfileClicked,
            avatarSize = dimens.homeHeaderAvatarCollapsedSize,
            modifier = Modifier.graphicsLayer { alpha = fraction },
        )
    }
}

@Composable
private fun ExpandedContent(
    displayName: String?,
    totalSpentFormatted: String,
    onProfileClicked: () -> Unit,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
) {
    val shimmerOffset = rememberShimmerOffset()
    val appColors = LocalAppColors.current
    val dimens = LocalAppDimens.current

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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.small3, vertical = dimens.small2 + dimens.extraSmall),
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
                Spacer(modifier = Modifier.height(dimens.xxSmall))
                Text(
                    text = if (!displayName.isNullOrBlank()) displayName
                    else stringResource(Res.string.home_greeting_default),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            InitialAvatar(name = displayName, size = avatarSize, onClick = onProfileClicked)
        }

        Spacer(modifier = Modifier.height(dimens.small3))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = appColors.cardGlass,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(appColors.glassCardGradient)
                    .border(1.dp, appColors.cardGlassBorder, MaterialTheme.shapes.large),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.small3),
                ) {
                    Text(
                        text = stringResource(Res.string.home_total_spent),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = CARD_LABEL_ALPHA),
                    )
                    Spacer(modifier = Modifier.height(dimens.xSmall))
                    Text(
                        text = totalSpentFormatted,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(dimens.xSmall))
                    Text(
                        text = stringResource(Res.string.home_spent_this_month, totalSpentFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = CARD_LABEL_ALPHA),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedBar(
    totalSpentFormatted: String,
    displayName: String?,
    onProfileClicked: () -> Unit,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CollapsedBarHeight)
            .padding(horizontal = dimens.small3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.home_spent_this_month, totalSpentFormatted),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(dimens.small1))
        InitialAvatar(name = displayName, size = avatarSize, onClick = onProfileClicked)
    }
}

@Composable
private fun InitialAvatar(
    name: String?,
    size: Dp,
    onClick: () -> Unit,
) {
    val initial = name?.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = AVATAR_BG_ALPHA))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = if (size >= 44.dp) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}
