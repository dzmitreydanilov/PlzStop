package com.dog.care.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.dog.care.features.engagement.presentation.GlobalEngagementCoordinator
import com.dog.care.features.engagement.presentation.NavigateToChatOnEngagement
import com.dog.care.features.engagement.presentation.NavigateToSymptomsOnEngagement
import com.dog.care.features.login.choseoption.presentation.ChooseSignInOptionRoute
import com.dog.care.features.login.greeting.presentation.GreetingsScreenRoute
import com.dog.care.features.login.mail.presentation.InputCodeScreenRoute
import com.dog.care.features.onboarding.presentation.OnboardingScreenRoute
import com.dog.care.features.profile.legal.LegalDocumentsScreenRoute
import com.dog.care.features.profile.petprofileinfo.presentation.PetProfileInfoScreenRoute
import com.dog.care.features.profile.profile.presentation.ProfileScreenRoute
import com.dog.care.features.profile.settings.presentation.SettingsScreenRoute
import com.dog.care.features.subscription.details.presentation.SubscriptionDetailsScreenRoute
import com.dog.care.features.subscription.paywall.SubscriptionPaywallRoute
import com.dog.care.features.symptoms.presentation.AddSymptomsRoute
import com.please.stop.app.navigation.animation.LocalRootAnimatedContentScope
import com.please.stop.app.navigation.animation.LocalSharedTransitionScope
import com.dog.care.navigation.animation.ProfileTransitionConfig
import com.please.stop.app.navigation.animation.predictivePopTransitionSpec
import com.dog.care.navigation.animation.profileTransitionSpecs
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
import com.please.stop.app.navigation.nav3.strategies.BottomSheetSceneStrategy
import com.please.stop.app.navigation.routes.AuthRoute
import com.please.stop.app.navigation.routes.LegalDocsRoute
import com.dog.care.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.routes.NoNetworkRoute
import com.please.stop.app.navigation.routes.OnboardingRoute
import com.please.stop.app.navigation.routes.PaywallRoute
import com.please.stop.app.navigation.routes.QuestionnaireRoute
import com.please.stop.app.navigation.routes.SymptomsRoute
import com.please.stop.app.navigation.routes.UserRoutes
import com.dog.care.navigation.routes.registerGlobalRotes
import com.dog.care.navigation.tabs.remindersEntries
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.collections.listOf

/**
 * Root navigation host that wires the entire app graph.
 *
 * Responsibilities:
 * - Creates a NavBackStack with saved-state support and registers polymorphic serializers
 *   for all global routes used in the app.
 * - Bridges our Router with Navigation 3 via Nav3Host, including back handling and
 *   entry decorators for ViewModelStore and SaveableStateHolder.
 * - Declares all top-level entries: bottom tabs host, reminders, user/profile, auth,
 *   onboarding, questionnaire, symptoms, and paywall flows.
 * - Resolves deep link URIs (cold start and runtime) into navigation targets.
 *
 * @param initialRoute The starting route determined by auth state / network availability.
 * @param coordinator Global engagement coordinator for in-app prompts.
 * @param deepLinkUri Optional deep link URI from the launching intent (cold start).
 * @param deepLinkHandler Handler for runtime deep link events (warm start / onNewIntent).
 */
@Composable
fun RootContent(
    initialRoute: NavKey,
    coordinator: GlobalEngagementCoordinator,
    deepLinkUri: String? = null,
    deepLinkHandler: DeepLinkHandler? = null,
) {
    DisposableEffect(coordinator) {
        coordinator.start()
        onDispose {
            coordinator.stop()
        }
    }

    val bottomNavIntentHolder = LocalBottomNavIntentHolder.current
    val deepLinkResolver = remember { DeepLinkResolver() }

    // Resolve once, then consume to prevent re-triggering on lifecycle restart
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
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        bottomNavigationNavHost(router)
                        remindersEntries(router, onBack)
                        userEntries(router)
                        authEntries(router)
                        onboardingEntries(router)
                        questionnaireEntries(router)
                        symptomsEntries(router)
                        paywallEntries(router)
                        noNetworkInitialRoute(router)
                    },
                    sceneStrategies = listOf(
                        BottomSheetSceneStrategy(),
                        SinglePaneSceneStrategy()
                    )
                )
            }
        }

        // Handle cold-start tab deep links once, then consume
        LaunchedEffect(coldStartResult) {
            val result = coldStartResult
            if (result is DeepLinkResult.TabRoute) {
                bottomNavIntentHolder?.setIntent(
                    BottomNavIntent(
                        targetTab = result.tab,
                        nestedRoute = result.nestedRoute
                    )
                )
                coldStartResult = null
            }
        }

        // Handle runtime deep links (onNewIntent, notification taps while app is running)
        if (deepLinkHandler != null) {
            CollectNavigationFlow(
                flow = deepLinkHandler.deepLinkEvents,
                key1 = deepLinkHandler
            ) { result ->
                applyDeepLinkResult(result, router, bottomNavIntentHolder)
                deepLinkHandler.consumeEvent()
            }
        }

        CollectNavigationFlow(
            flow = coordinator.navigation,
            key1 = coordinator
        ) { navigation ->
            when (navigation) {
                is NavigateToChatOnEngagement -> {
                    bottomNavIntentHolder?.setIntent(
                        BottomNavIntent(
                            targetTab = MainBottomTabs.AiAssistant,
                            nestedRoute = null
                        )
                    )
                }

                is NavigateToSymptomsOnEngagement -> {
                    router.push(SymptomsRoute)
                }
            }
        }
    }
}

@Suppress("SpreadOperator")
private fun applyDeepLinkResult(
    result: DeepLinkResult,
    router: Router<NavKey>,
    bottomNavIntentHolder: IBottomNavIntentHolder?
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
                    nestedRoute = result.nestedRoute
                )
            )
        }
    }
}

private fun EntryProviderScope<NavKey>.noNetworkInitialRoute(router: Router<NavKey>) {
    entry<NoNetworkRoute> { route ->
        NoNetworkRoute(onNavigateBack = { router.replaceCurrent(route.onNetworkAppearRoute) })
    }
}

private fun EntryProviderScope<NavKey>.paywallEntries(router: Router<NavKey>) {
    entry<PaywallRoute> {
        SubscriptionPaywallRoute(onNavigateBack = router::pop)
    }
}

private fun EntryProviderScope<NavKey>.bottomNavigationNavHost(router: Router<NavKey>) {
    entry<MainBottomTabs.Home> {
        CompositionLocalProvider(
            LocalRootAnimatedContentScope provides LocalNavAnimatedContentScope.current
        ) {
            BottomTabsNavNavigationHost(
                onNavigateReminders = router::push,
                onNavigateSignIn = router::replaceCurrent,
                onNavigateProfile = {
                    ProfileTransitionConfig.useSharedTransition = true
                    router.push(UserRoutes.Profile)
                },
                onNavigateSymptoms = { router.push(SymptomsRoute) },
                onNavigatePaywall = { router.push(UserRoutes.SubscriptionDetails) }
            )
        }
    }
}

private fun EntryProviderScope<NavKey>.onboardingEntries(router: Router<NavKey>) {
    entry<OnboardingRoute> {
        OnboardingScreenRoute(
            onFinishOnboarding = { router.replaceStack(AuthRoute.ChooseSignInOption) },
            onNavigateHome = { router.replaceStack(MainBottomTabs.Home) }
        )
    }
}

private fun EntryProviderScope<NavKey>.symptomsEntries(router: Router<NavKey>) {
    entry<SymptomsRoute> {
        AddSymptomsRoute(onNavigateBack = router::pop)
    }
}

private fun EntryProviderScope<NavKey>.questionnaireEntries(router: Router<NavKey>) {
    entry<QuestionnaireRoute> {
        QuestionnaireRoute(
            onNavigateHome = { router.replaceStack(MainBottomTabs.Home) }
        )
    }
}

private fun EntryProviderScope<NavKey>.authEntries(router: Router<NavKey>) {
    entry<AuthRoute.ChooseSignInOption> {
        ChooseSignInOptionRoute(
            onNavigateHome = { router.replaceStack(MainBottomTabs.Home) },
            onNavigateQuestionnaire = { router.replaceStack(QuestionnaireRoute) },
            onNavigateAuthRoute = router::push
        )
    }

    entry<AuthRoute.PreSignIn> {
        GreetingsScreenRoute(
            onSignInClick = { router.replaceCurrent(AuthRoute.ChooseSignInOption) },
            onSkipClick = { router.replaceStack(MainBottomTabs.Home) },
            onNavigateOnboarding = { router.replaceStack(OnboardingRoute) }
        )
    }

    entry<AuthRoute.ProvideEmailCode> {
        InputCodeScreenRoute(
            onNavigateHome = { router.replaceStack(MainBottomTabs.Home) },
            onNavigateBack = router::pop,
            onNavigateQuestionnaire = { router.replaceStack(QuestionnaireRoute) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun EntryProviderScope<NavKey>.userEntries(router: Router<NavKey>) {
    entry<UserRoutes.Profile>(metadata = profileTransitionSpecs) {
        ProfileScreenRoute(
            onNavigateBack = { router.pop() },
            onNavigateToSettings = { router.push(UserRoutes.Settings) },
            onNavigateToDogInfo = { router.push(UserRoutes.DogInfo) },
            onNavigateToTerms = { router.push(LegalDocsRoute) },
            onNavigateSubscription = { router.push(UserRoutes.SubscriptionDetails) },
            onNavigateHome = { router.replaceStack(MainBottomTabs.Home) }
        )
    }

    entry<UserRoutes.SubscriptionDetails> {
        SubscriptionDetailsScreenRoute(
            onNavigateBack = router::pop,
            onPurchaseProClick = { router.push(PaywallRoute) }
        )
    }

    entry<LegalDocsRoute> {
        LegalDocumentsScreenRoute(onNavigateBack = router::pop)
    }

    entry<UserRoutes.Settings> {
        SettingsScreenRoute(
            onNavigateBack = router::pop,
            onNavigateHome = { router.replaceStack(MainBottomTabs.Home) }
        )
    }
    entry<UserRoutes.DogInfo> {
        PetProfileInfoScreenRoute(onNavigateBack = router::pop)
    }
}
