package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMappersTest {
    @Test
    fun `device mapper preserves optional import metadata absence`() {
        val associatedAt = Instant.parse("2026-07-21T12:00:00Z")
        val device = Device.create(
            id = DeviceId(UuidV7Generator(Clock.fixed(associatedAt, ZoneOffset.UTC), Random(1)).next()),
            platformIdentifier = "platform-id",
            name = "Champion C001",
            associatedAt = associatedAt,
        ).valueOrNull()!!

        val mapped = device.toEntity().toDomain()

        assertEquals(device, mapped)
    }
}
