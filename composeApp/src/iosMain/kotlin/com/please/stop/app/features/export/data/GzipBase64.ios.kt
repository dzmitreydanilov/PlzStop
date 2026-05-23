package com.please.stop.app.features.export.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pin
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import platform.zlib.Z_DEFAULT_COMPRESSION
import platform.zlib.Z_DEFAULT_STRATEGY
import platform.zlib.Z_DEFLATED
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.ZLIB_VERSION
import platform.zlib.deflate
import platform.zlib.deflateEnd
import platform.zlib.deflateInit2_
import platform.zlib.z_stream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
internal actual fun gzipBase64(bytes: ByteArray): String = Base64.encode(gzip(bytes))

@OptIn(ExperimentalForeignApi::class)
private fun gzip(bytes: ByteArray): ByteArray = memScoped {
    val stream = alloc<z_stream>()
    val initResult = deflateInit2_(
        strm = stream.ptr,
        level = Z_DEFAULT_COMPRESSION,
        method = Z_DEFLATED,
        windowBits = GZIP_WINDOW_BITS,
        memLevel = GZIP_MEMORY_LEVEL,
        strategy = Z_DEFAULT_STRATEGY,
        version = ZLIB_VERSION,
        stream_size = sizeOf<z_stream>().convert(),
    )
    check(initResult == Z_OK) { "Unable to initialize gzip compression: $initResult" }

    val output = mutableListOf<Byte>()
    val buffer = ByteArray(OUTPUT_BUFFER_SIZE)
    val input = bytes.pin()
    val outputBuffer = buffer.pin()

    try {
        stream.next_in = input.addressOf(0).reinterpret()
        stream.avail_in = bytes.size.convert()

        do {
            stream.next_out = outputBuffer.addressOf(0).reinterpret<UByteVar>()
            stream.avail_out = buffer.size.convert()
            val deflateResult = deflate(strm = stream.ptr, flush = Z_FINISH)
            check(deflateResult == Z_OK || deflateResult == Z_STREAM_END) {
                "Unable to gzip export payload: $deflateResult"
            }

            val written = buffer.size - stream.avail_out.toInt()
            repeat(written) { index ->
                output.add(buffer[index])
            }
        } while (deflateResult != Z_STREAM_END)
    } finally {
        outputBuffer.unpin()
        input.unpin()
        deflateEnd(strm = stream.ptr)
    }

    output.toByteArray()
}

private const val GZIP_WINDOW_BITS = 31
private const val GZIP_MEMORY_LEVEL = 8
private const val OUTPUT_BUFFER_SIZE = 8 * 1024
