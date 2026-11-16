package app.ghghorg.atsassets.data

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The restore's delete and load on real rows (34 step 2; section 8, restore step 7; K2; C3): an in-memory database
 * through SQLCipher's driver, as the app opens its file. The store first holds an old portfolio with a buy, a sell and
 * its allocation, their lines, a hidden row, an instrument with a price, a list value with a text and an override, and
 * settings with a price key and a backup moment. The restore's rows are another portfolio's.
 */
@RunWith(AndroidJUnit4::class)
class RestoreDaoTest {
    private lateinit var database: AtsDatabase

    @Before
    fun open() {
        System.loadLibrary("sqlcipher")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room
                .inMemoryDatabaseBuilder(context, AtsDatabase::class.java)
                .setDriver(SQLCipherDriver(ByteArray(32) { 7 }, null, null))
                .build()
    }

    @After
    fun close() {
        database.close()
    }

    private suspend fun fillOld() {
        database.portfolioDao().insert(PortfolioRow("old", "Old portfolio", 1L))
        val transactions = database.transactionDao()
        transactions.insertTransaction(TransactionRow("ob", "old", "OLDCO", "BUY", "2025-01-02", 2L, "10", 100_000L, null))
        transactions.insertTransaction(TransactionRow("os", "old", "OLDCO", "SELL", "2025-02-01", 3L, "10", 150_000L, null))
        transactions.insertAllocations(listOf(SellAllocationRow("os", "ob", "10")))
        database.cashEntryDao().insertAll(
            listOf(
                CashEntryRow("oo", "2025-01-01", 500_000L, "OPENING_BALANCE", "", 1L, null, null),
                CashEntryRow("c-ob", "2025-01-02", -100_000L, "TRADE", "", 2L, null, "ob"),
                CashEntryRow("c-os", "2025-02-01", 150_000L, "TRADE", "", 3L, null, "os"),
            ),
        )
        transactions.insertHidden(HiddenHoldingRow("old", "OLDCO"))
        val instruments = database.instrumentDao()
        instruments.upsert(InstrumentRow("OLDCO", "STOCK", verified = true, price = "12.5", priceTime = 1_700_000_000L))
        instruments.insertListValue(ListValueRow(list = "SECTOR", value = "Old sector"))
        val seq = instruments.loadListValues().single().seq
        instruments.insertSourceText(SourceTextRow(list = "SECTOR", valueSeq = seq, text = "Old text"))
        instruments.upsertOverride(ClassOverrideRow("old", "OLDCO", "SECTOR", seq))
        val settings = SettingsRow(autoLock = "EVERY_LEAVE", priceKey = "oldkey1", lastBackupAt = 1_700_000_000_000L, backupUnfinished = true)
        database.settingsDao().upsert(settings)
    }

    private val newRows =
        RestoreRows(
            cash =
            listOf(
                CashEntryRow("n-o", "2026-01-01", 1_000_000L, "OPENING_BALANCE", "", 1L, null, null),
                CashEntryRow("n-c", "2026-01-02", -100_000L, "TRADE", "", 2L, null, "n-b"),
            ),
            portfolios = listOf(PortfolioRow("new", "Main", 1L)),
            transactions = listOf(TransactionRow("n-b", "new", "AAPL", "BUY", "2026-01-02", 2L, "10", 100_000L, null)),
            allocations = emptyList(),
            hidden = emptyList(),
            instruments = listOf(InstrumentRow("AAPL", "STOCK", verified = true)),
            listValues = listOf(ListValueRow(1L, "SECTOR", "Technology"), ListValueRow(2L, "GEOGRAPHY", "United States")),
            sourceTexts = listOf(SourceTextRow(1L, "SECTOR", 1L, "Information Technology")),
            overrides = listOf(ClassOverrideRow("new", "AAPL", "GEOGRAPHY", 2L)),
            settings = SettingsRow(autoLock = "IDLE_5_MIN", tabOrder = "overall;portfolios;cash;more"),
        )

    // A snapshot of every table.
    private suspend fun everything(): List<Any?> = listOf(
        database.portfolioDao().loadAll(),
        database.transactionDao().loadAll(),
        database.transactionDao().loadAllocations(),
        database.cashEntryDao().loadAll(),
        database.transactionDao().loadHidden(),
        database.instrumentDao().loadAll(),
        database.instrumentDao().loadListValues(),
        database.instrumentDao().loadSourceTexts(),
        database.instrumentDao().loadOverrides(),
        database.settingsDao().load(),
    )

    // Decision 4: everything stored goes, the price key and the backup moment with it (decision 1); exactly the file's
    // rows are there afterwards.
    @Test
    fun aRestore_leavesExactlyItsRows() = runBlocking {
        fillOld()
        database.restoreDao().replaceAll(newRows)
        assertEquals(
            listOf(
                newRows.portfolios,
                newRows.transactions,
                newRows.allocations,
                newRows.cash,
                newRows.hidden,
                newRows.instruments,
                newRows.listValues,
                newRows.sourceTexts,
                newRows.overrides,
                newRows.settings,
            ),
            everything(),
        )
    }

    // On an empty store too (a fresh install).
    @Test
    fun aRestore_ontoAnEmptyStore_writesItsRows() = runBlocking {
        database.restoreDao().replaceAll(newRows)
        assertEquals(newRows.portfolios, database.portfolioDao().loadAll())
        assertEquals(newRows.settings, database.settingsDao().load())
    }

    // C3, all or nothing: two cash rows with one id fail the load after the delete; the transaction rolls back and every
    // table is as it was, the old price key and backup moment too.
    @Test
    fun aFailedLoad_leavesEverythingAsItWas() = runBlocking {
        fillOld()
        val before = everything()
        val twice =
            RestoreRows(
                cash = newRows.cash + newRows.cash.first(),
                portfolios = newRows.portfolios,
                transactions = newRows.transactions,
                allocations = newRows.allocations,
                hidden = newRows.hidden,
                instruments = newRows.instruments,
                listValues = newRows.listValues,
                sourceTexts = newRows.sourceTexts,
                overrides = newRows.overrides,
                settings = newRows.settings,
            )
        try {
            database.restoreDao().replaceAll(twice)
            fail("The restore did not throw")
        } catch (e: Exception) {
            // Expected: the second row with the same id.
        }
        assertEquals(before, everything())
    }
}
