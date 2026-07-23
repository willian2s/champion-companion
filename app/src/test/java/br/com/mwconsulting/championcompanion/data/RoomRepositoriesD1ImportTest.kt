package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity
import br.com.mwconsulting.championcompanion.core.model.HeartRateDeduplicationKey
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomRepositoriesD1ImportTest {
    @Test
    fun `plan import is idempotent for repeated D1 batch`() {
        val measurement = d1Measurement(id = "m-1", bpm = 72, measuredAt = Instant.parse("2026-07-23T10:00:00Z"))

        val first = planD1Import(
            existingMeasurementsByKey = emptyMap(),
            existingPendingAggregateIds = emptySet(),
            incomingMeasurements = listOf(measurement),
        )
        val second = planD1Import(
            existingMeasurementsByKey = first.measurementsToInsert.associateBy { it.deduplicationKey },
            existingPendingAggregateIds = first.pendingOperationAggregateIdsToCreate,
            incomingMeasurements = listOf(measurement),
        )

        assertEquals(1, first.insertedCount)
        assertEquals(1, first.pendingCreatedCount)
        assertEquals(0, second.insertedCount)
        assertEquals(1, second.duplicateCount)
        assertEquals(1, second.pendingExistingCount)
    }

    @Test
    fun `plan import preserves first record on same timestamp with different bpm`() {
        val existing = d1Measurement(id = "m-1", bpm = 72, measuredAt = Instant.parse("2026-07-23T10:00:00Z"))
        val conflicting = d1Measurement(id = "m-2", bpm = 96, measuredAt = Instant.parse("2026-07-23T10:00:00Z"))

        val plan = planD1Import(
            existingMeasurementsByKey = mapOf(existing.deduplicationKey to existing),
            existingPendingAggregateIds = setOf(existing.id),
            incomingMeasurements = listOf(conflicting),
        )

        assertEquals(0, plan.insertedCount)
        assertEquals(0, plan.duplicateCount)
        assertEquals(1, plan.conflictCount)
        assertEquals(0, plan.pendingCreatedCount)
        assertEquals(1, plan.pendingExistingCount)
    }

    @Test
    fun `plan import repairs missing pending operation for existing D1 record`() {
        val existing = d1Measurement(id = "m-1", bpm = 72, measuredAt = Instant.parse("2026-07-23T10:00:00Z"))

        val plan = planD1Import(
            existingMeasurementsByKey = mapOf(existing.deduplicationKey to existing),
            existingPendingAggregateIds = emptySet(),
            incomingMeasurements = listOf(existing),
        )

        assertEquals(0, plan.insertedCount)
        assertEquals(1, plan.duplicateCount)
        assertEquals(1, plan.pendingCreatedCount)
        assertEquals(setOf(existing.id), plan.pendingOperationAggregateIdsToCreate)
    }

    private fun d1Measurement(id: String, bpm: Int, measuredAt: Instant): HeartRateMeasurementEntity {
        val deduplicationKey = HeartRateDeduplicationKey.D1History(
            deviceId = br.com.mwconsulting.championcompanion.core.model.DeviceId(uuid(1)),
            resolvedMeasuredAt = measuredAt,
            recordKey = null,
        ).encode()
        return HeartRateMeasurementEntity(
            id = id,
            source = HeartRateSource.WATCH_D1_HISTORY.name,
            bpm = bpm,
            measuredAtEpochMillis = measuredAt.toEpochMilli(),
            observedLocalDateTime = null,
            validity = "VALID",
            deviceId = uuid(1).toString(),
            importedAtEpochMillis = measuredAt.toEpochMilli(),
            parserVersion = "d1-v1",
            appWorkoutSessionId = null,
            workoutActivityId = null,
            deduplicationKey = deduplicationKey,
        )
    }

    @Suppress("unused")
    private fun manualMeasurement(id: String, measuredAt: Instant): HeartRateMeasurementEntity = HeartRateMeasurementEntity(
        id = id,
        source = HeartRateSource.MANUAL_MEASUREMENT.name,
        bpm = 80,
        measuredAtEpochMillis = measuredAt.toEpochMilli(),
        observedLocalDateTime = null,
        validity = "VALID",
        deviceId = uuid(1).toString(),
        importedAtEpochMillis = measuredAt.toEpochMilli(),
        parserVersion = "manual-v1",
        appWorkoutSessionId = null,
        workoutActivityId = null,
        deduplicationKey = HeartRateDeduplicationKey.MeasurementSession(
            HeartRateMeasurementSessionId(uuid(2)),
        ).encode(),
    )

    private fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
}
