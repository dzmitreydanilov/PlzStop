package com.please.stop.app.features.settings.presentation

sealed interface SettingsEvent {
    data object DismissError : SettingsEvent
}
