package com.please.stop.app.uicomponents.snackbar.core

import androidx.compose.ui.Alignment
import com.please.stop.app.uicomponents.snackbar.ui.models.BannerMessage

/**
 * A concrete implementation of [SnackbarContent] that represents a message to be displayed in a snackbar.
 *
 * @param T The type of content to be displayed in the snackbar.
 * @property duration The duration for which the snackbar should be displayed.
 * @property content The actual content to be displayed in the snackbar.
 * @property alignment The alignment of the snackbar within its container.
 */
class SnackbarMessage(
    override val duration: SnackbarDuration,
    override val content: BannerMessage,
    override val alignment: Alignment = Alignment.BottomCenter,
) : SnackbarContent<BannerMessage>
