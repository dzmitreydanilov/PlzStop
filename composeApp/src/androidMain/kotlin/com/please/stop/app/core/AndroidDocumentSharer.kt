package com.please.stop.app.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

internal class AndroidDocumentSharer(
    private val context: Context,
) : DocumentSharer {

    override suspend fun shareCsv(fileName: String, content: String): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, EXPORTS_DIR).apply { mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(text = content, charset = Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND)
            .setType(CSV_MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(shareIntent, null)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private companion object {
        const val EXPORTS_DIR = "exports"
        const val CSV_MIME_TYPE = "text/csv"
    }
}
