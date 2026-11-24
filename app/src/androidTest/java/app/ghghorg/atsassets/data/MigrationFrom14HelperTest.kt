package app.ghghorg.atsassets.data

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The migration from 1.0.0's schema 14 checked a second way (41 step 5; your A, 2026-11-24): Room's own
 * MigrationTestHelper builds the version 14 database from 14.json, runs the migration to 15 and validates the result
 * against 15.json; then the app's Room builder reads the same rows as MigrationFrom14Test, from [Migration14Rows]. Both
 * passing with the same rows confirms the file MigrationFrom14Test builds by hand.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom14HelperTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file: File =
        File(instrumentation.targetContext.cacheDir, "migration-from-14-helper.db").also { db ->
            System.loadLibrary("sqlcipher")
            listOf("", "-journal", "-wal", "-shm").forEach { File(db.path + it).delete() }
        }

    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation,
            file,
            SQLCipherDriver(Migration14Rows.PASSPHRASE, null, null),
            AtsDatabase::class,
        )

    @Test
    fun theHelpersVersion14Database_migratesTo15_andReadsTheSameRowsAsTheFileBuiltByHand() {
        runBlocking {
            helper.createDatabase(14).let { connection ->
                try {
                    Migration14Rows.insert(connection)
                } finally {
                    connection.close()
                }
            }
            // Runs AutoMigration(14, 15) and checks every table against 15.json; it throws on any difference.
            helper.runMigrationsAndValidate(15).close()
        }
        val database =
            Room
                .databaseBuilder<AtsDatabase>(instrumentation.targetContext, file.absolutePath)
                .setDriver(SQLCipherDriver(Migration14Rows.PASSPHRASE, null, null))
                .build()
        try {
            runBlocking {
                assertEquals(Migration14Rows.CASH, database.cashEntryDao().loadAll())
                assertEquals(Migration14Rows.INSTRUMENTS, database.instrumentDao().loadAll())
            }
        } finally {
            database.close()
        }
    }
}
