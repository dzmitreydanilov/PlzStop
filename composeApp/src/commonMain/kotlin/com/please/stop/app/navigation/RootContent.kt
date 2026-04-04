package com.please.stop.app.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.please.stop.app.navigation.animation.LocalSharedTransitionScope
import com.please.stop.app.navigation.animation.predictivePopTransitionSpec
import com.please.stop.app.navigation.animation.slideInTransitionSpec
import com.please.stop.app.navigation.animation.slideOutTransitionSpec
import com.please.stop.app.navigation.bottomnavbar.BottomNavIntent
import com.please.stop.app.navigation.bottomnavbar.IBottomNavIntentHolder
import com.please.stop.app.navigation.bottomnavbar.LocalBottomNavIntentHolder
import com.please.stop.app.navigation.deeplink.DeepLinkHandler
import com.please.stop.app.navigation.deeplink.DeepLinkResolver
import com.please.stop.app.navigation.deeplink.DeepLinkResult
import com.please.stop.app.navigation.deeplink.parseDeepLinkUri
import com.please.stop.app.navigation.nav3.Nav3Host
import com.please.stop.app.navigation.nav3.Router
import com.please.stop.app.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.routes.OnboardingRoute
import com.please.stop.app.navigation.routes.CreateExpenseRoute
import com.please.stop.app.navigation.routes.EditExpenseRoute
import com.please.stop.app.navigation.routes.registerGlobalRotes
import com.please.stop.app.features.expenses.presentation.ui.CreateExpenseScreen
import com.please.stop.app.features.expenses.presentation.ui.EditExpenseScreen
import com.please.stop.app.features.onboarding.presentation.ui.OnboardingScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Root navigation host that wires the entire app graph.
 *
 * @param initialRoute The starting route determined by onboarding state.
 * @param deepLinkUri Optional deep link URI from the launching intent (cold start).
 * @param deepLinkHandler Handler for runtime deep link events (warm start / onNewIntent).
 */
@Composable
fun RootContent(
    initialRoute: NavKey,
    deepLinkUri: String? = null,
    deepLinkHandler: DeepLinkHandler? = null,
) {
    val bottomNavIntentHolder = LocalBottomNavIntentHolder.current
    val deepLinkResolver = remember { DeepLinkResolver() }

    var coldStartResult by remember {
        mutableStateOf(
            deepLinkUri?.let { uri ->
                parseDeepLinkUri(uri)?.let { deepLinkResolver.resolve(it) }
            }
        )
    }

    val startElements: Array<NavKey> = remember(coldStartResult, initialRoute) {
        when (val result = coldStartResult) {
            is DeepLinkResult.GlobalRoute -> result.backstack.toTypedArray()
            is DeepLinkResult.TabRoute -> arrayOf(MainBottomTabs.Home)
            null -> arrayOf(initialRoute)
        }
    }

    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    registerGlobalRotes()
                }
            }
        },
        elements = startElements,
    )

    @OptIn(ExperimentalSharedTransitionApi::class)
    Nav3Host(backStack = backStack) { backStack, onBack, router ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavDisplay(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    transitionSpec = slideInTransitionSpec(),
                    popTransitionSpec = slideOutTransitionSpec(),
                    predictivePopTransitionSpec = predictivePopTransitionSpec(),
                    backStack = backStack,
                    onBack = onBack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        bottomNavigationNavHost(router)
                        onboardingEntries(router)
                    },
                    sceneStrategies = listOf(
                        SinglePaneSceneStrategy(),
                    ),
                )
            }
        }

        // Handle cold-start tab deep links once, then consume
        if (coldStartResult is DeepLinkResult.TabRoute) {
            val result = coldStartResult as DeepLinkResult.TabRoute
            bottomNavIntentHolder?.setIntent(
                BottomNavIntent(
                    targetTab = result.tab,
                    nestedRoute = result.nestedRoute,
                )
            )
            coldStartResult = null
        }

        // Handle runtime deep links (onNewIntent, notification taps while app is running)
        if (deepLinkHandler != null) {
            CollectNavigationFlow(
                flow = deepLinkHandler.deepLinkEvents,
                key1 = deepLinkHandler,
            ) { result ->
                applyDeepLinkResult(result, router, bottomNavIntentHolder)
                deepLinkHandler.consumeEvent()
            }
        }
    }
}

@Suppress("SpreadOperator")
private fun applyDeepLinkResult(
    result: DeepLinkResult,
    router: Router<NavKey>,
    bottomNavIntentHolder: IBottomNavIntentHolder?,
) {
    when (result) {
        is DeepLinkResult.GlobalRoute -> {
            router.replaceStack(*result.backstack.toTypedArray())
        }

        is DeepLinkResult.TabRoute -> {
            router.replaceStack(MainBottomTabs.Home)
            bottomNavIntentHolder?.setIntent(
                BottomNavIntent(
                    targetTab = result.tab,
                    nestedRoute = result.nestedRoute,
                )
            )
        }
    }
}

private fun EntryProviderScope<NavKey>.bottomNavigationNavHost(router: Router<NavKey>) {
    entry<MainBottomTabs.Home> {
        BottomTabsNavNavigationHost(
            onNavigateToAddExpense = { categoryId ->
                router.push(CreateExpenseRoute(categoryId = categoryId))
            },
            onNavigateToSettings = {},
            onNavigateToCreateExpense = { router.push(CreateExpenseRoute()) },
        )
    }

    entry<CreateExpenseRoute> { route ->
        CreateExpenseScreen(
            categoryId = route.categoryId,
            onGoBack = { router.pop() },
        )
    }

    entry<EditExpenseRoute> { route ->
        EditExpenseScreen(
            expenseId = route.expenseId,
            onGoBack = { router.pop() },
        )
    }
}

private fun EntryProviderScope<NavKey>.onboardingEntries(router: Router<NavKey>) {
    entry<OnboardingRoute> {
        OnboardingScreen(
            onNavigateToHome = { router.replaceStack(MainBottomTabs.Home) },
        )
    }
}
