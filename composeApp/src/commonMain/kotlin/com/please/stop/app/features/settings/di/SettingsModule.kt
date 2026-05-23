package com.please.stop.app.features.settings.di

import com.please.stop.app.features.settings.presentation.DefaultSettingsSectionsProvider
import com.please.stop.app.features.settings.presentation.SettingsSectionsProvider
import com.please.stop.app.features.settings.presentation.SettingsStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    single<SettingsSectionsProvider> { DefaultSettingsSectionsProvider() }
    viewModel { SettingsStateHolder(sectionsProvider = get()) }
}
