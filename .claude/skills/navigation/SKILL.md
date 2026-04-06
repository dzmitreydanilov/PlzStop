---
name: navigation
description: Navigation patterns — route definition, argument passing, deep links, bottom nav tabs, router API. Use when adding screens, changing navigation flows, or working with deep links.
---

# Navigation

Uses Navigation 3 with a custom `Router<T>` and `CommandQueue` pattern.

## Defining routes

Routes are `@Serializable data class` or `data object` implementing `NavKey`:

```kotlin
// No arguments
@Serializable
data object OnboardingRoute : NavKey

// Optional arguments
@Serializable
data class CreateExpenseRoute(val categoryId: Long? = null) : NavKey

// Required arguments
@Serializable
data class EditExpenseRoute(val expenseId: Long) : NavKey
```

All arguments are constructor parameters — type-safe via kotlinx.serialization.

## Registering routes

Add new routes in `RegisteredRoutes.kt`:

```kotlin
private fun PolymorphicModuleBuilder<NavKey>.registerGlobalRoutes() {
    subclass(OnboardingRoute::class)
    subclass(CreateExpenseRoute::class)
    // add here
}
```

## Bottom navigation tabs

Tabs are a sealed interface:

```kotlin
@Stable
sealed interface MainBottomTabs : NavKey {
    @Serializable data object Home : MainBottomTabs
    @Serializable data object Analytics : MainBottomTabs
    @Serializable data object Settings : MainBottomTabs
}
```

Each tab has an entry provider function in its own `*TabHost.kt`:

```kotlin
fun EntryProviderScope<NavKey>.homeTabEntries(
    onNavigateToAddExpense: (categoryId: Long) -> Unit,
    // ...
)
```

Tab UI configuration lives in `BottomNavBarConfigs.kt` via `TopLevelDestination` sealed interface (icons + labels).

## Router API

```kotlin
router.push(screen)           // Push onto stack
router.replaceCurrent(screen) // Replace top
router.replaceStack(screen)   // Replace entire stack
router.pop()                  // Go back
router.popTo(screen)          // Pop to specific screen
router.clearStack()           // Clear all except root
```

## Deep links

1. `DeepLinkResolver.resolve(data)` maps URI path segments to `DeepLinkResult`
2. `DeepLinkResult` variants: `GlobalRoute(backstack)` or `TabRoute(tab, nestedRoute?)`
3. `DeepLinkHandler` emits results via `SharedFlow`, consumed in `RootContent`

## Tab switching with nested navigation

Use `IBottomNavIntentHolder` (available via `LocalBottomNavIntentHolder`):

```kotlin
BottomNavIntent(
    targetTab = MainBottomTabs.Home,
    nestedRoute = CreateExpenseRoute(categoryId = 5)
)
```

The framework handles the tab switch first, then pushes the nested route.
