package com.please.stop.app.uicomponents.snackbar.core

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.time.Duration

@Suppress("TopLevelComposableFunctions")
object SnackbarBoxDefaults {
    /**
     * Creates an [EnterTransition] based on the specified [alignment] and [animationDuration].
     *
     * If the alignment is vertically at the top, the transition enters from the top.
     * Otherwise, it enters from the bottom. The transition duration is specified by [animationDuration].
     *
     * @param alignment The alignment indicating the vertical position (top or bottom).
     * @param animationDuration The duration of the animation.
     * @return An [EnterTransition] configured based on the alignment.
     */
    public fun enterTransition(
        alignment: Alignment,
        animationDuration: Duration
    ): EnterTransition {
        return if (alignment.isTopVertical()) {
            enterFromTopTransition(
                animationSpec = tween(durationMillis = animationDuration.inWholeMilliseconds.toInt())
            )
        } else {
            enterFromBottomTransition(
                animationSpec = tween(durationMillis = animationDuration.inWholeMilliseconds.toInt())
            )
        }
    }

    /**
     * Creates an [ExitTransition] based on the specified [alignment] and [animationDuration].
     *
     * If the alignment is vertically at the top, the transition exits to the top.
     * Otherwise, it exits to the bottom. The transition duration is specified by [animationDuration].
     *
     * @param alignment The alignment indicating the vertical position (top or bottom).
     * @param animationDuration The duration of the animation.
     * @return An [ExitTransition] configured based on the alignment.
     */
    public fun exitTransition(
        alignment: Alignment,
        animationDuration: Duration
    ): ExitTransition {
        return if (alignment.isTopVertical()) {
            exitToTopTransition(
                animationSpec = tween(durationMillis = animationDuration.inWholeMilliseconds.toInt())
            )
        } else {
            exitToBottomTransition(
                animationSpec = tween(durationMillis = animationDuration.inWholeMilliseconds.toInt())
            )
        }
    }

    /**
     * Creates animated [PaddingValues] based on the specified [alignment].
     *
     * This function calculates the appropriate padding to apply based on the alignment and any
     * registered offsets from composables using the [com.please.stop.app.uicomponents.snackbar.core.noOverlapTopContentBySnackbar] or
     * [com.please.stop.app.uicomponents.snackbar.core.noOverlapBottomContentBySnackbar] modifiers. The padding is animated to provide a smooth
     * transition when the offset changes.
     *
     * @param alignment The alignment indicating the vertical position (top or bottom).
     * @param windowInsets The window insets to apply to the padding.
     * @return [PaddingValues] with animated padding applied to the appropriate edge based on alignment.
     */
    @Composable
    fun animateAdditionalPadding(
        alignment: Alignment,
        windowInsets: WindowInsets? = null
    ): PaddingValues {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val localPadding = if (alignment.isTopVertical()) {
            com.please.stop.app.uicomponents.snackbar.core.LocalTopMessengerOffset.current.values.maxOrNull()
        } else {
            com.please.stop.app.uicomponents.snackbar.core.LocalBottomMessengerOffset.current.values.maxOrNull()
        }
        val localPaddingInDp = with(density) {
            (localPadding ?: 0).toDp()
        }
        val animatedPadding by animateDpAsState(localPaddingInDp, animationSpec = tween())

        val insetsPadding = windowInsets?.asPaddingValues(density) ?: PaddingValues(0.dp)

        return if (alignment.isTopVertical()) {
            PaddingValues(
                top = animatedPadding + insetsPadding.calculateTopPadding(),
                bottom = insetsPadding.calculateBottomPadding(),
                start = insetsPadding.calculateStartPadding(layoutDirection),
                end = insetsPadding.calculateEndPadding(layoutDirection)
            )
        } else {
            PaddingValues(
                top = insetsPadding.calculateTopPadding(),
                bottom = animatedPadding + insetsPadding.calculateBottomPadding(),
                start = insetsPadding.calculateStartPadding(layoutDirection),
                end = insetsPadding.calculateEndPadding(layoutDirection)
            )
        }
    }

    private fun enterFromBottomTransition(animationSpec: FiniteAnimationSpec<IntOffset>): EnterTransition {
        return slideInVertically(
            animationSpec = animationSpec,
            initialOffsetY = { it }
        )
    }

    private fun enterFromTopTransition(animationSpec: FiniteAnimationSpec<IntOffset>): EnterTransition {
        return slideInVertically(
            animationSpec = animationSpec,
            initialOffsetY = { -it }
        )
    }

    private fun exitToBottomTransition(animationSpec: FiniteAnimationSpec<IntOffset>): ExitTransition {
        return slideOutVertically(
            animationSpec = animationSpec,
            targetOffsetY = { it }
        )
    }

    private fun exitToTopTransition(animationSpec: FiniteAnimationSpec<IntOffset>): ExitTransition {
        return slideOutVertically(
            animationSpec = animationSpec,
            targetOffsetY = { -it }
        )
    }

    private fun Alignment.isTopVertical(): Boolean {
        return when (this) {
            Alignment.TopCenter,
            Alignment.TopStart,
            Alignment.TopEnd,
            Alignment.Top -> true

            else -> false
        }
    }
}
