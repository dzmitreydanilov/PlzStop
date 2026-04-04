package com.please.stop.app.features.expenses.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

actual fun compressImage(imageBytes: ByteArray, maxWidthPx: Int, quality: Int): ByteArray {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

    val originalWidth = options.outWidth
    val originalHeight = options.outHeight

    options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, maxWidthPx)
    options.inJustDecodeBounds = false

    val sampledBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        ?: return imageBytes

    val scaledBitmap = if (sampledBitmap.width > maxWidthPx || sampledBitmap.height > maxWidthPx) {
        val scale = maxWidthPx.toFloat() / maxOf(sampledBitmap.width, sampledBitmap.height)
        val targetWidth = (sampledBitmap.width * scale).toInt()
        val targetHeight = (sampledBitmap.height * scale).toInt()
        sampledBitmap.scale(targetWidth, targetHeight).also {
            if (it != sampledBitmap) sampledBitmap.recycle()
        }
    } else {
        sampledBitmap
    }

    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    scaledBitmap.recycle()

    return outputStream.toByteArray()
}

private fun calculateInSampleSize(width: Int, height: Int, maxPx: Int): Int {
    var inSampleSize = 1
    val longestSide = maxOf(width, height)
    if (longestSide > maxPx) {
        val halfLongest = longestSide / 2
        while (halfLongest / inSampleSize >= maxPx) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
