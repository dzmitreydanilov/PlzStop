package com.please.stop.app.features.settings.presentation

import kotlinx.collections.immutable.ImmutableList

interface SettingsSectionsProvider {
    fun getSections(): ImmutableList<SettingsSectionGroup>
}
