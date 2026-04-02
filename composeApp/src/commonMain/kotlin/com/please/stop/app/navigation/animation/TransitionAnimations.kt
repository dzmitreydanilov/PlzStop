package com.please.stop.app.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

/**
 * Default duration for slide navigation transitions (push/pop).
 * Also used by the engagement dialog cleanup to stay in sync with navigation timing.
 */
const val NAV_SLIDE_TRANSITION_DURATION_MS = 300

private const val BOTTOM_SHEET_ANIMATION_DURATION_MS = 400

fun predictivePopTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform =
    {
        // Slide in from left when navigating back
        slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(NAV_SLIDE_TRANSITION_DURATION_MS)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(NAV_SLIDE_TRANSITION_DURATION_MS)
        )
    }

fun slideOutTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        // Slide in from left when navigating back
        slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(NAV_SLIDE_TRANSITION_DURATION_MS)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(NAV_SLIDE_TRANSITION_DURATION_MS)
        )
    }

fun slideInTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        // Slide in from right when navigating forward
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(NAV_SLIDE_TRANSITION_DURATION_MS)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(NAV_SLIDE_TRANSITION_DURATION_MS)
        )
    }

fun bottomSheetEnterTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        // Slide in from bottom
        slideInVertically(
            initialOffsetY = { it }, // 'it' is the height of the content
            animationSpec = tween()
        ) togetherWith slideOutVertically(
            targetOffsetY = { -it / 3 }, // Move old content up slightly (parallax)
            animationSpec = tween()
        ) + fadeOut(
            animationSpec = tween() // Optional: fade out the background scene
        )
    }

fun bottomSheetPopTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        // Bring the background scene back down and fade it in
        slideInVertically(
            initialOffsetY = { -it / 3 }, // Start slightly shifted up
            animationSpec = tween()
        ) + fadeIn(
            animationSpec = tween()
        ) togetherWith slideOutVertically(
            targetOffsetY = { it }, // Slide the sheet down off-screen
            animationSpec = tween()
        )
    }

fun bottomSheetPredictivePopSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform =
    {
        // Same mechanics as standard pop: Slide sheet down, background comes in from top
        slideInVertically(
            initialOffsetY = { -it / 3 },
            animationSpec = tween()
        ) + fadeIn(
            animationSpec = tween()
        ) togetherWith slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween()
        )
    }

private const val CROSSFADE_DURATION_MS = 200

fun crossfadeTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        fadeIn(animationSpec = tween(CROSSFADE_DURATION_MS)) togetherWith
            fadeOut(animationSpec = tween(CROSSFADE_DURATION_MS))
    }

fun crossfadePredictivePopSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform =
    {
        fadeIn(animationSpec = tween(CROSSFADE_DURATION_MS)) togetherWith
            fadeOut(animationSpec = tween(CROSSFADE_DURATION_MS))
    }

val bottomSheetAnimationSpecs = NavDisplay.transitionSpec {
    // Enter: Slide new content (sheet) up.
    // We use KeepUntilTransitionsFinished to ensure the background stays visible
    // while the sheet slides over it.
    slideInVertically(
        initialOffsetY = { it }, // Start from bottom of screen
        animationSpec = tween(BOTTOM_SHEET_ANIMATION_DURATION_MS)
    ) togetherWith ExitTransition.KeepUntilTransitionsFinished
} + NavDisplay.popTransitionSpec {
    // Pop: Slide old content (sheet) down.
    // We use EnterTransition.None so the background is static/instant
    // (it is revealed as the sheet slides away).
    EnterTransition.None togetherWith
        slideOutVertically(
            targetOffsetY = { it }, // Slide off to bottom
            animationSpec = tween(BOTTOM_SHEET_ANIMATION_DURATION_MS)
        )
} + NavDisplay.predictivePopTransitionSpec { _ ->
    // Predictive: Same physics as Pop.
    EnterTransition.None togetherWith
        slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(BOTTOM_SHEET_ANIMATION_DURATION_MS)
        )
}
