package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceProfileDailySummaryTest {
    @Test
    fun `daily summary key deduplicates same device origin and local date`() {
        val key = DailySummaryKey(
            deviceId = deviceId(),
            origin = DataOrigin.WATCH_DAILY_SUMMARY,
            localDate = LocalDate.of(2026, 7, 21),
        )

        val duplicateImportKey = DailySummaryKey(
            deviceId = deviceId(),
            origin = DataOrigin.WATCH_DAILY_SUMMARY,
            localDate = LocalDate.of(2026, 7, 21),
        )

        assertEquals(key, duplicateImportKey)
    }

    @Test
    fun `daily summary update preserves identity and natural key`() {
        val summary = dailySummary()

        val updated = summary.update(
            steps = 1_200,
            estimatedDistanceMeters = 860.0,
            estimatedCaloriesKilocalories = 64.0,
            collectedAt = Instant.parse("2026-07-21T10:00:00Z"),
            updatedAt = Instant.parse("2026-07-21T11:00:00Z"),
            validity = DataValidity.VALID,
        )

        val updatedSummary = updated.valueOrNull()!!
        assertEquals(summary.id, updatedSummary.id)
        assertEquals(summary.key, updatedSummary.key)
        assertEquals(1_200, updatedSummary.steps)
    }

    @Test
    fun `manual daily summary permits absent device`() {
        val result = DailySummary.create(
            id = dailySummaryId(),
            key = DailySummaryKey(null, DataOrigin.MANUAL_ENTRY, LocalDate.of(2026, 7, 21)),
            steps = 0,
            estimatedDistanceMeters = null,
            estimatedCaloriesKilocalories = null,
            collectedAt = Instant.parse("2026-07-21T10:00:00Z"),
            updatedAt = Instant.parse("2026-07-21T10:00:00Z"),
            validity = DataValidity.VALID,
        )

        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `watch daily summary requires device`() {
        val result = DailySummary.create(
            id = dailySummaryId(),
            key = DailySummaryKey(null, DataOrigin.WATCH_DAILY_SUMMARY, LocalDate.of(2026, 7, 21)),
            steps = 0,
            estimatedDistanceMeters = null,
            estimatedCaloriesKilocalories = null,
            collectedAt = Instant.parse("2026-07-21T10:00:00Z"),
            updatedAt = Instant.parse("2026-07-21T10:00:00Z"),
            validity = DataValidity.VALID,
        )

        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER), result)
    }

    @Test
    fun `daily summary rejects negative metrics`() {
        val result = DailySummary.create(
            id = dailySummaryId(),
            key = DailySummaryKey(deviceId(), DataOrigin.WATCH_DAILY_SUMMARY, LocalDate.of(2026, 7, 21)),
            steps = -1,
            estimatedDistanceMeters = 0.0,
            estimatedCaloriesKilocalories = 0.0,
            collectedAt = Instant.parse("2026-07-21T10:00:00Z"),
            updatedAt = Instant.parse("2026-07-21T10:00:00Z"),
            validity = DataValidity.VALID,
        )

        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_RANGE), result)
    }

    @Test
    fun `weight preserves unit instant and origin`() {
        val measuredAt = Instant.parse("2026-07-21T10:00:00Z")

        val result = WeightMeasurement.create(
            id = WeightMeasurementId(UUID.fromString("0190029f-2f80-7000-8000-000000000003")),
            value = 154.3,
            unit = WeightUnit.POUNDS,
            measuredAt = measuredAt,
            origin = DataOrigin.MANUAL_ENTRY,
        )

        val measurement = result.valueOrNull()!!
        assertEquals(WeightUnit.POUNDS, measurement.unit)
        assertEquals(measuredAt, measurement.measuredAt)
        assertEquals(DataOrigin.MANUAL_ENTRY, measurement.origin)
    }

    private fun dailySummary(): DailySummary = DailySummary.create(
        id = dailySummaryId(),
        key = DailySummaryKey(deviceId(), DataOrigin.WATCH_DAILY_SUMMARY, LocalDate.of(2026, 7, 21)),
        steps = 1_000,
        estimatedDistanceMeters = 750.0,
        estimatedCaloriesKilocalories = 60.0,
        collectedAt = Instant.parse("2026-07-21T10:00:00Z"),
        updatedAt = Instant.parse("2026-07-21T10:00:00Z"),
        validity = DataValidity.VALID,
    ).valueOrNull()!!

    private fun deviceId() = DeviceId(UUID.fromString("0190029f-2f80-7000-8000-000000000001"))

    private fun dailySummaryId() = DailySummaryId(UUID.fromString("0190029f-2f80-7000-8000-000000000002"))
}
