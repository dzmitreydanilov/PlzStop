package com.please.stop.app.features.settings.presentation

import org.jetbrains.compose.resources.StringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.settings_app_version_subtitle
import plzstop.composeapp.generated.resources.settings_app_version_title
import plzstop.composeapp.generated.resources.settings_appearance_subtitle
import plzstop.composeapp.generated.resources.settings_appearance_title
import plzstop.composeapp.generated.resources.settings_budget_subtitle
import plzstop.composeapp.generated.resources.settings_budget_title
import plzstop.composeapp.generated.resources.settings_categories_subtitle
import plzstop.composeapp.generated.resources.settings_categories_title
import plzstop.composeapp.generated.resources.settings_currency_subtitle
import plzstop.composeapp.generated.resources.settings_currency_title
import plzstop.composeapp.generated.resources.settings_export_subtitle
import plzstop.composeapp.generated.resources.settings_export_title
import plzstop.composeapp.generated.resources.settings_help_subtitle
import plzstop.composeapp.generated.resources.settings_help_title
import plzstop.composeapp.generated.resources.settings_notifications_subtitle
import plzstop.composeapp.generated.resources.settings_notifications_title
import plzstop.composeapp.generated.resources.settings_privacy_subtitle
import plzstop.composeapp.generated.resources.settings_privacy_title
import plzstop.composeapp.generated.resources.settings_subscriptions_subtitle
import plzstop.composeapp.generated.resources.settings_subscriptions_title

sealed interface SettingsItem {
    val id: Int
    val title: StringResource
    val subtitle: StringResource
    val emoji: String

    data class Currency(
        override val id: Int = 1,
        override val title: StringResource = Res.string.settings_currency_title,
        override val subtitle: StringResource = Res.string.settings_currency_subtitle,
        override val emoji: String = "💱",
    ) : SettingsItem

    data class Categories(
        override val id: Int = 2,
        override val title: StringResource = Res.string.settings_categories_title,
        override val subtitle: StringResource = Res.string.settings_categories_subtitle,
        override val emoji: String = "📁",
    ) : SettingsItem

    data class Budget(
        override val id: Int = 3,
        override val title: StringResource = Res.string.settings_budget_title,
        override val subtitle: StringResource = Res.string.settings_budget_subtitle,
        override val emoji: String = "📊",
    ) : SettingsItem

    data class Subscriptions(
        override val id: Int = 4,
        override val title: StringResource = Res.string.settings_subscriptions_title,
        override val subtitle: StringResource = Res.string.settings_subscriptions_subtitle,
        override val emoji: String = "🔔",
    ) : SettingsItem

    data class Notifications(
        override val id: Int = 5,
        override val title: StringResource = Res.string.settings_notifications_title,
        override val subtitle: StringResource = Res.string.settings_notifications_subtitle,
        override val emoji: String = "🔔",
    ) : SettingsItem

    data class Appearance(
        override val id: Int = 6,
        override val title: StringResource = Res.string.settings_appearance_title,
        override val subtitle: StringResource = Res.string.settings_appearance_subtitle,
        override val emoji: String = "🌙",
    ) : SettingsItem

    data class ExportData(
        override val id: Int = 7,
        override val title: StringResource = Res.string.settings_export_title,
        override val subtitle: StringResource = Res.string.settings_export_subtitle,
        override val emoji: String = "📤",
    ) : SettingsItem

    data class HelpSupport(
        override val id: Int = 8,
        override val title: StringResource = Res.string.settings_help_title,
        override val subtitle: StringResource = Res.string.settings_help_subtitle,
        override val emoji: String = "❓",
    ) : SettingsItem

    data class PrivacyPolicy(
        override val id: Int = 9,
        override val title: StringResource = Res.string.settings_privacy_title,
        override val subtitle: StringResource = Res.string.settings_privacy_subtitle,
        override val emoji: String = "📄",
    ) : SettingsItem

    data class AppVersion(
        override val id: Int = 10,
        override val title: StringResource = Res.string.settings_app_version_title,
        override val subtitle: StringResource = Res.string.settings_app_version_subtitle,
        override val emoji: String = "ℹ️",
    ) : SettingsItem
}
