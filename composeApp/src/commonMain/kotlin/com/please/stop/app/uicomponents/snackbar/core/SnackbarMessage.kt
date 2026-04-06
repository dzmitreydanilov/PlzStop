package com.please.stop.app.uicomponents.snackbar.core

import androidx.compose.ui.Alignment

/**
 * A concrete implementation of [com.please.stop.app.uicomponents.snackbar.core.SnackbarContent]
 * that represents a message to be displayed in a snackbar.
 *
 * @param T The type of content to be displayed in the snackbar.
 * @property duration The duration for which the snackbar should be displayed.
 * @property content The actual content to be displayed in the snackbar.
 * @property alignment The alignment of the snackbar within its container.
 */
class SnackbarMessage(
    override val duration: com.please.stop.app.uicomponents.snackbar.core.SnackbarDuration,
    override val content: com.please.stop.app.uicomponents.snackbar.ui.models.BannerMessage,
    override val alignment: Alignment = Alignment.BottomCenter,
) : com.please.stop.app.uicomponents.snackbar.core.SnackbarContent<
    com.please.stop.app.uicomponents.snackbar.ui.models.BannerMessage
    >
