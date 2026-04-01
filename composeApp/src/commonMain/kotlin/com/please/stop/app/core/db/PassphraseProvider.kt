package com.please.stop.app.core.db

expect class PassphraseProvider {
    fun getOrCreate(): String
}
