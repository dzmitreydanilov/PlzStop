package com.please.stop.app.navigation.nav3

import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Adds a screen to the top of the stack.
 *
 * @param snapshot The stack snapshot to modify
 * @param command The push command containing the screen to add
 */
internal fun push(
    snapshot: MutableList<NavKey>,
    command: Push<NavKey>,
) {
    snapshot += command.screen
}

/**
 * Replaces the current top screen or adds a screen if the stack is empty.
 *
 * @param snapshot The stack snapshot to modify
 * @param command The replace command containing the new screen
 */
internal fun replace(
    snapshot: MutableList<NavKey>,
    command: ReplaceCurrent<NavKey>,
) {
    if (snapshot.isEmpty()) {
        snapshot += command.screen
    } else {
        snapshot.set(
            index = snapshot.indices.last,
            element = command.screen,
        )
    }
}

/**
 * Removes screens until the target screen is reached.
 *
 * If the target screen is not found, all screens except the root are removed.
 *
 * @param snapshot The stack snapshot to modify
 * @param command The popTo command containing the target screen
 */
internal fun popTo(
    snapshot: MutableList<NavKey>,
    command: PopTo<NavKey>,
) {
    val target = command.screen
    val idx = snapshot.indexOfFirst { it == target }

    if (idx == -1) {
        if (snapshot.size > 1) {
            snapshot.removeRange(1, snapshot.size)
        }
    } else {
        val from = idx + 1
        if (from < snapshot.size) {
            snapshot.removeRange(from, snapshot.size)
        }
    }
}

/**
 * Removes the top screen from the stack.
 *
 * @param snapshot The stack snapshot to modify
 * @return true if a screen was removed, false if the stack would become empty
 */
internal fun pop(
    snapshot: MutableList<NavKey>,
): Boolean {
    val result = snapshot.size > 1

    if (result) {
        snapshot.removeLastOrNull()
    }

    return result
}

/**
 * Removes all screens except the root.
 *
 * @param snapshot The stack snapshot to modify
 */
internal fun resetToRoot(snapshot: MutableList<NavKey>) {
    if (snapshot.size > 1) snapshot.removeRange(1, snapshot.size)
}

/**
 * Removes all screens except the current top screen.
 *
 * This function is useful when you want to close a nested navigation graph or
 * a whole application.
 *
 * @param snapshot The stack snapshot to modify
 */
internal fun dropStack(snapshot: MutableList<NavKey>) {
    snapshot.removeRange(0, snapshot.size - 1)
}


/**
 * Atomically replaces the contents of a SnapshotStateList.
 *
 * Uses Compose's snapshot system to ensure the update is atomic and
 * properly triggers recomposition.
 *
 * @param value The new contents for the list
 */
internal fun NavBackStack<NavKey>.swap(
    value: List<NavKey>,
) {
    Snapshot.withMutableSnapshot {
        clear()
        addAll(value)
    }
}

/**
 * Extension function to remove a range of elements from a MutableList.
 *
 * This is a convenience method that uses the subList approach for efficiency.
 *
 * @param fromIndex Start index (inclusive)
 * @param toIndex End index (exclusive)
 */
internal fun MutableList<NavKey>.removeRange(fromIndex: Int, toIndex: Int) {
    subList(fromIndex, toIndex).clear()
}
