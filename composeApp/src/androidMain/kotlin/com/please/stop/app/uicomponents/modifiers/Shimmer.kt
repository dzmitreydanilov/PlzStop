package com.dog.care.utils.uicomponents.modifiers

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dog.care.theme.ExpandedDimens
import com.dog.care.theme.LocalAppDimens
import com.dog.care.theme.MediumDimens

private const val SHIMMER_TRANSLATE_START = -400f
private const val SHIMMER_TRANSLATE_END = 1200f
private const val SHIMMER_DURATION_MS = 1600
private const val SHIMMER_WIDTH_DIVISOR = 1.5f

@Composable
fun Modifier.shimmer(cornerRadius: Dp = 0.dp): Modifier {
    val colors = MaterialTheme.colorScheme
    val shimmerColors = listOf(
        colors.surfaceVariant.copy(alpha = 0.60f),
        colors.surface.copy(alpha = 0.90f),
        colors.surfaceVariant.copy(alpha = 0.60f)
    )
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = SHIMMER_TRANSLATE_START,
        targetValue = SHIMMER_TRANSLATE_END,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SHIMMER_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        ),
        label = "Translate"
    )
    return this.drawWithCache {
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim, 0f),
            end = Offset(translateAnim + size.width / SHIMMER_WIDTH_DIVISOR, size.height)
        )
        val cornerPx = cornerRadius.toPx()
        onDrawWithContent {
            drawRoundRect(
                brush = brush,
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                size = size
            )
        }
    }
}

@Composable
fun Modifier.shimmer(
    isLoading: Boolean,
    cornerRadius: Dp = 0.dp
): Modifier {
    return if (isLoading) this.shimmer(cornerRadius = cornerRadius) else this
}

@Composable
fun ShimmerItem(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize()
            .shimmer(cornerRadius = cornerRadius, isLoading = isLoading)
    ) {
        if (!isLoading) content()
    }
}

/**
 * Responsive shimmer dimensions based on current WindowSizeClass.
 * Provides consistent sizing for shimmer placeholders across different screen sizes.
 */
@Immutable
data class ShimmerDimens(
    val textHeightSmall: Dp,
    val textHeightMedium: Dp,
    val textHeightLarge: Dp,
    val avatarSize: Dp,
    val buttonHeight: Dp,
    val cardMinHeight: Dp,
    val cornerRadiusSmall: Dp,
    val cornerRadiusMedium: Dp,
    val cornerRadiusLarge: Dp,
    val cornerRadiusCircle: Dp
)

/**
 * Returns responsive [ShimmerDimens] based on current [LocalAppDimens].
 */
@Composable
fun rememberShimmerDimens(): ShimmerDimens {
    val dimens = LocalAppDimens.current
    return remember(dimens) {
        when (dimens) {
            ExpandedDimens -> ShimmerDimens(
                textHeightSmall = 18.dp,
                textHeightMedium = 24.dp,
                textHeightLarge = 32.dp,
                avatarSize = 56.dp,
                buttonHeight = 52.dp,
                cardMinHeight = 160.dp,
                cornerRadiusSmall = 4.dp,
                cornerRadiusMedium = 8.dp,
                cornerRadiusLarge = 12.dp,
                cornerRadiusCircle = 50.dp
            )
            MediumDimens -> ShimmerDimens(
                textHeightSmall = 16.dp,
                textHeightMedium = 22.dp,
                textHeightLarge = 28.dp,
                avatarSize = 50.dp,
                buttonHeight = 48.dp,
                cardMinHeight = 150.dp,
                cornerRadiusSmall = 4.dp,
                cornerRadiusMedium = 8.dp,
                cornerRadiusLarge = 12.dp,
                cornerRadiusCircle = 50.dp
            )
            else -> ShimmerDimens(
                textHeightSmall = 14.dp,
                textHeightMedium = 20.dp,
                textHeightLarge = 24.dp,
                avatarSize = 44.dp,
                buttonHeight = 44.dp,
                cardMinHeight = 140.dp,
                cornerRadiusSmall = 4.dp,
                cornerRadiusMedium = 8.dp,
                cornerRadiusLarge = 12.dp,
                cornerRadiusCircle = 50.dp
            )
        }
    }
}

/**
 * Reusable shimmer text placeholder with responsive sizing.
 *
 * @param widthFraction Fraction of parent width (0.0 to 1.0)
 * @param height Height of the placeholder
 * @param modifier Additional modifiers
 * @param cornerRadius Corner radius for the shimmer effect
 */
@Composable
fun ShimmerTextPlaceholder(
    widthFraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .shimmer(cornerRadius = cornerRadius)
    )
}
