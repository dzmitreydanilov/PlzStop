package com.please.stop.app.core.db

import android.content.Context
import android.content.SharedPreferences
import com.please.stop.app.crypto.Crypto
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class PassphraseProvider(
    private val context: Context,
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @OptIn(ExperimentalEncodingApi::class)
    actual fun getOrCreate(): String {
        val stored = prefs.getString(KEY_PASSPHRASE, null)
        if (stored != null) {
            val encrypted = Base64.decode(stored)
            return Crypto.decrypt(encrypted).decodeToString()
        }
        val passphrase = generateRandomPassphrase()
        val encrypted = Crypto.encrypt(passphrase.toByteArray())
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encode(encrypted))
            .apply()
        return passphrase
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateRandomPassphrase(): String {
        val bytes = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(bytes)
        return Base64.encode(bytes)
    }

    private companion object {
        const val PREFS_NAME = "db_passphrase_prefs"
        const val KEY_PASSPHRASE = "encrypted_passphrase"
        const val PASSPHRASE_LENGTH = 32
    }
}
