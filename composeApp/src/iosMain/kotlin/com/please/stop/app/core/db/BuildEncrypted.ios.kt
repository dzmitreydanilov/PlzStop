package com.please.stop.app.core.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver

internal actual fun RoomDatabase.Builder<AppDatabase>.buildEncrypted(
    passphrase: String?,
): AppDatabase {
    val builder = if (passphrase != null) {
        encrypted(passphrase)
    } else {
        setDriver(NativeSQLiteDriver())
    }
    return builder.build()
}
