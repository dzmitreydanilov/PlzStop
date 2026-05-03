package com.please.stop.app.navigation.nav3.navigators.bottom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import com.please.stop.app.navigation.nav3.LocalParentRouter
import com.please.stop.app.navigation.nav3.Navigator
import com.please.stop.app.navigation.nav3.Router
import com.please.stop.app.navigation.nav3.platformOnBack
import com.please.stop.app.navigation.nav3.rememberRouter
import kotlinx.serialization.PolymorphicSerializer

/**
 * Multi-stack state holder for a bottom navigation setup built on Navigation 3.
 *
 * What it manages:
 * - A dedicated [NavBackStack] for every top-level tab (entries in [backStacks])
 * - The currently selected top-level tab ([topLevelRoute]) with saved state support
 * - A small set of stacks to render at any moment via [stacksInUse]
 *
 * Why [stacksInUse] only returns two stacks:
 * - We keep the start tab and the currently selected tab mounted. This preserves
 *   state/animations and avoids re-creating content on each tab switch while not
 *   over-rendering all tabs at once.
 *
 * Persistence:
 * - Each tab owns its own [NavBackStack] which retains saveable state and VM store
 *   through the decorators used when materializing entries (see [toEntries]).
 */
class BottomNavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(
                startRoute,
                topLevelRoute
            )
        }
}

/**
 * Creates and remembers a [BottomNavigationState] with per-tab [NavBackStack]s.
 *
 * Saving and restoring:
 * - [topLevelRoute] is persisted via [rememberSerializable] using
 *   [MutableStateSerializer] with a polymorphic serializer for [NavKey].
 * - Each tab back stack is created with the same [configuration] to enable
 *   process-death restoration.
 *
 * You must register polymorphic serializers for all tab [NavKey] types in the
 * provided [configuration] (see examples in BottomTabsNavNavigationHost.kt).
 *
 * @param startRoute The default/root tab (e.g., Home)
 * @param topLevelRoutes Set of all top-level tab keys
 * @param configuration Saved state configuration with serializers module
 */
@Composable
fun rememberBottomNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
    configuration: SavedStateConfiguration,
): BottomNavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        configuration = configuration,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
    ) { mutableStateOf(startRoute) }

    val backStacks = topLevelRoutes.associateWith { route ->
        key(route) {
            rememberNavBackStack(configuration = configuration, elements = arrayOf(route))
        }
    }

    return remember(startRoute, topLevelRoutes) {
        BottomNavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * Converts the [BottomNavigationState] into a list of decorated [NavEntry] items.
 *
 * This function is the core of the "Multiple Backstacks" implementation. It ensures that
 * every tab's state (UI state and ViewModels) is preserved even when the user switches
 * to a different tab.
 *
 * ### State Retention Mechanism:
 * 1. **Decorators:** It applies [rememberSaveableStateHolderNavEntryDecorator] and
 * [rememberViewModelStoreNavEntryDecorator] to handle [androidx.lifecycle.ViewModel]
 * and [androidx.compose.runtime.saveable.SaveableStateHolder] persistence.
 * 2. **Stable Identity:** The [key] block uses the tab's `route` to ensure that Compose
 * associates the state with a specific tab. This prevents state "leaking" or being
 * reset during recomposition.
 * 3. **Non-Disposal:** By iterating through all [backStacks], it keeps the ViewModels
 * for background tabs "alive" in the composition tree, preventing their `onCleared()`
 * from being called.
 *
 * ### Memory & Performance Considerations:
 * * **RAM Usage:** This approach prioritizes **UX over Memory**. By keeping background
 * stacks in the composition, ViewModels and UI states are not garbage collected.
 * This results in higher RAM usage proportional to the number of tabs and the data
 * held in their respective ViewModels.
 * * **Instant Switching:** The trade-off enables "Instant Apps" behavior where switching
 * tabs requires zero re-loading or network fetching, as the state is already warm in memory.
 * * **Lifecycle:** State is only released when a route is explicitly removed from
 * the [backStacks] map, at which point the associated [ViewModelStore] is cleared.
 *
 * @param entryProvider A lambda that resolves a [NavKey] into its corresponding [NavEntry].
 * @return A list of entries for the currently active [stacksInUse], decorated for
 * state persistence.
 */
@Composable
fun BottomNavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>) =
    run {
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator()
        )

        // Compose ALL tab stacks in a stable per-route location so their ViewModelStoreOwner is not
        // disposed on tab switches, but still render ONLY [stacksInUse] in NavDisplay.
        val allEntries = mutableListOf<Pair<NavKey, List<NavEntry<NavKey>>>>()
        backStacks.forEach { (route, stack) ->
            key(route) {
                val entries = rememberDecoratedNavEntries(
                    backStack = stack,
                    entryDecorators = decorators,
                    entryProvider = entryProvider
                )
                allEntries.add(route to entries)
            }
        }

        val entriesByRoute = allEntries.toMap()
        stacksInUse
            .flatMap { route -> entriesByRoute[route].orEmpty() }
            .toMutableStateList()
    }

/**
 * Host that wires [Router] and multi-stack navigator for bottom navigation.
 *
 * It:
 * - Connects the provided [router] to a [Navigator] that understands tab switching
 * - Exposes a uniform [onBack] implementation that calls [Router.pop]
 * - Provides parent router via [LocalParentRouter] for nested graphs
 *
 * Typical usage is to call this from a bottom-tabs container and render the
 * resulting entries with NavDisplay.
 */
@Composable
fun <T : NavKey> BottomNavigationNavHost(
    state: BottomNavigationState,
    router: Router<T> = rememberRouter(),
    navigator: Navigator<T> = rememberMultiStackNav3Navigator(
        state = state,
        onBack = LocalParentRouter.current?.let { it::pop } ?: platformOnBack(),
    ),
    content: @Composable (
        state: BottomNavigationState,
        onBack: () -> Unit,
        router: Router<T>,
    ) -> Unit,
) {

    DisposableEffect(router, navigator) {
        router.commandQueue.setNavigator(navigator)
        onDispose { router.commandQueue.removeNavigator() }
    }

    val onBack: () -> Unit = remember(router) { { router.pop() } }

    CompositionLocalProvider(LocalParentRouter provides router) {
        content(state, onBack, router)
    }
}
