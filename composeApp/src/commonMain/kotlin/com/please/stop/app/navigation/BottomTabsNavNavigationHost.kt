package com.please.stop.app.navigation

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
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
import com.please.stop.app.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.tabs.analyticsTabEntries
import com.please.stop.app.navigation.tabs.homeTabEntries
import com.please.stop.app.navigation.tabs.settingsTabEntries
import com.please.stop.app.navigation.ui.getNavigationSuiteItemColors
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val AppNavConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            registerMainBottomNavigation()
        }
    }
}

private fun PolymorphicModuleBuilder<NavKey>.registerMainBottomNavigation() {
    subclass(MainBottomTabs.Home::class)
    subclass(MainBottomTabs.Analytics::class)
    subclass(MainBottomTabs.Settings::class)
}

@Composable
fun BottomTabsNavNavigationHost(
    modifier: Modifier = Modifier,
    initialTab: MainBottomTabs = MainBottomTabs.Home,
    onNavigateToAddExpense: (preselectedCategoryId: Long) -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditExpense: (expenseId: Long) -> Unit = {},
    onNavigateToMonthlyExpenses: () -> Unit,
    onNavigateToCategories: () -> Unit,
) {
    val bottomNavIntentHolder = LocalBottomNavIntentHolder.current

    val state = rememberBottomNavigationState(
        startRoute = initialTab,
        topLevelRoutes = bottomNavBarRoutes,
        configuration = AppNavConfiguration,
    )

    ObserveBottomNavIntentsAndSwitchTabs(
        intentHolder = bottomNavIntentHolder,
        currentState = state,
    )

    val navigationSuiteItemColors = getNavigationSuiteItemColors()

    BottomNavigationNavHost(state = state) { hostState, onBack, router ->
        PushPendingNestedRouteAfterTabSwitch(
            intentHolder = bottomNavIntentHolder,
            currentTab = hostState.topLevelRoute,
            router = router,
        )

        NavigationSuiteScaffold(
            modifier = modifier,
            navigationSuiteItems = bottomNavigationItems(
                hostState = hostState,
                navigationSuiteItemColors = navigationSuiteItemColors,
                onTabClick = { route -> router.replaceCurrent(route) },
            ),
        ) {
            val entries = hostState.toEntries(
                entryProvider = entryProvider {
                    bottomNavigationEntries(
                        onNavigateToAddExpense = onNavigateToAddExpense,
                        onNavigateToCreateExpense = onNavigateToCreateExpense,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToEditExpense = onNavigateToEditExpense,
                        onNavigateToMonthlyExpenses = onNavigateToMonthlyExpenses,
                        onNavigateToCategories = onNavigateToCategories,
                    )
                },
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

@Composable
private fun ObserveBottomNavIntentsAndSwitchTabs(
    intentHolder: IBottomNavIntentHolder?,
    currentState: BottomNavigationState,
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

@Composable
private fun PushPendingNestedRouteAfterTabSwitch(
    intentHolder: IBottomNavIntentHolder?,
    currentTab: NavKey,
    router: Router<NavKey>,
) {
    LaunchedEffect(currentTab) {
        intentHolder?.consumeNestedRoute()?.let { nestedRoute ->
            router.push(nestedRoute)
        }
    }
}

private fun EntryProviderScope<NavKey>.bottomNavigationEntries(
    onNavigateToAddExpense: (categoryId: Long) -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditExpense: (expenseId: Long) -> Unit,
    onNavigateToMonthlyExpenses: () -> Unit,
    onNavigateToCategories: () -> Unit,
) {
    homeTabEntries(
        onNavigateToAddExpense = onNavigateToAddExpense,
        onNavigateToCreateExpense = onNavigateToCreateExpense,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToMonthlyExpenses = onNavigateToMonthlyExpenses,
    )
    analyticsTabEntries(onNavigateToEditExpense = onNavigateToEditExpense)
    settingsTabEntries(onNavigateToCategories = onNavigateToCategories)
}
