package com.please.stop.app.features.expenses.scanner

import com.please.stop.app.utils.toByteArray
import com.please.stop.app.utils.toNsData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

private const val PERCENT_MAX = 100.0

@OptIn(ExperimentalForeignApi::class)
actual fun compressImage(imageBytes: ByteArray, maxWidthPx: Int, quality: Int): ByteArray {
    val nsData = imageBytes.toNsData()
    val image = UIImage.imageWithData(nsData) ?: return imageBytes

    val size = image.size.useContents { Pair(width, height) }
    val originalWidth = size.first
    val originalHeight = size.second
    val longestSide = if (originalWidth > originalHeight) originalWidth else originalHeight

    val scale = if (longestSide > maxWidthPx.toDouble()) {
        maxWidthPx.toDouble() / longestSide
    } else {
        1.0
    }
    val targetWidth = originalWidth * scale
    val targetHeight = originalHeight * scale

    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(targetWidth, targetHeight),
        true,
        1.0,
    )
    image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    if (resizedImage == null) return imageBytes

    val qualityFloat = quality.toDouble() / PERCENT_MAX
    val jpegData = UIImageJPEGRepresentation(resizedImage, qualityFloat)
        ?: return imageBytes

    return jpegData.toByteArray()
}
