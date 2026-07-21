package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchWorkoutActivityTest {
    @Test
    fun `D3 D4 activity always starts pending review`() {
        val activity = activity(steps = 1_000)

        assertEquals(ActivityWorkflowState.PENDING_REVIEW, activity.workflowState)
        assertEquals(WorkoutType.RUNNING, activity.suggestedType)
        assertEquals(null, activity.confirmedType)
    }

    @Test
    fun `suggestion below one thousand steps is free workout`() {
        assertEquals(WorkoutType.FREE_WORKOUT, WorkoutActivity.suggestType(999))
        assertEquals(WorkoutType.RUNNING, WorkoutActivity.suggestType(1_000))
    }

    @Test
    fun `partial import cannot become ready to sync`() {
        val partial = activity(parserValidity = ParserValidity.PARTIAL)

        val result = partial.confirmReview(WorkoutType.RUNNING, importedAt.plusSeconds(60))

        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_STATE), result)
    }

    @Test
    fun `treadmill review requires calibration before sync`() {
        val reviewed = activity().confirmReview(WorkoutType.TREADMILL, importedAt.plusSeconds(60)).valueOrNull()!!

        assertEquals(ActivityWorkflowState.PENDING_CALIBRATION, reviewed.workflowState)
        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_STATE), reviewed.startSync())
    }

    @Test
    fun `reimport preserves review calibration and discarded state`() {
        val discarded = activity()
            .discard()
            .valueOrNull()!!
        val input = parserInput(
            steps = 12,
            parserVersion = ParserVersion.create("d4-v2").valueOrNull()!!,
            calibratedDistance = DomainValue.Present(840.0),
        )

        val reimported = discarded.reimport(input, importedAt.plusSeconds(60)).valueOrNull()!!

        assertEquals(ActivityWorkflowState.DISCARDED, reimported.workflowState)
        assertEquals(WorkoutType.FREE_WORKOUT, reimported.suggestedType)
        assertEquals(DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED), reimported.metrics.distances.calibratedMeters)
    }

    @Test
    fun `reimport preserves confirmed type and calibrated distance`() {
        val reviewed = activity(calibratedDistance = DomainValue.Present(810.0))
            .confirmReview(WorkoutType.TREADMILL, importedAt.plusSeconds(30))
            .valueOrNull()!!
        val input = parserInput(
            steps = 2_000,
            calibratedDistance = DomainValue.Present(900.0),
        )

        val reimported = reviewed.reimport(input, importedAt.plusSeconds(60)).valueOrNull()!!

        assertEquals(WorkoutType.TREADMILL, reimported.confirmedType)
        assertEquals(ActivityWorkflowState.PENDING_CALIBRATION, reimported.workflowState)
        assertEquals(DomainValue.Present(810.0), reimported.metrics.distances.calibratedMeters)
    }

    @Test
    fun `unavailable imported metric remains explicitly absent`() {
        val activity = activity()

        assertEquals(DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE), activity.metrics.distances.estimatedMeters)
        assertFalse(activity.metrics.distances.estimatedMeters is DomainValue.Present)
    }

    @Test
    fun `parser input rejects invalid block metrics intervals and heart rate`() {
        val invalidCrc = WatchWorkoutImport.create(WatchWorkoutImportId(uuid(3)), WorkoutActivityId(uuid(2)), parserInput().copy(crc = 0), importedAt)
        val invalidSample = WorkoutActivity.createFromImport(
            WorkoutActivityId(uuid(2)), parserInput().copy(heartRateSamples = listOf(WorkoutHeartRateSample(startedAt, 221))), importedAt,
        )

        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_RANGE), invalidCrc)
        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_RANGE), invalidSample)
    }

    @Test
    fun `sync workflow retries after failure but cannot discard syncing activity`() {
        val reviewed = activity().confirmReview(WorkoutType.RUNNING, importedAt).valueOrNull()!!
        val failed = reviewed.startSync().valueOrNull()!!.markSyncFailed().valueOrNull()!!

        assertEquals(ActivityWorkflowState.SYNCING, failed.startSync().valueOrNull()!!.workflowState)
        assertEquals(DomainResult.Failure(DomainValidationError.INVALID_STATE), failed.startSync().valueOrNull()!!.discard())
    }

    private fun activity(
        steps: Long = 500,
        parserValidity: ParserValidity = ParserValidity.VALID,
        calibratedDistance: DomainValue<Double> = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
    ): WorkoutActivity = WorkoutActivity.createFromImport(
        id = WorkoutActivityId(uuid(2)),
        input = parserInput(steps, parserValidity, calibratedDistance = calibratedDistance),
        importedAt = importedAt,
    ).valueOrNull()!!

    private fun parserInput(
        steps: Long = 500,
        parserValidity: ParserValidity = ParserValidity.VALID,
        parserVersion: ParserVersion = ParserVersion.create("d4-v1").valueOrNull()!!,
        calibratedDistance: DomainValue<Double> = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
    ) = WatchWorkoutParserInput(
        deviceId = DeviceId(uuid(1)),
        crc = 42,
        externalBlockIndex = 0,
        parserVersion = parserVersion,
        parserValidity = parserValidity,
        diagnosticPayloadId = null,
        metrics = WorkoutMetrics(
            startedAt = startedAt,
            duration = Duration.ofMinutes(20),
            steps = steps,
            caloriesKilocalories = DomainValue.Present(120.0),
            distances = WorkoutDistanceMetrics(
                originalMeters = DomainValue.Present(800.0),
                estimatedMeters = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
                calibratedMeters = calibratedDistance,
            ),
            averagePaceSecondsPerKilometer = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
            averageSpeedMetersPerSecond = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
            averageBpm = DomainValue.Present(90),
            minimumBpm = DomainValue.Present(80),
            maximumBpm = DomainValue.Present(100),
        ),
        intervals = emptyList(),
        heartRateSamples = emptyList(),
    )

    private companion object {
        val startedAt: Instant = Instant.parse("2026-07-21T10:00:00Z")
        val importedAt: Instant = Instant.parse("2026-07-21T10:30:00Z")

        fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
    }
}
