package com.please.stop.app.core.db

import androidx.room.RoomDatabase

internal actual fun RoomDatabase.Builder<AppDatabase>.buildEncrypted(
    passphrase: String?,
): AppDatabase {
    val builder = if (passphrase != null) encrypted(passphrase) else this
    return builder.build()
}
