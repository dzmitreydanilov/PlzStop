package com.please.stop.app.navigation.animation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * Provides the [SharedTransitionScope] from the root [SharedTransitionLayout]
 * so that deeply nested composables can apply [sharedElement] modifiers.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * Provides the root [NavDisplay]'s [AnimatedContentScope].
 *
 * Inside nested NavDisplays (e.g. bottom-tabs), [LocalNavAnimatedContentScope] is overridden
 * by the inner NavDisplay. This local preserves the root scope so that shared elements
 * spanning the root transition can use the correct animation scope.
 */
val LocalRootAnimatedContentScope = compositionLocalOf<AnimatedContentScope?> { null }

const val SHARED_ELEMENT_KEY_PET_AVATAR = "pet-avatar"
