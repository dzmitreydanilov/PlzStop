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
}
