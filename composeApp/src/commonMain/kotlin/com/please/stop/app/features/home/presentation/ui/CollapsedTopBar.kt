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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.ANIMATION_DURATION_MS
import com.please.stop.app.uicomponents.animation.rememberShimmerOffset
import com.please.stop.app.uicomponents.animation.shimmerOverlay
import com.please.stop.app.uicomponents.previews.ApplicationPreviewThemeWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.home_greeting_default
import plzstop.composeapp.generated.resources.home_spent_this_month
import plzstop.composeapp.generated.resources.home_total_spent
import plzstop.composeapp.generated.resources.home_welcome_back
import kotlin.time.Duration.Companion.milliseconds

private const val CARD_STAGGER_DELAY_MS = 200L
private const val CARD_INITIAL_SCALE = 0.92f
private const val HEADER_SLIDE_OFFSET_PX = 20
private const val SUBTITLE_ALPHA = 0.8f
private const val CARD_LABEL_ALPHA = 0.7f
private const val SHIMMER_ALPHA = 0.08f
private const val CARD_COLLAPSE_SCALE_DELTA = 0.06f
private const val CARD_COLLAPSE_TRANSLATION_Y_PX = 18f

internal val CollapsedBarHeight = 56.dp

private val HeaderHorizontalPadding = 20.dp
private val HeaderVerticalPadding = 24.dp
private val ExpandedAvatarSize = 44.dp
private val CollapsedAvatarSize = 36.dp
private val ExpandedSummaryTopPadding = 188.dp
private val CollapsedSummaryTopPadding = 16.dp

@Composable
internal fun CollapsingHomeHeader(
    displayName: String?,
    totalSpentFormatted: String,
    onProfileClicked: () -> Unit,
    currentHeight: Dp,
    expandedHeight: Dp,
    onExpandedHeightMeasured: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val collapseFraction = when {
        currentHeight == Dp.Unspecified || expandedHeight <= CollapsedBarHeight -> 0f
        else -> {
            val expandedRange = expandedHeight - CollapsedBarHeight
            ((expandedHeight - currentHeight) / expandedRange).coerceIn(0f, 1f)
        }
    }
    val avatarTopPadding = interpolate(
        start = HeaderVerticalPadding,
        stop = (CollapsedBarHeight - CollapsedAvatarSize) * 0.5f,
        fraction = collapseFraction,
    )
    val avatarScale = interpolate(
        start = 1f,
        stop = CollapsedAvatarSize.value / ExpandedAvatarSize.value,
        fraction = collapseFraction,
    )
    val summaryTopPadding = interpolate(
        start = ExpandedSummaryTopPadding,
        stop = CollapsedSummaryTopPadding,
        fraction = collapseFraction,
    )
    val summaryStartPadding = interpolate(
        start = HeaderHorizontalPadding + 20.dp,
        stop = HeaderHorizontalPadding,
        fraction = collapseFraction,
    )

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
        ExpandedContent(
            displayName = displayName,
            totalSpentFormatted = totalSpentFormatted,
            collapseFraction = collapseFraction,
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    onExpandedHeightMeasured(coordinates.size.height.toFloat())
                },
        )
        SpendSummaryText(
            text = stringResource(Res.string.home_spent_this_month, totalSpentFormatted),
            collapseFraction = collapseFraction,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = summaryStartPadding,
                    top = summaryTopPadding,
                    end = HeaderHorizontalPadding + CollapsedAvatarSize + 12.dp,
                )
                .fillMaxWidth(),
        )
        InitialAvatar(
            name = displayName,
            size = ExpandedAvatarSize,
            onClick = onProfileClicked,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = avatarTopPadding, end = HeaderHorizontalPadding)
                .graphicsLayer {
                    scaleX = avatarScale
                    scaleY = avatarScale
                    transformOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0f)
                },
        )
    }
}

@Composable
private fun ExpandedContent(
    displayName: String?,
    totalSpentFormatted: String,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
) {
    val shimmerOffset = rememberShimmerOffset()
    val appColors = LocalAppColors.current

    val headerProgress = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(CARD_INITIAL_SCALE) }
    val cardCollapseFraction = collapseFraction.fastOut()
    val cardContentAlpha = (1f - collapseFraction * 2f).coerceIn(0f, 1f)

    LaunchedEffect(Unit) {
        launch { headerProgress.animateTo(1f, tween(ANIMATION_DURATION_MS)) }
        delay(CARD_STAGGER_DELAY_MS.milliseconds)
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
            .padding(horizontal = HeaderHorizontalPadding, vertical = HeaderVerticalPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = headerProgress.value * (1f - collapseFraction)
                    translationY = (1f - headerProgress.value) * HEADER_SLIDE_OFFSET_PX
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.home_welcome_back),
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.headerContent.copy(alpha = SUBTITLE_ALPHA),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (!displayName.isNullOrBlank()) displayName
                    else stringResource(Res.string.home_greeting_default),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = appColors.headerContent,
                )
            }
            Spacer(modifier = Modifier.width(ExpandedAvatarSize))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = appColors.headerContainer,
            ),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .graphicsLayer {
                    alpha = cardAlpha.value * (1f - cardCollapseFraction)
                    scaleX = cardScale.value * (1f - CARD_COLLAPSE_SCALE_DELTA * cardCollapseFraction)
                    scaleY = cardScale.value * (1f - CARD_COLLAPSE_SCALE_DELTA * cardCollapseFraction)
                    translationY = -CARD_COLLAPSE_TRANSLATION_Y_PX * cardCollapseFraction
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
                }
                .shimmerOverlay(shimmerOffset, appColors.headerContent.copy(alpha = SHIMMER_ALPHA)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(Res.string.home_total_spent),
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.headerContent.copy(alpha = CARD_LABEL_ALPHA * cardContentAlpha),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalSpentFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = appColors.headerContent.copy(alpha = cardContentAlpha),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SpendSummaryText(
    text: String,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current

    Text(
        text = text,
        style = lerp(
            start = MaterialTheme.typography.bodySmall,
            stop = MaterialTheme.typography.titleMedium,
            fraction = collapseFraction,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = FontWeight.SemiBold,
        color = appColors.headerContent.copy(
            alpha = CARD_LABEL_ALPHA + (1f - CARD_LABEL_ALPHA) * collapseFraction,
        ),
        modifier = modifier,
    )
}

@Composable
private fun InitialAvatar(
    name: String?,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initial = name?.firstOrNull()?.uppercase() ?: "?"
    val appColors = LocalAppColors.current

    Box(
        modifier = modifier
            .requiredSize(size)
            .clip(CircleShape)
            .background(appColors.headerAvatarContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = if (size >= ExpandedAvatarSize) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
            color = appColors.headerContent,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun interpolate(start: Dp, stop: Dp, fraction: Float): Dp =
    start + (stop - start) * fraction

private fun interpolate(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun Float.fastOut(): Float =
    this * this

@Preview(showBackground = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun CollapsingHomeHeaderExpandedPreview() {
    CollapsingHomeHeaderPreview(currentHeight = 220.dp)
}

@Preview(showBackground = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun CollapsingHomeHeaderMidCollapsePreview() {
    CollapsingHomeHeaderPreview(currentHeight = 138.dp)
}

@Preview(showBackground = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun CollapsingHomeHeaderCollapsedPreview() {
    CollapsingHomeHeaderPreview(currentHeight = CollapsedBarHeight)
}

@Composable
private fun CollapsingHomeHeaderPreview(currentHeight: Dp) {
    CollapsingHomeHeader(
        displayName = null,
        totalSpentFormatted = "$1,248.32",
        onProfileClicked = {},
        currentHeight = currentHeight,
        expandedHeight = 220.dp,
        onExpandedHeightMeasured = {},
    )
}
