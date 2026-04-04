package com.please.stop.app.features.expenses.scanner

expect fun compressImage(imageBytes: ByteArray, maxWidthPx: Int = 1024, quality: Int = 80): ByteArray
