package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateAndSyncTest {
    @Test
    fun `D1 measurement deduplicates by device source and resolved instant`() {
        val measuredAt = Instant.parse("2026-07-21T10:00:00Z")
        val key = HeartRateDeduplicationKey.D1History(deviceId(), measuredAt, null)

        val measurement = HeartRateMeasurement.create(
            id = measurementId(),
            source = HeartRateSource.WATCH_D1_HISTORY,
            bpm = 72,
            measuredAt = measuredAt,
            validity = DataValidity.VALID,
            deviceId = deviceId(),
            importedAt = Instant.parse("2026-07-21T10:01:00Z"),
            parserVersion = parserVersion(),
            deduplicationKey = key,
        ).valueOrNull()!!

        assertEquals(key, measurement.deduplicationKey)
        assertTrue(measurement.isEligibleForSync)
    }

    @Test
    fun `D1 measurement requires resolved instant or record key`() {
        val result = HeartRateMeasurement.create(
            id = measurementId(),
            source = HeartRateSource.WATCH_D1_HISTORY,
            bpm = 72,
            measuredAt = null,
            observedAt = ObservedWatchTime(java.time.LocalDateTime.of(2026, 7, 21, 10, 0)),
            validity = DataValidity.VALID,
            deviceId = deviceId(),
            parserVersion = parserVersion(),
            deduplicationKey = HeartRateDeduplicationKey.D1History(deviceId(), null, null),
        )

        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER), result)
    }

    @Test
    fun `manual measurements from distinct sessions do not collapse`() {
        val first = HeartRateDeduplicationKey.MeasurementSession(measurementSessionId(4))
        val second = HeartRateDeduplicationKey.MeasurementSession(measurementSessionId(5))

        assertFalse(first == second)
    }

    @Test
    fun `manual measurement factory creates valid non sync record`() {
        val measurement = HeartRateMeasurement.createManualMeasurement(
            id = measurementId(),
            sessionId = measurementSessionId(6),
            deviceId = deviceId(),
            bpm = 78,
            measuredAt = Instant.parse("2026-07-21T10:02:00Z"),
            parserVersion = ParserVersion.create("manual-v1").valueOrNull()!!,
        ).valueOrNull()!!

        assertEquals(HeartRateSource.MANUAL_MEASUREMENT, measurement.source)
        assertFalse(measurement.isEligibleForSync)
        assertEquals(
            HeartRateDeduplicationKey.MeasurementSession(measurementSessionId(6)),
            measurement.deduplicationKey,
        )
    }

    @Test
    fun `sync operation only allows valid state transitions`() {
        val operation = operation()
        val syncing = operation.start(Instant.parse("2026-07-21T10:01:00Z")).valueOrNull()!!
        val synced = syncing.markSynced().valueOrNull()!!

        assertEquals(SyncOperationState.PENDING, operation.state)
        assertEquals(SyncOperationState.SYNCING, syncing.state)
        assertEquals(1, syncing.attemptCount)
        assertEquals(SyncOperationState.SYNCED, synced.state)
        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_STATE), synced.markSynced())
    }

    @Test
    fun `partial failure leaves only affected operation pending for retry`() {
        val successful = operation(measurementId()).start(Instant.parse("2026-07-21T10:01:00Z"))
            .valueOrNull()!!
            .markSynced()
            .valueOrNull()!!
        val failed = operation(measurementId(2)).start(Instant.parse("2026-07-21T10:01:00Z"))
            .valueOrNull()!!
            .markFailed(SafeSyncError.RETRY_LATER)
            .valueOrNull()!!

        assertFalse(successful.isPending)
        assertTrue(failed.isPending)
        assertEquals(SafeSyncError.RETRY_LATER, failed.lastError)
        assertEquals(SyncOperationState.SYNCING, failed.start(Instant.parse("2026-07-21T10:02:00Z")).valueOrNull()!!.state)
    }

    private fun operation(measurementId: HeartRateMeasurementId = measurementId()): HealthSyncOperation =
        HealthSyncOperation.create(
            id = HealthSyncOperationId(uuid(3)),
            key = HealthSyncOperationKey(
                aggregate = HealthSyncAggregate.HeartRateMeasurement(measurementId),
                recordType = HealthSyncRecordType.HEART_RATE,
            ),
        ).valueOrNull()!!

    private fun deviceId() = DeviceId(uuid(1))

    private fun measurementId(last: Int = 2) = HeartRateMeasurementId(uuid(last))

    private fun measurementSessionId(last: Int) = HeartRateMeasurementSessionId(uuid(last))

    private fun parserVersion() = ParserVersion.create("d1-v1").valueOrNull()!!

    private fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
}
