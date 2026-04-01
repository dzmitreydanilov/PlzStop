package com.please.stop.app.core.db

import androidx.room.RoomDatabase

expect class AppDatabaseFactory {
    fun create(): AppDatabase
}

internal fun RoomDatabase.Builder<AppDatabase>.buildEncrypted(
    passphrase: String?,
): AppDatabase {
    val builder = if (passphrase != null) encrypted(passphrase) else this
    return builder.build()
}
