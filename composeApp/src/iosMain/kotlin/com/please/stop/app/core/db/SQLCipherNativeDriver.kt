package com.please.stop.app.core.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.NativeSQLiteDriver

class SQLCipherNativeDriver(
    private val passphrase: String,
    private val enableWal: Boolean = true,
) : SQLiteDriver {

    private val base = NativeSQLiteDriver()
    private val quotedKey: String = passphrase.replace("'", "''")

    override fun open(name: String): SQLiteConnection {
        val conn = base.open(name)
        conn.execSQL("PRAGMA key = '$quotedKey';")
        if (enableWal) {
            conn.execSQL("PRAGMA journal_mode = WAL;")
        }
        return conn
    }
}
