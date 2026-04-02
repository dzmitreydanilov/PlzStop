package com.please.stop.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Collects a Flow of one-off navigation events in a lifecycle-aware way.
 *
 * This helper ties a Flow to the Composition and collects it only when the
 * lifecycle is at least STARTED, preventing navigation calls when the UI is not visible.
 * It is useful when your StateHolder exposes a Flow of navigation commands or routes.
 *
 * Example:
 *
 * val stateHolder: MyStateHolder = remember { ... }
 * CollectNavigationFlow(
 *     flow = stateHolder.navigation,
 *     onNavigate = { event ->
 *         when (event) {
 *             is UserRoutes -> router.push(event)
 *             is AuthRoute -> router.replaceCurrent(event)
 *         }
 *     }
 * )
 *
 * @param flow The flow of navigation events to collect
 * @param key1 Optional key to restart collection when changed
 * @param key2 Optional key to restart collection when changed
 * @param onNavigate Callback invoked for each collected event
 */
@Suppress("ComposableParametersOrdering")
@Composable
fun <T> CollectNavigationFlow(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onNavigate: (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(key1 = lifecycleOwner.lifecycle, key1, key2) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest(onNavigate)
        }
    }
}
