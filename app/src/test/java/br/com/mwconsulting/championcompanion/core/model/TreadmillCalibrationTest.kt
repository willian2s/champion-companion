package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class TreadmillCalibrationTest {
    @Test
    fun `imported calibration derives factor and preserves source metrics`() {
        val reviewed = activity().confirmReview(WorkoutType.TREADMILL, reviewedAt).valueOrNull()!!

        val calibration = TreadmillCalibration.forImportedActivity(
            id = calibrationId(),
            activity = reviewed,
            revision = 1,
            reportedMeters = 1_200.0,
            appliedAt = calibratedAt,
        ).valueOrNull()!!
        val calibrated = reviewed.applyCalibration(calibration).valueOrNull()!!

        assertEquals(DomainValue.Present(1.5), calibration.factor)
        assertEquals(DomainValue.Present(800.0), calibrated.metrics.distances.originalMeters)
        assertEquals(DomainValue.Present(1_200.0), calibrated.metrics.distances.calibratedMeters)
        assertEquals(MetricProvenance.CALIBRATED, calibrated.metrics.calibratedMetrics!!.provenance)
        assertEquals(ActivityWorkflowState.READY_TO_SYNC, calibrated.workflowState)
    }

    @Test
    fun `calibration rejects missing or nonpositive original distance`() {
        val reviewed = activity(originalMeters = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE))
            .confirmReview(WorkoutType.TREADMILL, reviewedAt)
            .valueOrNull()!!

        val result = TreadmillCalibration.forImportedActivity(
            calibrationId(), reviewed, 1, 1_000.0, calibratedAt,
        )

        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_RANGE), result)
    }

    @Test
    fun `session calibration without estimate records explicit absent factor`() {
        val calibration = TreadmillCalibration.forSessionWithoutPositiveEstimate(
            calibrationId(), AppWorkoutSessionId(uuid(4)), 1, 1_000.0, calibratedAt, Duration.ofMinutes(10),
        ).valueOrNull()!!

        assertEquals(DomainValue.Absent(AbsenceReason.NOT_APPLICABLE), calibration.factor)
        assertEquals(DomainValue.Present(1_000.0), calibration.calibratedMeters)
    }

    private fun activity(
        originalMeters: DomainValue<Double> = DomainValue.Present(800.0),
    ): WorkoutActivity = WorkoutActivity.createFromImport(
        WorkoutActivityId(uuid(2)),
        WatchWorkoutParserInput(
            deviceId = DeviceId(uuid(1)), crc = 42, externalBlockIndex = 0,
            parserVersion = ParserVersion.create("d4-v1").valueOrNull()!!,
            parserValidity = ParserValidity.VALID, diagnosticPayloadId = null,
            metrics = WorkoutMetrics(
                startedAt = startedAt, duration = Duration.ofMinutes(20), steps = 1_000,
                caloriesKilocalories = DomainValue.Present(100.0),
                distances = WorkoutDistanceMetrics(
                    originalMeters, DomainValue.Present(850.0),
                    DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
                ),
                averagePaceSecondsPerKilometer = DomainValue.Present(1_500.0),
                averageSpeedMetersPerSecond = DomainValue.Present(0.53),
                averageBpm = DomainValue.Present(90), minimumBpm = DomainValue.Present(80),
                maximumBpm = DomainValue.Present(100),
            ),
            intervals = emptyList(), heartRateSamples = emptyList(),
        ),
        importedAt,
    ).valueOrNull()!!

    private fun calibrationId() = TreadmillCalibrationId(uuid(3))

    private companion object {
        val startedAt: Instant = Instant.parse("2026-07-21T10:00:00Z")
        val importedAt: Instant = Instant.parse("2026-07-21T10:30:00Z")
        val reviewedAt: Instant = Instant.parse("2026-07-21T10:31:00Z")
        val calibratedAt: Instant = Instant.parse("2026-07-21T10:32:00Z")

        fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
    }
}
