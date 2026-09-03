package com.please.stop.app.uicomponents.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

private const val SCALE_INITIAL_VALUE = 0.8f
private const val SCALE_FADE_DURATION_MILLIS = 300
private const val PULSE_DURATION_MILLIS = 2_000
private const val SHIMMER_DURATION_MILLIS = 2_500
private const val SHIMMER_WIDTH_FRACTION = 0.4f

@Composable
fun FadeSlideIn(
    delayMillis: Int = 0,
    durationMillis: Int = 400,
    initialOffsetY: Float = 30f,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(initialOffsetY) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        alpha.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        offsetY.animateTo(0f, tween(durationMillis, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        },
    ) {
        content()
    }
}

@Composable
fun ScaleIn(
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val scale = remember { Animatable(SCALE_INITIAL_VALUE) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
    }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        alpha.animateTo(1f, tween(SCALE_FADE_DURATION_MILLIS))
    }

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        },
    ) {
        content()
    }
}

@Suppress("UnusedParameter")
@Composable
fun PulseGlow(
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    content: @Composable () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_DURATION_MILLIS),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_DURATION_MILLIS),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
            alpha = pulseAlpha
        },
    ) {
        content()
    }
}

fun Modifier.shimmerEffect(): Modifier {
    return this.then(ShimmerModifier())
}

@Composable
fun rememberShimmerOffset(): Float {
    val transition = rememberInfiniteTransition()
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(SHIMMER_DURATION_MILLIS),
        ),
    )
    return offset
}

private class ShimmerModifier : Modifier.Element

fun Modifier.shimmerOverlay(
    shimmerOffset: Float,
    color: Color,
): Modifier = this.drawWithContent {
    drawContent()
    val width = size.width
    val start = width * shimmerOffset
    val brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, color, Color.Transparent),
        start = Offset(start, 0f),
        end = Offset(start + width * SHIMMER_WIDTH_FRACTION, size.height),
    )
    drawRect(brush = brush)
}
