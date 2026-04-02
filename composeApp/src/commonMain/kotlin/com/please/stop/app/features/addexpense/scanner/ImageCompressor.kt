package com.please.stop.app.features.addexpense.scanner

expect fun compressImage(imageBytes: ByteArray, maxWidthPx: Int = 1024, quality: Int = 80): ByteArray
