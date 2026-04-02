package com.please.stop.app.uicomponents.snackbar.core

import androidx.compose.ui.Alignment


/**
 * Interface representing the content of a snackbar message.
 * It encapsulates both the content to be displayed and the duration for which it should be shown.
 *
 * @param T The type of content to be displayed in the snackbar.
 */
interface SnackbarContent<T> {
    /**
     * The duration for which the snackbar should be displayed.
     * This can be a predefined duration like [com.please.stop.app.uicomponents.snackbar.core.SnackbarDuration.Short] or a custom duration.
     */
    val duration: com.please.stop.app.uicomponents.snackbar.core.SnackbarDuration

    /**
     * The actual content to be displayed in the snackbar.
     * This can be any type T, which will be rendered by the snackbar content composable.
     */
    val content: T

    /**
     * The alignment of the snackbar within its container.
     * Determines where the snackbar appears (e.g., top or bottom) and the animation direction.
     * Defaults to [Alignment.BottomCenter].
     */
    val alignment: Alignment
        get() = Alignment.BottomCenter
}
