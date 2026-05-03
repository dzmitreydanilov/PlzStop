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
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
            )
            .buildEncrypted(passphrase)
    }

    private fun resolvePassphrase(): String? {
        if (buildSettingsProvider.isDebug()) return null
        return passphraseProvider.getOrCreate()
    }
}
