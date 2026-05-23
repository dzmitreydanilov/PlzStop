package com.please.stop.app.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.credentials.CredentialManager
import androidx.lifecycle.DefaultLifecycleObserver
import com.please.stop.app.core.AndroidDocumentSharer
import com.please.stop.app.core.DocumentSharer
import com.please.stop.app.core.IFcmTokenProvider
import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.INotificationPermission
import com.please.stop.app.core.buildSettings.AndroidBuildSettingsProvider
import com.please.stop.app.core.buildSettings.AppBuildSettingsProvider
import com.please.stop.app.core.coroutines.AndroidApplicationCoroutinesScope
import com.please.stop.app.core.coroutines.ICoroutineScopeProvider
import com.please.stop.app.core.db.AppDatabaseFactory
import com.please.stop.app.core.db.PassphraseProvider
import com.please.stop.app.features.auth.GoogleAuthProviderImpl
import com.please.stop.app.features.auth.apple.AppleAuthProvider
import com.please.stop.app.features.auth.apple.NoOpAppleAuthProvider
import com.please.stop.app.features.auth.data.AndroidFcmTokenProvider
import com.please.stop.app.features.auth.data.AndroidFirebaseAuthProvider
import com.please.stop.app.features.auth.data.AndroidGoogleAccountStorage
import com.please.stop.app.features.auth.data.AndroidNotificationPermission
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.google.GoogleAuthCredentials
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import com.please.stop.app.features.expenses.data.remote.AndroidFirebaseCallableFunctions
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions
import com.please.stop.app.features.export.data.AndroidExportWorkerScheduler
import com.please.stop.app.kvs.createDataStore
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

private const val GOOGLE_ACCOUNT_DATASTORE_FILE = "google_account_storage.preferences_pb"

actual val platformModule: Module = module {
    single<AppBuildSettingsProvider> {
        val context: Context = get()
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        AndroidBuildSettingsProvider(context = context, debug = isDebug)
    }
    single { PassphraseProvider(context = get()) }
    single { AppDatabaseFactory(context = get(), passphraseProvider = get(), buildSettingsProvider = get()) }
    single<FirebaseCallableFunctions> { AndroidFirebaseCallableFunctions() }
    single {
        AndroidApplicationCoroutinesScope()
    } binds arrayOf(ICoroutineScopeProvider::class, DefaultLifecycleObserver::class)

    // Auth
    single { GoogleAuthCredentials(webClientId = "YOUR_WEB_CLIENT_ID") }
    single { CredentialManager.create(get<Context>()) }
    single<GoogleAuthProvider> {
        GoogleAuthProviderImpl(credentials = get(), credentialManager = get<CredentialManager>())
    }
    single<FirebaseAuthProvider> { AndroidFirebaseAuthProvider() }
    single<AppleAuthProvider> { NoOpAppleAuthProvider() }
    single<IGoogleAccountStorage> {
        AndroidGoogleAccountStorage(
            dataStore = createDataStore(context = get(), prefFileName = GOOGLE_ACCOUNT_DATASTORE_FILE),
        )
    }
    single<IFcmTokenProvider> { AndroidFcmTokenProvider() }
    single<INotificationPermission> { AndroidNotificationPermission(context = get()) }

    single<com.please.stop.app.core.UrlOpener> { com.please.stop.app.core.AndroidUrlOpener(context = get()) }
    single<DocumentSharer> { AndroidDocumentSharer(context = get()) }

    // Export
    single<com.please.stop.app.features.export.domain.ExportWorkerScheduler> {
        AndroidExportWorkerScheduler(context = get())
    }
}
