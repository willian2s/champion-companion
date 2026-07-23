package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DiagnosticCaptureFlow
import br.com.mwconsulting.championcompanion.core.model.DiagnosticDerivedOutputKey
import br.com.mwconsulting.championcompanion.core.model.DiagnosticDerivedOutputType
import br.com.mwconsulting.championcompanion.core.model.DiagnosticParseResult
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayload
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadCapture
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadId
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportStatus
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomRepositoriesWatchWorkoutImportTest {
    @Test
    fun `plan watch workout persistence keeps new import when no existing crc`() {
        val incomingActivity = activityEntity(id = "activity-new", parserValidity = "VALID", workflowState = "PENDING_REVIEW")
        val incomingImport = importEntity(id = "import-new", activityId = incomingActivity.id, parserValidity = "VALID")

        val plan = planWatchWorkoutPersistence(incomingActivity, incomingImport, existingImport = null, existingActivity = null)

        assertNotNull(plan)
        assertFalse(plan!!.isReimport)
        assertEquals("activity-new", plan.persistedActivity.id)
        assertEquals("import-new", plan.persistedImport.id)
    }

    @Test
    fun `plan watch workout persistence preserves review workflow and calibration on reimport`() {
        val existingActivity = activityEntity(
            id = "activity-existing",
            parserValidity = "VALID",
            workflowState = ActivityWorkflowState.READY_TO_SYNC.name,
            confirmedType = "RUNNING",
            reviewedAtEpochMillis = 2000L,
            calibrationId = "calibration-1",
            calibratedDistanceMeters = 1234.0,
            calibratedAveragePaceSecondsPerKilometer = 321.0,
            calibratedAverageSpeedMetersPerSecond = 4.5,
        )
        val existingImport = importEntity(id = "import-existing", activityId = existingActivity.id, parserValidity = "VALID")
        val incomingActivity = activityEntity(id = "activity-incoming", parserValidity = "PARTIAL", workflowState = "PENDING_REVIEW")
        val incomingImport = importEntity(id = "import-incoming", activityId = incomingActivity.id, parserValidity = "PARTIAL")

        val plan = planWatchWorkoutPersistence(incomingActivity, incomingImport, existingImport, existingActivity)

        assertNotNull(plan)
        assertTrue(plan!!.isReimport)
        assertEquals(existingActivity.id, plan.persistedActivity.id)
        assertEquals(existingActivity.confirmedType, plan.persistedActivity.confirmedType)
        assertEquals(existingActivity.workflowState, plan.persistedActivity.workflowState)
        assertEquals(existingActivity.calibrationId, plan.persistedActivity.calibrationId)
        assertEquals(existingImport.id, plan.persistedImport.id)
        assertEquals(existingActivity.id, plan.persistedImport.activityId)
    }

    @Test
    fun `plan watch workout persistence rejects invalid parser output`() {
        val incomingActivity = activityEntity(id = "activity-invalid", parserValidity = "INVALID", workflowState = "PENDING_REVIEW")
        val incomingImport = importEntity(id = "import-invalid", activityId = incomingActivity.id, parserValidity = "INVALID")

        val plan = planWatchWorkoutPersistence(incomingActivity, incomingImport, existingImport = null, existingActivity = null)

        assertNull(plan)
    }

    @Test
    fun `watch workout metadata mapping uses dedicated import type and preserves status`() {
        val metadata = WatchWorkoutImportMetadata(
            deviceId = DeviceId(uuid(1)),
            lastAttemptAt = Instant.parse("2026-07-23T10:00:00Z"),
            lastSuccessAt = Instant.parse("2026-07-23T10:00:15Z"),
            lastResult = WatchWorkoutImportStatus.COMPLETED,
            parserVersion = parserVersion(),
            insertedCount = 2,
            duplicateCount = 1,
            conflictCount = 0,
            invalidCount = 1,
            missingIndexCount = 3,
            timedOut = false,
            updatedAt = Instant.parse("2026-07-23T10:00:15Z"),
        )

        val entity = metadata.toEntity(id = "meta-1")
        val roundTrip = entity.toWatchWorkoutImportMetadata()

        assertEquals(WATCH_D3_D4_IMPORT_TYPE, entity.importType)
        assertNotNull(roundTrip)
        assertEquals(WatchWorkoutImportStatus.COMPLETED, roundTrip!!.lastResult)
        assertEquals(3, roundTrip.missingIndexCount)
    }

    @Test
    fun `lookup mapping skips valid and retries partial`() {
        val validLookup = br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportLookupRecord(
            importId = uuid(2).toString(),
            activityId = uuid(3).toString(),
            deviceId = uuid(1).toString(),
            crc = 0x1234,
            parserValidity = "VALID",
            importedAtEpochMillis = 1000L,
            workflowState = "PENDING_REVIEW",
        ).toDomain()!!
        val partialLookup = br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportLookupRecord(
            importId = uuid(4).toString(),
            activityId = uuid(5).toString(),
            deviceId = uuid(1).toString(),
            crc = 0x5678,
            parserValidity = "PARTIAL",
            importedAtEpochMillis = 1000L,
            workflowState = "PENDING_REVIEW",
        ).toDomain()!!

        assertTrue(validLookup.canSkipOnResume)
        assertFalse(validLookup.shouldRetryOnResume)
        assertFalse(partialLookup.canSkipOnResume)
        assertTrue(partialLookup.shouldRetryOnResume)
    }

    @Test
    fun `diagnostic payload derived outputs map to entities`() {
        val payloadId = DiagnosticPayloadId(uuid(9))
        val payload = (DiagnosticPayload.create(
            id = payloadId,
            capture = DiagnosticPayloadCapture(
                command = "D4",
                flow = DiagnosticCaptureFlow.WATCH_WORKOUT,
                privateBytes = byteArrayOf(0x01, 0x02),
                deviceId = DeviceId(uuid(1)),
                firmware = "1.0.0",
                parserVersion = parserVersion(),
                capturedAt = Instant.parse("2026-07-23T10:00:00Z"),
            ),
            result = DiagnosticParseResult.PARTIAL,
        ) as DomainResult.Success).value
            .recordDerivedOutput(DiagnosticDerivedOutputKey(payloadId, DiagnosticDerivedOutputType.WATCH_WORKOUT_IMPORT, 0))
            .let { (it as DomainResult.Success).value }

        val entities = payload.toDerivedOutputEntities(listOf(uuid(10), uuid(11)).iterator())

        assertEquals(1, entities.size)
        assertEquals(payloadId.value.toString(), entities.first().payloadId)
        assertEquals(DiagnosticDerivedOutputType.WATCH_WORKOUT_IMPORT.name, entities.first().outputType)
    }

    private fun activityEntity(
        id: String,
        parserValidity: String,
        workflowState: String,
        confirmedType: String? = null,
        reviewedAtEpochMillis: Long? = null,
        calibrationId: String? = null,
        calibratedDistanceMeters: Double? = null,
        calibratedAveragePaceSecondsPerKilometer: Double? = null,
        calibratedAverageSpeedMetersPerSecond: Double? = null,
    ) = WorkoutActivityEntity(
        id = id,
        origin = DataOrigin.WATCH_D3_D4.name,
        deviceId = uuid(1).toString(),
        parserValidity = parserValidity,
        importedAtEpochMillis = 1000L,
        parserVersion = parserVersion().value,
        diagnosticPayloadId = null,
        startedAtEpochMillis = 1000L,
        durationNanos = 60_000_000_000L,
        steps = 1500L,
        caloriesKilocalories = 12.3,
        originalDistanceMeters = 1000.0,
        estimatedDistanceMeters = null,
        calibratedDistanceMeters = calibratedDistanceMeters,
        importedAveragePaceSecondsPerKilometer = 300.0,
        importedAverageSpeedMetersPerSecond = 3.3,
        calibratedAveragePaceSecondsPerKilometer = calibratedAveragePaceSecondsPerKilometer,
        calibratedAverageSpeedMetersPerSecond = calibratedAverageSpeedMetersPerSecond,
        averageBpm = 120,
        minimumBpm = 110,
        maximumBpm = 130,
        suggestedType = "RUNNING",
        confirmedType = confirmedType,
        workflowState = workflowState,
        reviewedAtEpochMillis = reviewedAtEpochMillis,
        calibrationId = calibrationId,
    )

    private fun importEntity(id: String, activityId: String, parserValidity: String) = WatchWorkoutImportEntity(
        id = id,
        activityId = activityId,
        deviceId = uuid(1).toString(),
        origin = DataOrigin.WATCH_D3_D4.name,
        crc = 0x1234,
        externalBlockIndex = 7,
        parserVersion = parserVersion().value,
        parserValidity = parserValidity,
        diagnosticPayloadId = null,
        importedAtEpochMillis = 1000L,
    )

    private fun parserVersion() = (ParserVersion.create("d4-v1") as DomainResult.Success).value

    private fun uuid(last: Int) = UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
}
