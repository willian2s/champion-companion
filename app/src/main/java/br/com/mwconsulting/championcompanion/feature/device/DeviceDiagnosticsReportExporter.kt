package br.com.mwconsulting.championcompanion.feature.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class ExportedDeviceDiagnosticsReport(
    val file: File,
    val contentUri: Uri,
)

object DeviceDiagnosticsReportExporter {
    fun exportJson(
        context: Context,
        directoryName: String,
        fileName: String,
        content: String,
    ): ExportedDeviceDiagnosticsReport {
        val directory = File(context.cacheDir, directoryName).apply { mkdirs() }
        val file = File(directory, fileName).also { it.writeText(content, Charsets.UTF_8) }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return ExportedDeviceDiagnosticsReport(file = file, contentUri = uri)
    }

    fun createShareIntent(exportedReport: ExportedDeviceDiagnosticsReport): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, exportedReport.contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
