package com.please.stop.app.core.db

import androidx.room.Room
import com.please.stop.app.core.buildSettings.AppBuildSettingsProvider
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class AppDatabaseFactory(
    private val passphraseProvider: PassphraseProvider,
    private val buildSettingsProvider: AppBuildSettingsProvider,
) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun create(): AppDatabase {
        val passphrase = resolvePassphrase()
        val documentDir = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        ) ?: error("Unable to resolve NSDocumentDirectory")
        val dbFilePath = "${documentDir.path}/${AppDatabase.NAME}"
        return Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
            )
            .buildEncrypted(passphrase)
    }

    private fun resolvePassphrase(): String? {
        if (buildSettingsProvider.isDebug()) return null
        return passphraseProvider.getOrCreate()
    }
}
