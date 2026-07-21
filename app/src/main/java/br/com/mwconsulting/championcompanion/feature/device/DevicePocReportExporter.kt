package br.com.mwconsulting.championcompanion.feature.device

import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class ExportedDevicePocReport(
    val file: File,
    val contentUri: Uri,
)

object DevicePocReportExporter {
    fun export(context: Context, report: DevicePocCycleReport): ExportedDevicePocReport {
        val directory = File(context.cacheDir, "poc-reports").apply { mkdirs() }
        val file = File(directory, "cycle-${report.cycleId}.json").also { file ->
            file.writeText(report.asJson(), Charsets.UTF_8)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return ExportedDevicePocReport(file = file, contentUri = uri)
    }

    fun createShareIntent(exportedReport: ExportedDevicePocReport): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, exportedReport.contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
