package com.please.stop.app.features.expenses.scanner

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.resume

@Composable
actual fun rememberDocumentScanner(): DocumentScanner {
    val activity = LocalContext.current as ComponentActivity
    return remember(activity) { DocumentScanner(activity) }
}

actual class DocumentScanner(
    private val activity: ComponentActivity,
) {

    private val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setGalleryImportAllowed(true)
        .setPageLimit(1)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .build()

    actual suspend fun scan(): Result<ByteArray> = runCatching {
        val scanner = GmsDocumentScanning.getClient(scannerOptions)
        val intentSender = scanner.getStartScanIntent(activity).await()

        val activityResult = launchIntentSender(intentSender)

        if (activityResult.resultCode != Activity.RESULT_OK) {
            error("Document scanning was cancelled")
        }

        val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            ?: error("No scanning result received")

        val pages = scanningResult.pages
            ?: error("No pages in scanning result")

        if (pages.isEmpty()) {
            error("No pages scanned")
        }

        val pageUri = pages[0].imageUri
        val rawBytes = activity.contentResolver.openInputStream(pageUri)?.use { it.readBytes() }
            ?: error("Failed to read scanned image")

        compressImage(rawBytes)
    }

    private suspend fun launchIntentSender(
        intentSender: android.content.IntentSender,
    ): ActivityResult = suspendCancellableCoroutine { continuation ->
        val key = "document_scanner_${UUID.randomUUID()}"
        var launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>? = null
        launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            launcher?.unregister()
            continuation.resume(result)
        }

        continuation.invokeOnCancellation {
            launcher.unregister()
        }

        launcher.launch(
            IntentSenderRequest.Builder(intentSender).build()
        )
    }
}
