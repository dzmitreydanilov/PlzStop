package com.please.stop.app.core

interface DocumentSharer {
    suspend fun shareCsv(fileName: String, content: String): Result<Unit>
}
