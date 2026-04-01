package com.please.stop.app.core.db

import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.encrypted(
    passphrase: String,
): RoomDatabase.Builder<T> {
    val factory = SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8))
    return openHelperFactory(factory)
}
