package com.please.stop.app.features.settings.presentation

import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource

data class SettingsSectionGroup(
    val id: Int,
    val title: StringResource,
    val items: ImmutableList<SettingsItem>,
)
