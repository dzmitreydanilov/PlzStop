package com.please.stop.app.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.please.stop.app.core.buildSettings.AndroidBuildSettingsProvider
import com.please.stop.app.core.buildSettings.AppBuildSettingsProvider
import com.please.stop.app.core.db.AppDatabaseFactory
import com.please.stop.app.core.db.PassphraseProvider
import com.please.stop.app.features.expenses.data.remote.AndroidFirebaseCallableFunctions
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AppBuildSettingsProvider> {
        val context: Context = get()
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        AndroidBuildSettingsProvider(context = context, debug = isDebug)
    }
    single { PassphraseProvider(context = get()) }
    single { AppDatabaseFactory(context = get(), passphraseProvider = get(), buildSettingsProvider = get()) }
    single<FirebaseCallableFunctions> { AndroidFirebaseCallableFunctions() }
}
