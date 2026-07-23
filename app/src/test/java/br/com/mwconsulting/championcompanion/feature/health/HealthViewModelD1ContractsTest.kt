package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportResult
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportSummary
import br.com.mwconsulting.championcompanion.core.model.D1HeartRatePacketProgress
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.HeartRateHistoryPeriod
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthViewModelD1ContractsTest {
    @Test
    fun `health ui state starts with manual and d1 states independent`() {
        val state = HealthUiState()

        assertTrue(state.surfaceState is HealthSurfaceState.Idle)
        assertTrue(state.d1ImportState is D1HeartRateImportSurfaceState.Idle)
        assertEquals(HeartRateHistoryPeriod.TODAY, state.selectedHistoryPeriod)
        assertEquals(HeartRateHistoryPeriod.TODAY, state.history.period)
        assertNull(state.latestD1ImportMetadata)
    }

    @Test
    fun `d1 progress exposes normalized fraction only when total exists`() {
        val unknown = D1HeartRatePacketProgress(receivedPacketCount = 2, highestPacketIndex = 2, totalPackets = 0)
        val known = D1HeartRatePacketProgress(receivedPacketCount = 3, highestPacketIndex = 2, totalPackets = 4)

        assertNull(unknown.progressFraction)
        assertEquals(0.5f, known.progressFraction)
    }

    @Test
    fun `d1 import result keeps typed status and persisted data flag`() {
        val summary = D1HeartRateImportSummary(
            receivedPacketCount = 4,
            validPointCount = 3,
            insertedCount = 1,
            duplicateCount = 1,
            conflictCount = 0,
            invalidCount = 1,
            missingIndexCount = 0,
            pendingCreatedCount = 1,
            pendingExistingCount = 1,
            startedAt = Instant.parse("2026-07-23T10:00:00Z"),
            finishedAt = Instant.parse("2026-07-23T10:00:10Z"),
            timedOut = true,
        )

        val result: D1HeartRateImportResult = D1HeartRateImportResult.Partial(summary)

        assertEquals(D1HeartRateImportStatus.PARTIAL, result.status)
        assertTrue(summary.hasPersistedData)
    }

    @Test
    fun `d1 metadata stores dedicated status separate from generic device import metadata`() {
        val metadata = D1HeartRateImportMetadata(
            deviceId = DeviceId(UUID.fromString("0190029f-2f80-7000-8000-000000000001")),
            lastAttemptAt = Instant.parse("2026-07-23T10:00:00Z"),
            lastSuccessAt = Instant.parse("2026-07-23T10:00:10Z"),
            lastResult = D1HeartRateImportStatus.COMPLETED_WITH_GAPS,
            insertedCount = 5,
            duplicateCount = 1,
            conflictCount = 0,
            invalidCount = 2,
            missingIndexCount = 1,
            timedOut = false,
            updatedAt = Instant.parse("2026-07-23T10:00:10Z"),
        )

        assertEquals(D1HeartRateImportStatus.COMPLETED_WITH_GAPS, metadata.lastResult)
        assertEquals(5, metadata.insertedCount)
        assertEquals(1, metadata.missingIndexCount)
    }
}
