package br.com.mwconsulting.championcompanion.feature.device

import android.content.Context
import android.content.Intent

typealias ExportedDeviceFeatureReport = ExportedDeviceDiagnosticsReport

object DeviceDiagnosticsFeatureReportExporter {
    fun export(context: Context, report: DeviceDiagnosticsReport): ExportedDeviceFeatureReport =
        DeviceDiagnosticsReportExporter.exportJson(
            context = context,
            directoryName = "poc-reports",
            fileName = "device-diagnostics-${report.capturedAt.replace(":", "-")}.json",
            content = report.asJson(),
        )

    fun createShareIntent(exportedReport: ExportedDeviceFeatureReport): Intent =
        DeviceDiagnosticsReportExporter.createShareIntent(exportedReport)
}
