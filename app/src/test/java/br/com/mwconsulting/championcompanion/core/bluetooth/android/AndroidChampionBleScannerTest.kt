package br.com.mwconsulting.championcompanion.core.bluetooth.android

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidChampionBleScannerTest {
    @Test
    fun `toDiscoveredDeviceOrNull returns null when names are blank`() {
        val view = AndroidScanResultView(
            transportKey = "AA:BB:CC:DD:EE:FF",
            name = "   ",
            localName = null,
            observedAt = Instant.parse("2026-07-21T21:00:00Z"),
        )

        val result = view.toDiscoveredDeviceOrNull()

        assertNull(result)
    }

    @Test
    fun `toDiscoveredDeviceOrNull trims names before building device`() {
        val view = AndroidScanResultView(
            transportKey = "AA:BB:CC:DD:EE:FF",
            name = "  Champion C001  ",
            localName = "  Champion  ",
            observedAt = Instant.parse("2026-07-21T21:00:00Z"),
        )

        val result = checkNotNull(view.toDiscoveredDeviceOrNull())

        assertEquals("Champion C001", result.name)
        assertEquals("Champion", result.localName)
    }
}
