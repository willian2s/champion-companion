package br.com.mwconsulting.championcompanion.feature.device

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicePocReportExporterInstrumentedTest {
    @Test
    fun exportWritesReportInPrivateCacheAndBuildsShareIntent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val report = DevicePocCycleReport.fromState(
            DeviceUiState(
                cycleId = DevicePocCycleId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                cycleStartedAt = Instant.parse("2026-07-21T12:00:00Z"),
                pocState = DevicePocState.CycleCompleted,
                pocStateHistory = listOf("idle", "transport_ready", "handshake_confirmed", "cycle_completed"),
                stepsPacketCount = 2,
                d0PacketCount = 1,
                f002PacketCount = 5,
                f002BufferedSampleCount = 64,
                finalHeartRateBpm = 72,
                finalHeartRateSource = DevicePocHeartRateFinalSource.F002_ESTIMATE,
            ),
            finishedAt = Instant.parse("2026-07-21T12:00:07Z"),
        )

        val exported = DevicePocReportExporter.export(context, report)
        val shareIntent = DevicePocReportExporter.createShareIntent(exported)

        assertTrue(exported.file.exists())
        assertTrue(exported.file.parentFile?.absolutePath?.endsWith("/cache/poc-reports") == true)
        assertEquals(report.asJson(), exported.file.readText(Charsets.UTF_8))
        assertEquals("content", exported.contentUri.scheme)
        assertEquals("${context.packageName}.fileprovider", exported.contentUri.authority)
        assertEquals(android.content.Intent.ACTION_SEND, shareIntent.action)
        assertEquals("application/json", shareIntent.type)
        assertEquals(exported.contentUri, shareIntent.getParcelableExtra(android.content.Intent.EXTRA_STREAM))
        assertTrue(
            shareIntent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
        )
    }
}
