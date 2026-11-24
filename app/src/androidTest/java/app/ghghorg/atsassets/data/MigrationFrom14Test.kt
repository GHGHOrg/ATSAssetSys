package app.ghghorg.atsassets.data

import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * AC-71 and plan/release.md section 3 (CR5, 41 step 5; decision of 2026-11-24, your A): the database of 1.0.0, schema
 * version 14, opens as version 15 with every row kept. The version 14 file is built here from 14.json, the schema Room
 * wrote for 1.0.0 (its tables, indices, setup queries and identity hash), through SQLCipher's driver as the app opens
 * its file; then the app's Room builder opens it. No row is typed as SQL beyond the two inserted, so a wrong file makes
 * Room refuse it and the test fails, rather than pass.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom14Test {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var file: File

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        file = File(context.cacheDir, "migration-from-14.db")
        deleteFiles()
    }

    @After
    fun tearDown() {
        deleteFiles()
    }

    private fun deleteFiles() {
        listOf("", "-journal", "-wal", "-shm").forEach { File(file.path + it).delete() }
    }

    private fun schema14(): JsonObject {
        val text = InstrumentationRegistry.getInstrumentation().context.assets
            .open("app.ghghorg.atsassets.data.AtsDatabase/14.json")
            .bufferedReader()
            .use { it.readText() }
        return Json.parseToJsonElement(text).jsonObject.getValue("database").jsonObject
    }

    /** Every table and index of version 14, Room's setup queries with 14's identity hash, and the user version 14. */
    private fun SQLiteConnection.createVersion14(schema: JsonObject) {
        for (entity in schema.getValue("entities").jsonArray) {
            val table = entity.jsonObject
            val name = table.getValue("tableName").jsonPrimitive.content
            execSQL(table.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name))
            table["indices"]?.jsonArray?.forEach { index ->
                execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name))
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { execSQL(it.jsonPrimitive.content) }
        execSQL("PRAGMA user_version = 14")
    }

    @Test
    fun aVersion14Database_opensAsVersion15_withEveryRowKept_andNoPreviousCloseYet() {
        val schema = schema14()
        assertEquals(14, schema.getValue("version").jsonPrimitive.content.toInt())
        SQLCipherDriver(Migration14Rows.PASSPHRASE, null, null).open(file.absolutePath).let { connection ->
            try {
                connection.createVersion14(schema)
                Migration14Rows.insert(connection)
            } finally {
                connection.close()
            }
        }
        val database =
            Room
                .databaseBuilder<AtsDatabase>(context, file.absolutePath)
                .setDriver(SQLCipherDriver(Migration14Rows.PASSPHRASE, null, null))
                .build()
        try {
            runBlocking {
                assertEquals(Migration14Rows.CASH, database.cashEntryDao().loadAll())
                val instruments = database.instrumentDao().loadAll()
                assertEquals(Migration14Rows.INSTRUMENTS, instruments)
                // The stored price reads as it did in 1.0.0, with no previous close until the first refresh (AC-71).
                assertEquals(null, instruments.single().toInstrument().price!!.previousClose)
            }
        } finally {
            database.close()
        }
    }
}
