package com.please.stop.app.uicomponents.error

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType

@Stable
sealed interface ScreenOverlay {
    data class Error(
        val type: ErrorType,
        val title: String? = null,
        val subtitle: String? = null
    ) : com.please.stop.app.uicomponents.error.ScreenOverlay

    data class Message(
        val title: String,
        val subtitle: String? = null,
        val type: com.please.stop.app.uicomponents.error.MessageType = com.please.stop.app.uicomponents.error.MessageType.Info,
        val position: com.please.stop.app.uicomponents.error.SnackbarPosition = com.please.stop.app.uicomponents.error.SnackbarPosition.Top,
    ) : com.please.stop.app.uicomponents.error.ScreenOverlay
}

enum class MessageType { Info, Success }

enum class SnackbarPosition { Top, Bottom }
