package com.dog.care.navigation

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.dog.care.features.engagement.data.ITabClickHandler
import com.please.stop.app.navigation.animation.crossfadePredictivePopSpec
import com.please.stop.app.navigation.animation.crossfadeTransitionSpec
import com.please.stop.app.navigation.bottomnavbar.IBottomNavIntentHolder
import com.please.stop.app.navigation.bottomnavbar.LocalBottomNavIntentHolder
import com.please.stop.app.navigation.bottomnavbar.bottomNavBarRoutes
import com.please.stop.app.navigation.bottomnavbar.bottomNavigationItems
import com.please.stop.app.navigation.nav3.Router
import com.please.stop.app.navigation.nav3.navigators.bottom.BottomNavigationNavHost
import com.please.stop.app.navigation.nav3.navigators.bottom.BottomNavigationState
import com.please.stop.app.navigation.nav3.navigators.bottom.rememberBottomNavigationState
import com.please.stop.app.navigation.nav3.navigators.bottom.toEntries
import com.please.stop.app.navigation.routes.AuthRoute
import com.dog.care.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.routes.RemindersRoutes
import com.dog.care.navigation.tabs.ArticlesRoutes
import com.dog.care.navigation.tabs.articlesTabEntries
import com.dog.care.navigation.tabs.assolChatEntries
import com.dog.care.navigation.tabs.calendarTabEntries
import com.please.stop.app.navigation.tabs.homeTabEntries
import com.dog.care.navigation.ui.getNavigationSuiteItemColors
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject

/**
 * Navigation host for the application's main bottom tabs.
 *
 * This composable wires up:
 * - A Navigation Suite scaffold (Material navigation rail/bar on different window sizes)
 * - The bottom tab navigator state (rememberBottomNavigationState)
 * - The entries for each tab (Home, Reminders, Articles, Assistant)
 * - Animated transitions between destinations
 *
 * It exposes a small set of callbacks to let the feature screens request navigation
 * to global routes that live outside of the bottom tabs graph (e.g. Auth, Reminders details).
 *
 * @param initialTab Which tab should be initially selected when this host is shown.
 *                   Allows direct navigation to specific tabs from global routes.
 * @param modifier Modifier for the root container
 * @param onNavigateSignIn Callback to navigate to authentication screens
 * @param onNavigateSymptoms Callback to navigate to symptoms screen
 * @param onNavigateProfile Callback to navigate to user profile
 * @param onNavigateReminders Callback to navigate to reminders screens
 *
 * Example usage:
 *
 * val onNavigateReminders: (RemindersRoutes) -> Unit = router::push
 * BottomTabsNavNavigationHost(
 *     initialTab = MainBottomTabs.AiAssistant,
 *     onNavigateSignIn = router::replaceCurrent,
 *     onNavigateSymptoms = { router.push(SymptomsRoute) },
 *     onNavigateProfile = { router.push(UserRoutes.Profile) },
 *     onNavigateReminders = onNavigateReminders,
 * )
 *
 * Notes:
 * - State preservation is enabled via SavedStateConfiguration and kotlinx.serialization, where
 *   we register polymorphic serializers for NavKey to support process death / recreation.
 * - Transitions are centralized in navigation.animation.* to keep visuals consistent.
 */

val AppNavConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            registerMainBottomNavigation()
        }
    }
}

private fun PolymorphicModuleBuilder<NavKey>.registerMainBottomNavigation() {
    subclass(MainBottomTabs.Home::class)
    subclass(MainBottomTabs.AiAssistant::class)
    subclass(MainBottomTabs.Calendar::class)
    subclass(MainBottomTabs.Articles::class)

    subclass(ArticlesRoutes.ArticleDetails::class)
}

@Composable
fun BottomTabsNavNavigationHost(
    modifier: Modifier = Modifier,
    initialTab: MainBottomTabs = MainBottomTabs.Home,
    onNavigateSignIn: (AuthRoute) -> Unit,
    onNavigateSymptoms: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateReminders: (RemindersRoutes) -> Unit,
    onNavigatePaywall: () -> Unit
) {
    val tabClickHandler = koinInject<ITabClickHandler>()
    val bottomNavIntentHolder = LocalBottomNavIntentHolder.current
    val coroutineScope = rememberCoroutineScope()

    val state = rememberBottomNavigationState(
        startRoute = initialTab,
        topLevelRoutes = bottomNavBarRoutes,
        configuration = AppNavConfiguration
    )

    ObserveBottomNavIntentsAndSwitchTabs(
        intentHolder = bottomNavIntentHolder,
        currentState = state
    )

    val handleTabClick = { router: Router<NavKey>, route: NavKey ->
        if (route is MainBottomTabs) {
            coroutineScope.launch { tabClickHandler.onTabClicked(route) }
        }
        router.replaceCurrent(route)
    }

    val navigationSuiteItemColors = getNavigationSuiteItemColors()

    BottomNavigationNavHost(state = state) { hostState, onBack, router ->
        PushPendingNestedRouteAfterTabSwitch(
            intentHolder = bottomNavIntentHolder,
            currentTab = hostState.topLevelRoute,
            router = router
        )

        NavigationSuiteScaffold(
            modifier = modifier,
            navigationSuiteItems = bottomNavigationItems(
                hostState = hostState,
                navigationSuiteItemColors = navigationSuiteItemColors,
                onTabClick = { route -> handleTabClick(router, route) }
            )
        ) {
            val entries = hostState.toEntries(
                entryProvider = entryProvider {
                    bottomNavigationEntries(
                        router = router,
                        onNavigateSignIn = onNavigateSignIn,
                        onNavigateProfile = onNavigateProfile,
                        onNavigateSymptoms = onNavigateSymptoms,
                        onNavigateReminder = onNavigateReminders,
                        onNavigatePaywall = onNavigatePaywall,
                        onNavigateArticles = { handleTabClick(router, MainBottomTabs.Articles) }
                    )
                }
            )
            NavDisplay(
                entries = entries,
                onBack = onBack,
                transitionSpec = crossfadeTransitionSpec(),
                popTransitionSpec = crossfadeTransitionSpec(),
                predictivePopTransitionSpec = crossfadePredictivePopSpec(),
            )
        }
    }
}

/**
 * Observes navigation intents from global routes and switches to the target bottom tab.
 *
 * **Purpose:**
 * Bridges navigation from global routes (e.g., Auth, Onboarding) to specific bottom tabs.
 * When a global route wants to navigate to a bottom tab, it sets an intent via the
 * [IBottomNavIntentHolder]. This composable observes that intent and switches the active tab.
 *
 * **Why it's needed:**
 * Navigation 3's root router and bottom navigation routers operate in separate scopes.
 * Direct navigation from global scope to a specific tab isn't possible without a bridge.
 * This intent holder pattern allows cross-scope communication while preserving per-tab backstacks.
 *
 * **Runs in:** Outer composition scope (has access to [BottomNavigationState])
 *
 * @param intentHolder The shared intent holder for cross-scope navigation communication
 * @param currentState The bottom navigation state that manages which tab is active
 */
@Composable
private fun ObserveBottomNavIntentsAndSwitchTabs(
    intentHolder: IBottomNavIntentHolder?,
    currentState: BottomNavigationState
) {
    LaunchedEffect(intentHolder) {
        intentHolder?.intent?.collect { intent ->
            intent?.let {
                if (it.targetTab != currentState.topLevelRoute) {
                    currentState.topLevelRoute = it.targetTab
                }
                intentHolder.clearIntent()
            }
        }
    }
}

/**
 * Pushes a pending nested route to the tab's router after a tab switch completes.
 *
 * **Purpose:**
 * When navigating from a global route directly to a nested route within a bottom tab
 * (e.g., navigating to Chat screen within the AiAssistant tab), we need two steps:
 * 1. Switch to the target tab (handled by [ObserveBottomNavIntentsAndSwitchTabs])
 * 2. Push the nested route within that tab's backstack (handled by this function)
 *
 * **Why it's separate from tab switching:**
 * The tab's router is only available inside the [BottomNavigationNavHost] scope, after
 * the tab has been switched. We can't push to the nested router from the outer scope.
 *
 * **Why it triggers on every tab change:**
 * This effect runs whenever the user switches tabs (manually or via intent). It checks
 * if there's a pending nested route to push. If not (normal tab switch), it's a no-op.
 * If yes (intent-based navigation), it pushes the route and consumes the intent.
 *
 * **Runs in:** Inner composition scope (has access to tab's [Router])
 *
 * @param intentHolder The shared intent holder containing pending nested routes
 * @param currentTab The currently active bottom tab (triggers re-check on tab switch)
 * @param router The tab-specific router for pushing nested routes
 */
@Composable
private fun PushPendingNestedRouteAfterTabSwitch(
    intentHolder: IBottomNavIntentHolder?,
    currentTab: NavKey,
    router: Router<NavKey>
) {
    LaunchedEffect(currentTab) {
        intentHolder?.consumeNestedRoute()?.let { nestedRoute ->
            router.push(nestedRoute)
        }
    }
}

/**
 * Declares navigation3 entries for all bottom tab destinations.
 *
 * Each tab contributes its own entries via helper functions in the tabs/ package.
 * The callbacks allow pages inside tabs to request navigation to global routes
 * (e.g., open Profile screen from Home tab).
 */
private fun EntryProviderScope<NavKey>.bottomNavigationEntries(
    onNavigateProfile: () -> Unit,
    router: Router<NavKey>,
    onNavigateSignIn: (AuthRoute) -> Unit,
    onNavigateSymptoms: () -> Unit,
    onNavigateReminder: (RemindersRoutes) -> Unit,
    onNavigatePaywall: () -> Unit,
    onNavigateArticles: () -> Unit
) {
    homeTabEntries(
        onNavigateProfile = onNavigateProfile,
        router = router,
        onNavigateSignIn = onNavigateSignIn,
        onNavigateSymptoms = onNavigateSymptoms,
        onNavigateReminder = onNavigateReminder
    )
    assolChatEntries(
        onNavigateSignIn = onNavigateSignIn,
        onNavigatePaywall = onNavigatePaywall,
        onNavigateArticles = onNavigateArticles
    )
    calendarTabEntries(
        onNavigateSignIn = onNavigateSignIn,
        onNavigateReminder = onNavigateReminder,
    )
    articlesTabEntries(
        router = router,
        onNavigateSignIn = onNavigateSignIn
    )
}
