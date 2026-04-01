Navigation package overview

This folder contains the shared navigation setup for the DogCare app. It integrates Jetpack Navigation 3 with a thin Router/Command layer to make navigation testable and platform-agnostic within the KMP shared module.

Key concepts

- Router: High-level API you call from presentation code (push, pop, replaceCurrent, replaceStack, dropStack). See nav3/Router.kt.
- Command: Immutable operations representing navigation actions (Push, Pop, etc.). See nav3/Command.kt.
- Navigator: Applies commands to a concrete back stack. The default implementation bridges to Navigation 3. See nav3/Nav3Navigator.kt.
- Nav3Host: Glue composable that connects the Router with Navigation 3 back stack and handles lifecycle, back interception, and entry decorators. See nav3/Navigation.kt.

Where things live

- RootContent.kt: Declares the app graph and hosts NavDisplay for global routes.
- BottomTabsNavNavigationHost.kt: Hosts the bottom tabs sub-graph with NavigationSuiteScaffold and transitions.
- navigation/routes: Sealed routes for the global graph and registration for SavedState serializers.
- navigation/tabs: Entries for each bottom tab.

Quick start: Show the app graph

// Compose entry point (platform-specific)
RootContent(initialRoute = MainBottomTabs.Home)

Navigating from a screen

// Inside a @Composable with access to Router (e.g., provided by Nav3Host)
val onNavigateProfile = { router.push(UserRoutes.Profile) }

// Or replace current (e.g., after successful auth)
router.replaceCurrent(MainBottomTabs.Home)

// Or replace the whole stack (switching flows)
router.replaceStack(MainBottomTabs.Home)

Collecting navigation events from a StateHolder

// In a screen composable
CollectNavigationFlow(flow = stateHolder.navigation) { event ->
    when (event) {
        is UserRoutes -> router.push(event)
        is AuthRoute -> router.replaceCurrent(event)
    }
}

Add a new screen

1) Define a route (sealed NavKey) in navigation/routes:

sealed interface UserRoutes : NavKey {
    data object Profile : UserRoutes
    data object Settings : UserRoutes
    // Add new route:
    data object DogInfo : UserRoutes
}

2) Register it for saved state serialization in RegisteredRoutes.kt:

private fun PolymorphicModuleBuilder<NavKey>.registerUserRoutes() {
    subclass(UserRoutes.Profile::class)
    subclass(UserRoutes.Settings::class)
    // New:
    subclass(UserRoutes.DogInfo::class)
}

3) Provide an entry (UI binding) in RootContent.kt:

entry<UserRoutes.DogInfo> {
    PetProfileInfoScreenRoute(onNavigateBack = router::pop)
}

Bottom tabs overview

BottomTabsNavNavigationHost sets up state with rememberBottomNavigationState and renders NavigationSuiteScaffold. Each tab contributes entries from navigation/tabs. Switch between top-level destinations by calling router.replaceCurrent(tabRoute). Back follows an "exit-through-home" rule: popping at the root of a non-home tab switches to the Home tab; popping at the home root delegates to the platform back.

Transitions and state

Animations for enter/exit/pop are provided by navigation/animation. Saved state is handled by SavedStateConfiguration with polymorphic serializers for NavKey, enabling process death recovery.

Tips

- Keep all navigation logic in the shared module; use expect/actual only for platform-specific bits when necessary.
- Use immutable routes (objects or data classes) to ensure stable equality in back stack operations.
- For MVI, expose navigation as a Flow of one-off events and collect it with CollectNavigationFlow.
