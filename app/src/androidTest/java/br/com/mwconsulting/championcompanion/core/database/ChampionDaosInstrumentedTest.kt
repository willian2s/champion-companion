package br.com.mwconsulting.championcompanion.core.database

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SaveManualHeartRateMeasurementInput
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import br.com.mwconsulting.championcompanion.data.RoomRepositories
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionDaosInstrumentedTest {
    @Test
    fun queriesPendingHeartRatesAndUsesConditionalTransitions() = runBlocking {
        val database = database()
        try {
            database.deviceDao().insert(device())
            database.heartRateDao().insert(
                HeartRateMeasurementEntity(
                    id = "heart-rate-1",
                    source = "WATCH_D1_HISTORY",
                    bpm = 72,
                    measuredAtEpochMillis = 1_000L,
                    observedLocalDateTime = null,
                    validity = "VALID",
                    deviceId = "device-1",
                    importedAtEpochMillis = 2_000L,
                    parserVersion = "v1",
                    appWorkoutSessionId = null,
                    workoutActivityId = null,
                    deduplicationKey = "d1:1",
                ),
            )

            assertEquals(listOf("heart-rate-1"), database.heartRateDao().findValidPendingForSync().map { it.id })

            database.appWorkoutSessionDao().insert(session())
            assertEquals(0, database.appWorkoutSessionDao().transitionState(
                id = "session-1",
                expectedStates = listOf("ACTIVE"),
                nextState = "FINISHED",
            ))
            assertEquals(1, database.appWorkoutSessionDao().transitionState(
                id = "session-1",
                expectedStates = listOf("AWAITING_RECOVERY"),
                nextState = "ACTIVE",
            ))
            assertNull(database.appWorkoutSessionDao().observeRecoverable().first().singleOrNull())
        } finally {
            database.close()
        }
    }

    @Test
    fun findsPayloadsExpiredBeforeRetentionCutoff() = runBlocking {
        val database = database()
        try {
            database.diagnosticPayloadDao().insert(payload("payload-old", 10L))
            database.diagnosticPayloadDao().insert(payload("payload-current", 20L))

            assertEquals(
                listOf("payload-old"),
                database.diagnosticPayloadDao().findExpired(20L).map { it.id },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun pendingBpmQueryExcludesInvalidUnresolvedAndActiveSyncOperations() = runBlocking {
        val database = database()
        try {
            database.deviceDao().insert(device())
            database.heartRateDao().insert(heartRate("eligible", "VALID", 1_000L, "WATCH_D1_HISTORY"))
            database.heartRateDao().insert(heartRate("invalid", "INVALID", 2_000L, "WATCH_D1_HISTORY"))
            database.heartRateDao().insert(heartRate("unresolved", "VALID", null, "WATCH_D1_HISTORY"))
            database.heartRateDao().insert(heartRate("other-source", "VALID", 3_000L, "MANUAL"))
            database.heartRateDao().insert(heartRate("syncing", "VALID", 4_000L, "WATCH_D1_HISTORY"))
            database.healthSyncOperationDao().insert(
                HealthSyncOperationEntity("sync-1", "syncing", "HEART_RATE_MEASUREMENT", "HEART_RATE", "SYNCING", 1, 4_001L, null),
            )

            assertEquals(listOf("eligible"), database.heartRateDao().findValidPendingForSync().map { it.id })
        } finally {
            database.close()
        }
    }

    @Test
    fun uniqueDeduplicationKeyRejectsDuplicateHeartRateRows() = runBlocking {
        val database = database()
        try {
            database.deviceDao().insert(device())
            database.heartRateDao().insert(heartRate("first", "VALID", 1_000L, "WATCH_D1_HISTORY"))
            val duplicateRejected = runCatching {
                database.heartRateDao().insert(heartRate("first", "VALID", 2_000L, "WATCH_D1_HISTORY").copy(id = "second"))
            }.isFailure

            assertTrue(duplicateRejected)
        } finally {
            database.close()
        }
    }

    @Test
    fun latestValidMeasurementIgnoresInvalidRowsAndUsesNewestResolvedInstant() = runBlocking {
        val database = database()
        try {
            database.deviceDao().insert(device())
            database.heartRateDao().insert(heartRate("older-valid", "VALID", 1_000L, "WATCH_D1_HISTORY"))
            database.heartRateDao().insert(heartRate("newer-invalid", "INVALID", 5_000L, "MANUAL_MEASUREMENT"))
            database.heartRateDao().insert(heartRate("newest-valid", "VALID", 4_000L, "MANUAL_MEASUREMENT"))

            assertEquals("newest-valid", database.heartRateDao().observeLatestValidMeasurement().first()?.id)
        } finally {
            database.close()
        }
    }

    @Test
    fun recentManualMeasurementsFiltersByManualSourceAndLimit() = runBlocking {
        val database = database()
        try {
            database.deviceDao().insert(device())
            database.heartRateDao().insert(heartRate("manual-old", "VALID", 1_000L, "MANUAL_MEASUREMENT"))
            database.heartRateDao().insert(heartRate("watch-new", "VALID", 5_000L, "WATCH_D1_HISTORY"))
            database.heartRateDao().insert(heartRate("manual-mid", "VALID", 3_000L, "MANUAL_MEASUREMENT"))
            database.heartRateDao().insert(heartRate("manual-invalid", "INVALID", 4_000L, "MANUAL_MEASUREMENT"))
            database.heartRateDao().insert(heartRate("manual-new", "VALID", 6_000L, "MANUAL_MEASUREMENT"))

            assertEquals(
                listOf("manual-new", "manual-mid"),
                database.heartRateDao().observeRecentValidManualMeasurements(limit = 2).first().map { it.id },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun roomRepositoriesDeduplicatesManualSaveBySessionAndSkipsSyncQueue() = runBlocking {
        val database = database()
        try {
            database.deviceDao().insert(device())
            val repositories = RoomRepositories(database, Dispatchers.Unconfined)
            val sessionId = HeartRateMeasurementSessionId(uuid(10))
            val first = SaveManualHeartRateMeasurementInput(
                measurementId = HeartRateMeasurementId(uuid(11)),
                sessionId = sessionId,
                deviceId = null,
                bpm = 77,
                measuredAt = Instant.parse("2026-07-22T10:00:00Z"),
                parserVersion = ParserVersion.create("manual-v1").valueOrNull()!!,
            )

            val saved = repositories.saveManualMeasurement(first)
            val duplicate = repositories.saveManualMeasurement(
                first.copy(measurementId = HeartRateMeasurementId(uuid(13)), bpm = 81),
            )

            assertTrue(saved is RepositoryResult.Success)
            assertTrue(duplicate is RepositoryResult.Success)
            assertEquals(
                (saved as RepositoryResult.Success).value.id,
                (duplicate as RepositoryResult.Success).value.id,
            )
            assertEquals(1, repositories.observeRecentManualMeasurements(limit = 10).first().size)
            assertEquals(emptyList<HeartRateMeasurementEntity>(), database.heartRateDao().findValidPendingForSync())
        } finally {
            database.close()
        }
    }

    private fun database(): ChampionDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Room.inMemoryDatabaseBuilder(context, ChampionDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .allowMainThreadQueries()
            .build()
    }

    private fun device() = DeviceEntity(
        id = "device-1",
        platformIdentifier = "platform-1",
        name = "Champion",
        model = StoredDeviceModel.CHAMPION_C001,
        firmware = null,
        associatedAtEpochMillis = 0L,
        lastSuccessfulConnectionAtEpochMillis = null,
        lastImportedAtEpochMillis = null,
        importParserVersion = null,
    )

    private fun session() = AppWorkoutSessionEntity(
        id = "session-1",
        type = "RUNNING",
        createdAtEpochMillis = 0L,
        startedAtEpochMillis = 0L,
        endedAtEpochMillis = null,
        activeDurationNanos = 0L,
        pausedDurationNanos = 0L,
        activeSinceEpochMillis = null,
        pausedSinceEpochMillis = null,
        state = "AWAITING_RECOVERY",
        steps = 0L,
        distanceMeters = null,
        caloriesKilocalories = null,
        averageBpm = null,
        minimumBpm = null,
        maximumBpm = null,
        workoutActivityId = null,
    )

    private fun payload(id: String, capturedAtEpochMillis: Long) = DiagnosticPayloadEntity(
        id = id,
        command = "D1",
        flow = "WATCH_HISTORY",
        privateBytes = byteArrayOf(1),
        byteCount = 1L,
        deviceId = null,
        firmware = null,
        parserVersion = "v1",
        result = "COMPLETED",
        safeError = null,
        capturedAtEpochMillis = capturedAtEpochMillis,
    )

    private fun heartRate(id: String, validity: String, measuredAt: Long?, source: String) = HeartRateMeasurementEntity(
        id = id,
        source = source,
        bpm = 72,
        measuredAtEpochMillis = measuredAt,
        observedLocalDateTime = null,
        validity = validity,
        deviceId = "device-1",
        importedAtEpochMillis = 2_000L,
        parserVersion = "v1",
        appWorkoutSessionId = null,
        workoutActivityId = null,
        deduplicationKey = "d1:$id",
    )

    private fun uuid(last: Int): UUID =
        UUID.fromString("0190029f-2f80-7000-8000-0000000000${last.toString().padStart(2, '0')}")
}
