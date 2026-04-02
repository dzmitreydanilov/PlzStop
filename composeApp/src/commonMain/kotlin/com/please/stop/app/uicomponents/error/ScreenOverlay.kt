package com.please.stop.app.uicomponents.error

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType

@Stable
sealed interface ScreenOverlay {
    data class Error(
        val type: ErrorType,
        val title: String? = null,
        val subtitle: String? = null
    ) : ScreenOverlay

    data class Message(
        val title: String,
        val subtitle: String? = null,
        val type: MessageType = MessageType.Info,
        val position: SnackbarPosition = SnackbarPosition.Top,
    ) : ScreenOverlay
}

enum class MessageType { Info, Success }

enum class SnackbarPosition { Top, Bottom }
