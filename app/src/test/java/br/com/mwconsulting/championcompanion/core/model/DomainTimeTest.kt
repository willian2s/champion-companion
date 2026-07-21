package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTimeTest {
    @Test
    fun `watch time remains unresolved without timezone evidence`() {
        val observed = ObservedWatchTime(LocalDateTime.of(2026, 7, 20, 9, 30))

        assertEquals(WatchTimeResolution.Unresolved, observed.resolution)
    }

    @Test
    fun `watch time resolves unique local time only with supplied zone`() {
        val observed = ObservedWatchTime(LocalDateTime.of(2026, 7, 20, 9, 30))

        val resolved = observed.resolveIn(ZoneId.of("America/Sao_Paulo"))

        assertEquals(
            Instant.parse("2026-07-20T12:30:00Z"),
            (resolved.resolution as WatchTimeResolution.Resolved).instant,
        )
    }

    @Test
    fun `watch time keeps daylight saving overlap ambiguous`() {
        val observed = ObservedWatchTime(LocalDateTime.of(2026, 11, 1, 1, 30))

        val resolved = observed.resolveIn(ZoneId.of("America/New_York"))

        assertTrue(resolved.resolution is WatchTimeResolution.Ambiguous)
    }

    @Test
    fun `watch time keeps daylight saving gap nonexistent`() {
        val observed = ObservedWatchTime(LocalDateTime.of(2026, 3, 8, 2, 30))

        val resolved = observed.resolveIn(ZoneId.of("America/New_York"))

        assertTrue(resolved.resolution is WatchTimeResolution.Nonexistent)
    }
}
