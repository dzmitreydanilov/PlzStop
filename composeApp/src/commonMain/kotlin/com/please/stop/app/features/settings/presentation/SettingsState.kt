package com.please.stop.app.features.settings.presentation

import androidx.compose.runtime.Composable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.uicomponents.error.ScreenOverlay
import kotlinx.collections.immutable.ImmutableList

sealed interface SettingsState {

    val sections: ImmutableList<SettingsSectionGroup>

    data class Loaded(
        override val sections: ImmutableList<SettingsSectionGroup>,
    ) : SettingsState

    data class Error(
        val errorType: ErrorType,
        override val sections: ImmutableList<SettingsSectionGroup>,
    ) : SettingsState
}

internal val SettingsState.asOverlay: ScreenOverlay?
    @Composable get() = when (this) {
        is SettingsState.Error -> ScreenOverlay.Error(type = errorType)
        else -> null
    }
