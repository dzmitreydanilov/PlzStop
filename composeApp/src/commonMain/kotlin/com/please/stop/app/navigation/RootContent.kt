package com.please.stop.app.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.please.stop.app.core.UrlOpener
import com.please.stop.app.features.auth.presentation.ui.AuthScreen
import com.please.stop.app.features.categories.presentation.archived.ArchivedCategoriesScreen
import com.please.stop.app.features.categories.presentation.ui.CategoriesScreen
import com.please.stop.app.features.expenses.presentation.ui.CreateExpenseScreen
import com.please.stop.app.features.expenses.presentation.ui.EditExpenseScreen
import com.please.stop.app.features.expenses.receiptitems.presentation.ui.ReceiptItemsScreen
import com.please.stop.app.features.export.presentation.ui.ExportScreenRoute
import com.please.stop.app.features.onboarding.presentation.ui.OnboardingScreen
import com.please.stop.app.features.subscriptions.presentation.promotion.NavigateToSubscriptionsList
import com.please.stop.app.features.subscriptions.presentation.promotion.SubscriptionPromoBottomSheet
import com.please.stop.app.features.subscriptions.presentation.promotion.SubscriptionPromoCoordinator
import com.please.stop.app.features.subscriptions.presentation.ui.SubscriptionsListScreen
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
import com.please.stop.app.navigation.routes.ArchivedCategoriesRoute
import com.please.stop.app.navigation.routes.AuthRoute
import com.please.stop.app.navigation.routes.CategoriesRoute
import com.please.stop.app.navigation.routes.CreateExpenseRoute
import com.please.stop.app.navigation.routes.EditExpenseRoute
import com.please.stop.app.navigation.routes.ExportRoute
import com.please.stop.app.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.routes.OnboardingRoute
import com.please.stop.app.navigation.routes.ReceiptItemsRoute
import com.please.stop.app.navigation.routes.SubscriptionsListRoute
import com.please.stop.app.navigation.routes.registerGlobalRotes
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject

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
    deepLinkHandler: DeepLinkHandler,
) {
    val bottomNavIntentHolder = LocalBottomNavIntentHolder.current
    val urlOpener = koinInject<UrlOpener>()
    val promoCoordinator = koinInject<SubscriptionPromoCoordinator>()
    val promoState by promoCoordinator.state.collectAsStateWithLifecycle()

    DisposableEffect(promoCoordinator) {
        promoCoordinator.start()
        onDispose { promoCoordinator.stop() }
    }

    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    registerGlobalRotes()
                }
            }
        },
        elements = arrayOf(initialRoute)
    )

    @OptIn(ExperimentalSharedTransitionApi::class)
    Nav3Host(backStack = backStack) { backStack, onBack, router ->
        Box(modifier = Modifier.fillMaxSize()) {
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

            SubscriptionPromoBottomSheet(
                state = promoState,
                onCtaClick = promoCoordinator::onCtaClicked,
                onDismiss = promoCoordinator::onDismissed,
            )
        }

        CollectNavigationFlow(
            flow = promoCoordinator.navigation,
            key1 = promoCoordinator,
        ) { navigation ->
            if (navigation is NavigateToSubscriptionsList) {
                router.push(SubscriptionsListRoute)
            }
        }

        // Handle runtime deep links (onNewIntent, notification taps while app is running)
        if (deepLinkHandler != null) {
            CollectNavigationFlow(
                flow = deepLinkHandler.deepLinkEvents,
                key1 = deepLinkHandler
            ) { result ->
                applyDeepLinkResult(result, router, bottomNavIntentHolder, urlOpener)
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
    urlOpener: UrlOpener? = null,
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

        is DeepLinkResult.OpenExternalUrl -> {
            urlOpener?.open(result.url)
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
            onNavigateToEditExpense = { expenseId -> router.push(EditExpenseRoute(expenseId)) },
            onNavigateToCategories = { router.push(CategoriesRoute) },
            onNavigateToSubscriptions = { router.push(SubscriptionsListRoute) },
            onNavigateToExportData = { router.push(ExportRoute) }
        )
    }

    entry<CreateExpenseRoute> { route ->
        CreateExpenseScreen(
            categoryId = route.categoryId,
            onGoBack = { router.pop() },
            onOpenReceiptItems = { router.replaceCurrent(ReceiptItemsRoute) },
        )
    }

    entry<EditExpenseRoute> { route ->
        EditExpenseScreen(
            expenseId = route.expenseId,
            onGoBack = { router.pop() },
        )
    }

    entry<ReceiptItemsRoute> {
        ReceiptItemsScreen(
            onGoBack = { router.pop() },
            onSaved = { router.pop() },
        )
    }

    entry<CategoriesRoute> {
        CategoriesScreen(
            onGoBack = { router.pop() },
            onOpenArchive = { router.push(ArchivedCategoriesRoute) },
        )
    }

    entry<ArchivedCategoriesRoute> {
        ArchivedCategoriesScreen(onGoBack = { router.pop() })
    }

    entry<SubscriptionsListRoute> {
        SubscriptionsListScreen(onGoBack = { router.pop() })
    }

    entry<ExportRoute> {
        ExportScreenRoute(onNavigateBack = { router.pop() })
    }
}

private fun EntryProviderScope<NavKey>.onboardingEntries(router: Router<NavKey>) {
    entry<AuthRoute> {
        AuthScreen(
            onNavigateToHome = { router.replaceStack(MainBottomTabs.Home) },
        )
    }

    entry<OnboardingRoute> {
        OnboardingScreen(
            onNavigateToHome = { router.replaceStack(MainBottomTabs.Home) },
        )
    }
}
