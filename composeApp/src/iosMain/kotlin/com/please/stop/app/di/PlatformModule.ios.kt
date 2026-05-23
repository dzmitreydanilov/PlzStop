package com.please.stop.app.di

import com.please.stop.app.core.ApplicationBuildSettings
import com.please.stop.app.core.DocumentSharer
import com.please.stop.app.core.IFcmTokenProvider
import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.INotificationPermission
import com.please.stop.app.core.IosDocumentSharer
import com.please.stop.app.core.buildSettings.AppBuildSettingsProvider
import com.please.stop.app.core.coroutines.ApplicationCoroutineScopeProvider
import com.please.stop.app.core.coroutines.ICoroutineScopeProvider
import com.please.stop.app.core.db.AppDatabaseFactory
import com.please.stop.app.core.db.PassphraseProvider
import com.please.stop.app.features.auth.apple.AppleAuthProvider
import com.please.stop.app.features.auth.apple.IosAppleAuthProvider
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.data.IosFcmTokenProvider
import com.please.stop.app.features.auth.data.IosFirebaseAuthProvider
import com.please.stop.app.features.auth.data.IosGoogleAccountStorage
import com.please.stop.app.features.auth.data.IosNotificationPermission
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import com.please.stop.app.features.auth.google.IosGoogleAuthProvider
import com.please.stop.app.features.export.data.IosExportWorkerScheduler
import com.please.stop.app.features.export.domain.ExportWorkerScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AppBuildSettingsProvider> { ApplicationBuildSettings() }
    single { PassphraseProvider() }
    single { AppDatabaseFactory(passphraseProvider = get(), buildSettingsProvider = get()) }
    single<ICoroutineScopeProvider> { ApplicationCoroutineScopeProvider() }
    single { IosAppLifecycleHandler(featureFlagsObserver = get(), dataWarmUpObserver = get(), scopeProvider = get()) }

    // Auth
    single<GoogleAuthProvider> { IosGoogleAuthProvider(bridge = get()) }
    single<FirebaseAuthProvider> { IosFirebaseAuthProvider(bridge = get()) }
    single<AppleAuthProvider> { IosAppleAuthProvider(bridge = get()) }
    single<IGoogleAccountStorage> { IosGoogleAccountStorage() }
    single<IFcmTokenProvider> { IosFcmTokenProvider(bridge = get()) }
    single<INotificationPermission> { IosNotificationPermission() }

    single<com.please.stop.app.core.UrlOpener> { com.please.stop.app.core.IosUrlOpener() }
    single<DocumentSharer> { IosDocumentSharer() }

    // Export
    single<ExportWorkerScheduler> {
        IosExportWorkerScheduler(exportWorkRunner = get(), scopeProvider = get())
    }
}
