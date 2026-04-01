package com.please.stop.app.core.db

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection

/**
 * Applies SQLCipher encryption to the Room database builder.
 *
 * - Android: uses [net.zetetic.database.sqlcipher.SupportOpenHelperFactory]
 * - iOS: uses [SQLCipherNativeDriver] wrapping NativeSQLiteDriver with PRAGMA key
 */
expect fun <T : RoomDatabase> RoomDatabase.Builder<T>.encrypted(
    passphrase: String,
): RoomDatabase.Builder<T>

fun SQLiteConnection.execSQL(sql: String) {
    val statement = prepare(sql)
    try {
        while (statement.step()) { /* discard rows */ }
    } finally {
        statement.close()
    }
}
