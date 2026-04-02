package com.please.stop.app.core.db

import androidx.room.RoomDatabase

expect class AppDatabaseFactory {
    fun create(): AppDatabase
}

internal expect fun RoomDatabase.Builder<AppDatabase>.buildEncrypted(
    passphrase: String?,
): AppDatabase
