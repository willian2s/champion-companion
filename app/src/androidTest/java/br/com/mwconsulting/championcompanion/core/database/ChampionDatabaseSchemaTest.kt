package br.com.mwconsulting.championcompanion.core.database

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ChampionDatabaseSchemaTest {
    @Test
    fun createsEmptyVersionOneDatabaseWithSQLiteDriver() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, ChampionDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .allowMainThreadQueries()
            .build()

        try {
            assertEquals(1, database.schemaDao().verifyConnection())
        } finally {
            database.close()
        }
    }
}
