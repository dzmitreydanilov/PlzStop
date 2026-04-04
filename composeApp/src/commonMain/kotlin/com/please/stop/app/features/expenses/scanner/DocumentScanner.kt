package com.please.stop.app.features.expenses.scanner

import androidx.compose.runtime.Composable

expect class DocumentScanner {
    suspend fun scan(): Result<ByteArray>
}

@Composable
expect fun rememberDocumentScanner(): DocumentScanner
