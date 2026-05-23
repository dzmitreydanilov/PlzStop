package com.please.stop.app.features.export.data

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal actual fun gzipBase64(bytes: ByteArray): String {
    val output = ByteArrayOutputStream()
    GZIPOutputStream(output).use { gzip -> gzip.write(bytes) }
    return Base64.encode(output.toByteArray())
}
