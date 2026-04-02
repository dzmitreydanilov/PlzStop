package com.please.stop.app.features.addexpense.scanner

expect class DocumentScanner {
    suspend fun scan(): Result<ByteArray>
}
