package com.please.stop.app.features.settings.presentation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.settings_section_about
import plzstop.composeapp.generated.resources.settings_section_general
import plzstop.composeapp.generated.resources.settings_section_preferences

class DefaultSettingsSectionsProvider : SettingsSectionsProvider {

    override fun getSections(): ImmutableList<SettingsSectionGroup> = persistentListOf(
        SettingsSectionGroup(
            id = 1,
            title = Res.string.settings_section_general,
            items = persistentListOf(
                SettingsItem.Currency(),
                SettingsItem.Categories(),
                SettingsItem.Budget(),
                SettingsItem.Subscriptions(),
            ),
        ),
        SettingsSectionGroup(
            id = 2,
            title = Res.string.settings_section_preferences,
            items = persistentListOf(
                SettingsItem.Notifications(),
                SettingsItem.Appearance(),
                SettingsItem.ExportData(),
            ),
        ),
        SettingsSectionGroup(
            id = 3,
            title = Res.string.settings_section_about,
            items = persistentListOf(
                SettingsItem.HelpSupport(),
                SettingsItem.PrivacyPolicy(),
                SettingsItem.AppVersion(),
            ),
        ),
    )
}
