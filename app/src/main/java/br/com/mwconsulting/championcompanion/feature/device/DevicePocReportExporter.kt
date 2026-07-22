package br.com.mwconsulting.championcompanion.feature.device

import android.content.Intent
import android.content.Context

typealias ExportedDevicePocReport = ExportedDeviceDiagnosticsReport

object DevicePocReportExporter {
    fun export(context: Context, report: DevicePocCycleReport): ExportedDevicePocReport =
        DeviceDiagnosticsReportExporter.exportJson(
            context = context,
            directoryName = "poc-reports",
            fileName = "cycle-${report.cycleId}.json",
            content = report.asJson(),
        )

    fun createShareIntent(exportedReport: ExportedDevicePocReport): Intent =
        DeviceDiagnosticsReportExporter.createShareIntent(exportedReport)
}
