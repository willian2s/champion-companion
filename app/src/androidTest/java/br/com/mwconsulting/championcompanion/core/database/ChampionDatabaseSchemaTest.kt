package br.com.mwconsulting.championcompanion.core.database

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ChampionDatabaseSchemaTest {
    @Test
    fun createsEmptyVersionThreeDatabaseWithSQLiteDriver() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, ChampionDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .allowMainThreadQueries()
            .build()

        try {
            assertEquals(1, database.schemaDao().verifyConnection())
            database.deviceDao().insert(DeviceEntity(
                id = "0190029f-2f80-7000-8000-000000000001",
                platformIdentifier = "platform-id",
                name = "Champion C001",
                model = StoredDeviceModel.CHAMPION_C001,
                firmware = null,
                associatedAtEpochMillis = 0L,
                lastSuccessfulConnectionAtEpochMillis = null,
                lastImportedAtEpochMillis = null,
                importParserVersion = null,
            ))
            database.deviceImportMetadataDao().upsert(DeviceImportMetadataEntity(
                id = "metadata-1",
                deviceId = "0190029f-2f80-7000-8000-000000000001",
                importType = "WATCH_D1_HISTORY",
                lastAttemptAtEpochMillis = 1L,
                lastSuccessAtEpochMillis = 2L,
                lastResult = "COMPLETED",
                parserVersion = "d1-v1",
                insertedCount = 1,
                duplicateCount = 0,
                conflictCount = 0,
                invalidCount = 0,
                missingIndexCount = 0,
                timedOut = false,
                updatedAtEpochMillis = 2L,
            ))
            assertEquals(
                "WATCH_D1_HISTORY",
                database.deviceImportMetadataDao()
                    .findByDeviceAndImportType("0190029f-2f80-7000-8000-000000000001", "WATCH_D1_HISTORY")
                    ?.importType,
            )
        } finally {
            database.close()
        }
    }
}
