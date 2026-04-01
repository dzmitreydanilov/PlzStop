package com.please.stop.app.core.db

import androidx.room.RoomDatabase

actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.encrypted(
    passphrase: String,
): RoomDatabase.Builder<T> {
    return setDriver(SQLCipherNativeDriver(passphrase = passphrase))
}
