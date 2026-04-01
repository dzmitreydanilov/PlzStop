package com.please.stop.app.di

import com.please.stop.app.core.db.AppDatabaseFactory
import com.please.stop.app.core.db.PassphraseProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { PassphraseProvider(context = get()) }
    single { AppDatabaseFactory(context = get(), passphraseProvider = get(), buildSettingsProvider = get()) }
}
