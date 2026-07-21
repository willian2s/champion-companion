package br.com.mwconsulting.championcompanion.core.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionBleDeviceFilterTest {
    @Test
    fun `matches name or local name with champion prefix ignoring case and spaces`() {
        assertTrue(ChampionDeviceFilter.matches(name = "  CHAMPION C001 ", localName = null))
        assertTrue(ChampionDeviceFilter.matches(name = null, localName = " champion-proto "))
        assertFalse(ChampionDeviceFilter.matches(name = "companion", localName = "watch"))
        assertFalse(ChampionDeviceFilter.matches(name = "   ", localName = null))
    }

    @Test
    fun `deduplicate keeps first device by trimmed transport key`() {
        val first = testDevice(transportKey = " key-1 ", name = "Champion One")
        val duplicate = testDevice(transportKey = "key-1", name = "Champion Two")
        val second = testDevice(transportKey = "key-2", name = "Champion Three")

        val result = ChampionDeviceFilter.deduplicate(listOf(first, duplicate, second))

        assertEquals(listOf(first, second), result)
    }
}
