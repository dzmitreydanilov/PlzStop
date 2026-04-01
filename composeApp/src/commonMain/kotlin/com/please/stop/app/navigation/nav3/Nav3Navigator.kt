package com.please.stop.app.navigation.nav3

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.core.logger.logErrorWithTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Navigator implementation that integrates with Jetpack Navigation 3.
 *
 * This class bridges the gap between the high-level Router API and the underlying
 * Navigation 3 system. It translates navigation commands into direct manipulations
 * of the Navigation 3 back stack.
 *
 * @param navBackStack The Navigation 3 back stack to manipulate
 * @param onBack Callback to trigger system back navigation when the stack is empty
 */
@Suppress("TooManyFunctions")
open class Nav3Navigator(
    private val navBackStack: NavBackStack<NavKey>,
    private val onBack: () -> Unit,
) : Navigator<NavKey> {

    /** Coroutine scope for scheduling back navigation calls */
    protected val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Applies an array of navigation commands to the back stack.
     *
     * Commands are processed sequentially against a snapshot of the current stack.
     * Once all commands are processed, the actual back stack is updated atomically.
     * This ensures consistency and prevents intermediate states from being visible.
     *
     * @param commands Array of navigation commands to apply
     */
    override fun applyCommands(
        commands: Array<out Command<NavKey>>,
    ) {
        val snapshot = navBackStack.toMutableList()
        var callOnBack = false

        for (command in commands) {
            try {
                applyCommand(
                    snapshot = snapshot,
                    command = command,
                    onBackRequested = {
                        callOnBack = true
                    },
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
                logErrorWithTag(
                    throwable = e,
                    tag = "Nav3Navigator",
                    message = "Error executing command: $command"
                )
            }
        }

        navBackStack.swap(snapshot)

        if (callOnBack) {
            scheduleOnBack()
        }
    }

    /**
     * Processes a single navigation command against the stack snapshot.
     *
     * This method can be overridden in subclasses to add custom command types
     * or modify the behavior of existing commands.
     *
     * @param snapshot Mutable copy of the navigation stack
     * @param command Command to process
     * @param onBackRequested Callback to trigger when system back navigation is needed
     */
    protected open fun applyCommand(
        snapshot: MutableList<NavKey>,
        command: Command<NavKey>,
        onBackRequested: () -> Unit,
    ) {
        when (command) {
            is Push<NavKey> -> push(
                snapshot = snapshot,
                command = command,
            )
            is ReplaceCurrent<NavKey> -> replace(
                snapshot = snapshot,
                command = command,
            )
            is PopTo<NavKey> -> popTo(
                snapshot = snapshot,
                command = command,
            )
            is Pop -> {
                if (!pop(snapshot)) onBackRequested()
            }
            is ResetToRoot -> resetToRoot(
                snapshot = snapshot,
            )
            is DropStack -> {
                dropStack(
                    snapshot = snapshot,
                )
                onBackRequested()
            }
        }
    }

    /**
     * Schedules a system back navigation call.
     *
     * The call is delayed using yield() to ensure that NavDisplay has received
     * the updated back stack before the system back navigation is triggered.
     * This prevents race conditions where the system tries to handle back
     * navigation before the UI has been updated.
     */
    protected open fun scheduleOnBack() {
        mainScope.launch {
            yield()
            onBack()
        }
    }
}
