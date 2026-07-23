package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DiagnosticParseResult
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutDiagnosticCapturePolicy
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportProgress
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportStatus
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportSummary
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserPolicy
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisesImportContractsTest {
    @Test
    fun `exercises ui state starts idle with dedicated import state`() {
        val state = ExercisesUiState()

        assertTrue(state.importState is WatchWorkoutImportSurfaceState.Idle)
        assertTrue(state.pendingActivities.isEmpty())
        assertNull(state.latestImportMetadata)
    }

    @Test
    fun `watch workout import progress exposes normalized fractions only when totals exist`() {
        val unknown = WatchWorkoutImportProgress()
        val known = WatchWorkoutImportProgress(
            discoveredBlockCount = 4,
            processedBlockCount = 2,
            currentPacketIndex = 3,
            totalPacketsInBlock = 6,
        )

        assertNull(unknown.overallProgressFraction)
        assertNull(unknown.blockProgressFraction)
        assertEquals(0.5f, known.overallProgressFraction)
        assertEquals(0.5f, known.blockProgressFraction)
    }

    @Test
    fun `watch workout import result keeps typed status and summary`() {
        val summary = WatchWorkoutImportSummary(
            discoveredBlockCount = 3,
            processedBlockCount = 3,
            insertedCount = 1,
            duplicateCount = 1,
            partialCount = 1,
            invalidCount = 0,
            timedOutBlockCount = 0,
            skippedValidCount = 1,
            missingPacketCount = 2,
            startedAt = Instant.parse("2026-07-23T10:00:00Z"),
            finishedAt = Instant.parse("2026-07-23T10:00:20Z"),
            timedOut = true,
        )

        val result: WatchWorkoutImportResult = WatchWorkoutImportResult.Partial(summary)

        assertEquals(WatchWorkoutImportStatus.PARTIAL, result.status)
        assertEquals(2, result.summary?.missingPacketCount)
    }

    @Test
    fun `watch workout parser policy persists only valid or safe partial`() {
        val valid = WatchWorkoutParserPolicy(ParserValidity.VALID, hasRequiredPackets = true, hasSafeMinimumData = true)
        val safePartial = WatchWorkoutParserPolicy(ParserValidity.PARTIAL, hasRequiredPackets = true, hasSafeMinimumData = true)
        val unsafePartial = WatchWorkoutParserPolicy(ParserValidity.PARTIAL, hasRequiredPackets = true, hasSafeMinimumData = false)
        val invalid = WatchWorkoutParserPolicy(ParserValidity.INVALID, hasRequiredPackets = true, hasSafeMinimumData = true)

        assertTrue(valid.shouldPersistActivity)
        assertTrue(safePartial.shouldPersistActivity)
        assertFalse(unsafePartial.shouldPersistActivity)
        assertFalse(invalid.shouldPersistActivity)
    }

    @Test
    fun `watch workout diagnostic policy captures release failures and debug explicit valid`() {
        val policy = WatchWorkoutDiagnosticCapturePolicy()

        assertFalse(policy.shouldCapture(DiagnosticParseResult.COMPLETED, isDebugBuild = false, explicitDebugCaptureRequested = false))
        assertTrue(policy.shouldCapture(DiagnosticParseResult.PARTIAL, isDebugBuild = false, explicitDebugCaptureRequested = false))
        assertTrue(policy.shouldCapture(DiagnosticParseResult.INVALID, isDebugBuild = false, explicitDebugCaptureRequested = false))
        assertTrue(policy.shouldCapture(DiagnosticParseResult.TECHNICAL_ERROR, isDebugBuild = false, explicitDebugCaptureRequested = false))
        assertTrue(policy.shouldCapture(DiagnosticParseResult.COMPLETED, isDebugBuild = true, explicitDebugCaptureRequested = true))
    }

    @Test
    fun `watch workout metadata keeps dedicated import type state`() {
        val metadata = WatchWorkoutImportMetadata(
            deviceId = DeviceId(UUID.fromString("0190029f-2f80-7000-8000-000000000001")),
            lastAttemptAt = Instant.parse("2026-07-23T10:00:00Z"),
            lastSuccessAt = Instant.parse("2026-07-23T10:00:15Z"),
            lastResult = WatchWorkoutImportStatus.COMPLETED,
            insertedCount = 2,
            duplicateCount = 1,
            conflictCount = 0,
            invalidCount = 1,
            missingIndexCount = 3,
            timedOut = false,
            updatedAt = Instant.parse("2026-07-23T10:00:15Z"),
        )

        assertEquals(WatchWorkoutImportStatus.COMPLETED, metadata.lastResult)
        assertEquals(2, metadata.insertedCount)
        assertEquals(3, metadata.missingIndexCount)
    }
}
