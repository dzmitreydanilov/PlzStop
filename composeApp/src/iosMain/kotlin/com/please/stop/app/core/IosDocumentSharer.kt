package com.please.stop.app.core

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController

internal class IosDocumentSharer : DocumentSharer {

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    override suspend fun shareCsv(fileName: String, content: String): Result<Unit> = runCatching {
        val fileUrl = NSURL.fileURLWithPath("${NSTemporaryDirectory()}$fileName")
        NSString.create(string = content).writeToURL(
            url = fileUrl,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        val shareSheet = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null,
        )
        presentIosViewController(shareSheet)
    }
}
