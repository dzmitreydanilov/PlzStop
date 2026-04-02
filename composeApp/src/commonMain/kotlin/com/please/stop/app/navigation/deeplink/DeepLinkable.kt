package com.please.stop.app.navigation.deeplink

import androidx.navigation3.runtime.NavKey

/**
 * Marker interface for routes that can be reached via deep links.
 *
 * Each deep-linkable route declares its [parentRoute] in the navigation hierarchy.
 * [buildSyntheticBackstack] walks up the parent chain to construct the full backstack
 * automatically — no hardcoded mapping needed.
 *
 * @see buildSyntheticBackstack
 */
interface DeepLinkable {

    /**
     * The parent route in the navigation hierarchy, or `null` if this route is a root.
     */
    val parentRoute: NavKey?
}

/**
 * Walks up the [DeepLinkable.parentRoute] chain to build a root-first backstack.
 *
 * Example: `SubscriptionDetails(parent=Profile) → Profile(parent=Home) → Home(parent=null)`
 * produces `[Home, Profile, SubscriptionDetails]`.
 */
fun buildSyntheticBackstack(target: NavKey): List<NavKey> {
    val stack = mutableListOf(target)
    var current = target
    while (current is DeepLinkable) {
        val parent = current.parentRoute ?: break
        stack.add(0, parent)
        current = parent
    }
    return stack
}
