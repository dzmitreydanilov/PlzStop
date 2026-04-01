package com.please.stop.app.navigation.bottomnavbar

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey
import com.dog.care.navigation.routes.MainBottomTabs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Intent to navigate to a specific bottom tab, optionally with a nested route.
 *
 * Used to bridge navigation from global routes to nested bottom navigation routes.
 *
 * @param targetTab The bottom navigation tab to switch to
 * @param nestedRoute Optional route to push within the target tab's backstack
 */
data class BottomNavIntent(
    val targetTab: MainBottomTabs,
    val nestedRoute: NavKey? = null
)

/**
 * Holder for bottom navigation intents that allows communication between
 * the root navigation scope and the nested bottom navigation scope.
 *
 * ## Threading Assumptions
 * This interface is designed to be used from Compose's Main dispatcher only.
 * All methods should be called from the same thread (UI thread).
 *
 * ## Guarantees
 * - **Last-write-wins**: Multiple rapid [setIntent] calls will result in only the last
 *   intent being processed (StateFlow behavior)
 * - **Single consumption**: [consumeNestedRoute] atomically reads and clears the pending route
 * - **Flow safety**: Collectors see consistent snapshots of state
 *
 * ## Not Thread-Safe For
 * - Multi-threaded producers calling [setIntent] concurrently
 * - Multiple consumers calling [consumeNestedRoute] concurrently
 *
 * These scenarios should not occur in normal Compose usage where all calls
 * happen on the Main dispatcher.
 */
interface IBottomNavIntentHolder {
    /**
     * Flow of navigation intents. Emits when [setIntent] is called.
     * Cleared when [clearIntent] is called.
     */
    val intent: StateFlow<BottomNavIntent?>

    /**
     * Flow of pending nested routes. Updated when [setIntent] is called.
     * Cleared when [consumeNestedRoute] is called.
     */
    val pendingNestedRoute: StateFlow<NavKey?>

    /**
     * Sets a new navigation intent.
     *
     * This updates both [intent] and [pendingNestedRoute] flows.
     * If called multiple times rapidly, only the last intent is guaranteed to be processed.
     *
     * @param intent The navigation intent containing target tab and optional nested route
     */
    fun setIntent(intent: BottomNavIntent)

    /**
     * Clears the current intent.
     *
     * **Note:** This only clears [intent], not [pendingNestedRoute].
     * The pending route remains available for [consumeNestedRoute].
     */
    fun clearIntent()

    /**
     * Atomically reads and clears the pending nested route.
     *
     * This is a consume operation - the route is returned once and then cleared.
     * Subsequent calls return null until [setIntent] is called again with a nested route.
     *
     * @return The pending nested route, or null if none exists
     */
    fun consumeNestedRoute(): NavKey?
}

/**
 * Default implementation of [IBottomNavIntentHolder].
 *
 * Uses [MutableStateFlow] for state management. Individual StateFlow operations
 * are thread-safe, but compound operations (like [setIntent] which writes to two flows)
 * are not atomic.
 *
 * **Thread Safety:** Designed for single-threaded use (Main dispatcher).
 * Do not call from multiple threads concurrently.
 */
class BottomNavIntentHolder : IBottomNavIntentHolder {

    final override val intent: StateFlow<BottomNavIntent?>
        field = MutableStateFlow(null)

    final override val pendingNestedRoute: StateFlow<NavKey?>
        field = MutableStateFlow(null)

    override fun setIntent(intent: BottomNavIntent) {
        this.intent.value = intent
        pendingNestedRoute.value = intent.nestedRoute
    }

    override fun clearIntent() {
        intent.value = null
    }

    override fun consumeNestedRoute(): NavKey? {
        val route = pendingNestedRoute.value
        pendingNestedRoute.value = null
        return route
    }
}

/**
 * CompositionLocal for providing [IBottomNavIntentHolder] down the composition tree.
 */
val LocalBottomNavIntentHolder = compositionLocalOf<IBottomNavIntentHolder?> { null }
