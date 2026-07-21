package br.com.mwconsulting.championcompanion.core.database

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
