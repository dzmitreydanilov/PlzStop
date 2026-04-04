package com.please.stop.app.core.db

import android.content.Context
import androidx.room.Room
import com.please.stop.app.core.buildSettings.AppBuildSettingsProvider

actual class AppDatabaseFactory(
    private val context: Context,
    private val passphraseProvider: PassphraseProvider,
    private val buildSettingsProvider: AppBuildSettingsProvider,
) {
    actual fun create(): AppDatabase {
        val passphrase = resolvePassphrase()
        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        return Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath,
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .buildEncrypted(passphrase)
    }

    private fun resolvePassphrase(): String? {
        if (buildSettingsProvider.isDebug()) return null
        return passphraseProvider.getOrCreate()
    }
}
