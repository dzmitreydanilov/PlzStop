package com.please.stop.app.features.expenses.scanner

expect class DocumentScanner {
    suspend fun scan(): Result<ByteArray>
}
