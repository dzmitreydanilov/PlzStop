package com.please.stop.app.features.addexpense.scanner

import com.please.stop.app.utils.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIApplication
import platform.UIKit.UIImageJPEGRepresentation
import platform.VisionKit.VNDocumentCameraViewController
import platform.VisionKit.VNDocumentCameraViewControllerDelegateProtocol
import platform.VisionKit.VNDocumentCameraScan
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class DocumentScanner {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun scan(): Result<ByteArray> = runCatching {
        val imageData = suspendCancellableCoroutine { continuation ->
            val delegate = object : NSObject(), VNDocumentCameraViewControllerDelegateProtocol {
                override fun documentCameraViewController(
                    controller: VNDocumentCameraViewController,
                    didFinishWithScan: VNDocumentCameraScan,
                ) {
                    controller.dismissViewControllerAnimated(true, completion = null)
                    val page = didFinishWithScan.imageOfPageAtIndex(0.toULong())
                    val jpegData = UIImageJPEGRepresentation(page, 0.9)
                    if (jpegData != null) {
                        continuation.resume(jpegData)
                    } else {
                        continuation.resumeWithException(
                            Exception("Failed to convert scanned image to JPEG")
                        )
                    }
                }

                override fun documentCameraViewControllerDidCancel(
                    controller: VNDocumentCameraViewController,
                ) {
                    controller.dismissViewControllerAnimated(true, completion = null)
                    continuation.resumeWithException(Exception("Document scanning was cancelled"))
                }

                override fun documentCameraViewController(
                    controller: VNDocumentCameraViewController,
                    didFailWithError: platform.Foundation.NSError,
                ) {
                    controller.dismissViewControllerAnimated(true, completion = null)
                    continuation.resumeWithException(
                        Exception(didFailWithError.localizedDescription)
                    )
                }
            }

            val scanner = VNDocumentCameraViewController()
            scanner.delegate = delegate

            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: error("No root view controller found")

            rootViewController.presentViewController(scanner, animated = true, completion = null)

            continuation.invokeOnCancellation {
                scanner.dismissViewControllerAnimated(true, completion = null)
            }
        }

        val rawBytes = imageData.toByteArray()
        compressImage(rawBytes)
    }
}
