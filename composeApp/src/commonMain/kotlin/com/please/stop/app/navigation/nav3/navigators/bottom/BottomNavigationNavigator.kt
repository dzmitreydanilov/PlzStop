package com.please.stop.app.navigation.nav3.navigators.bottom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.navigation.nav3.Command
import com.please.stop.app.navigation.nav3.DropStack
import com.please.stop.app.navigation.nav3.Navigator
import com.please.stop.app.navigation.nav3.Pop
import com.please.stop.app.navigation.nav3.PopTo
import com.please.stop.app.navigation.nav3.Push
import com.please.stop.app.navigation.nav3.ReplaceCurrent
import com.please.stop.app.navigation.nav3.ResetToRoot
import com.please.stop.app.navigation.nav3.dropStack
import com.please.stop.app.navigation.nav3.pop
import com.please.stop.app.navigation.nav3.popTo
import com.please.stop.app.navigation.nav3.push
import com.please.stop.app.navigation.nav3.replace
import com.please.stop.app.navigation.nav3.resetToRoot
import com.please.stop.app.navigation.nav3.swap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Multi-stack Navigator that executes Router commands against [BottomNavigationState].
 *
 * How it works
 * - We always operate on the CURRENT tab's back stack via a mutable snapshot (a MutableList copy).
 * - Commands are processed sequentially. This avoids exposing intermediate states to UI.
 * - When we see ReplaceCurrent with a TOP-LEVEL NavKey (a tab root), it means "switch tab".
 *   In that case we:
 *   1) Commit the pending snapshot into the OLD tab's back stack so its state is preserved.
 *   2) Change focus to the NEW tab (update state.topLevelRoute and local references).
 *   3) Recreate the working snapshot from the NEW tab's back stack and continue processing
 *      subsequent commands against this NEW snapshot.
 * - Pop follows the "exit-through-home" rule: if you're at the base of a non-start tab, we
 *   switch to the start tab instead of exiting; if already at the start tab base, we delegate to
 *   system back via onBack().
 * - After all commands are processed, the last active snapshot is committed to its back stack.
 */
@Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
class BottomNavigationNavigator(
    private val state: BottomNavigationState,
    private val onBack: () -> Unit,
) : Navigator<NavKey> {

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun applyCommands(commands: Array<out Command<NavKey>>) {
        var currentTop = state.topLevelRoute
        var currentBackStack = state.backStacks[currentTop]
            ?: error("Stack for $currentTop not found")
        var snapshot = currentBackStack.toMutableList()
        var callOnBack = false

        for (command in commands) {
            when (command) {
                is Push<NavKey> -> {
                    push(snapshot, command)
                }

                is ReplaceCurrent<NavKey> -> {
                    val target = command.screen
                    if (state.backStacks.containsKey(target)) {
                        // Tab switch: first commit current snapshot so we don't lose pending mutations.
                        currentBackStack.swap(snapshot)
                        // Update focus to the new tab and rebind working data.
                        state.topLevelRoute = target
                        currentTop = target
                        currentBackStack = state.backStacks[target]
                            ?: error("Stack for $target not found")
                        snapshot = currentBackStack.toMutableList()
                        // Note: we DO use the new snapshot afterwards by continuing the loop.
                    } else {
                        // Regular replace within the current tab
                        replace(snapshot, command)
                    }
                }

                is PopTo<NavKey> -> {
                    popTo(snapshot, command)
                }

                is Pop -> {
                    val atBase = snapshot.lastOrNull() == currentTop
                    if (atBase) {
                        if (currentTop != state.startRoute) {
                            // Switch to start tab base
                            // Commit current snapshot before switching focus
                            currentBackStack.swap(snapshot)
                            state.topLevelRoute = state.startRoute
                            currentTop = state.topLevelRoute
                            currentBackStack = state.backStacks[currentTop]
                                ?: error("Stack for $currentTop not found")
                            snapshot = currentBackStack.toMutableList()
                        } else {
                            callOnBack = true
                        }
                    } else {
                        // in-tab pop
                        pop(snapshot)
                    }
                }

                is ResetToRoot -> {
                    resetToRoot(snapshot)
                }

                is DropStack -> {
                    dropStack(snapshot)
                    callOnBack = true
                }
            }
        }

        // Commit the final working snapshot into the currently active tab's back stack
        currentBackStack.swap(snapshot)
        if (callOnBack) scheduleOnBack()
    }

    /**
     * Schedules a system back navigation call.
     *
     * The call is delayed using yield() to ensure that NavDisplay has received
     * the updated back stack before the system back navigation is triggered.
     * This prevents race conditions where the system tries to handle back
     * navigation before the UI has been updated.
     */
    private fun scheduleOnBack() {
        mainScope.launch {
            yield()
            onBack()
        }
    }
}

@Composable
/**
 * Remembers a multi-stack [Navigator] that knows how to:
 * - Execute Router commands against the currently selected tab
 * - Switch tabs when [ReplaceCurrent] targets a top-level tab key
 * - Apply "exit-through-home" semantics for [Pop]
 *
 * The returned navigator is stable for the same [state] and [onBack] pair.
 *
 * @param state The [BottomNavigationState] with per-tab back stacks
 * @param onBack Callback to invoke when the app should delegate to platform back
 */
fun <T : NavKey> rememberMultiStackNav3Navigator(
    state: BottomNavigationState,
    onBack: () -> Unit,
): Navigator<T> {

    return remember(state, onBack) {
        @Suppress("UNCHECKED_CAST")
        BottomNavigationNavigator(state, onBack) as Navigator<T>
    }
}
