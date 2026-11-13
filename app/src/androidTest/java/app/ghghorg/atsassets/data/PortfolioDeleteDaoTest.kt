package app.ghghorg.atsassets.data

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The portfolio deletion's queries on real rows (29 step 7; the review's finding 1; 4.7, K2): an in-memory database
 * through SQLCipher's driver, as the app opens its file. Portfolio P has a buy, a sell with its allocation, a reversal of
 * the sell with its line, a split, a hidden row and an override; Portfolio Q has the same ticker, its line, a hidden row
 * and an override; the opening balance, a deposit and its reversal are direct entries.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioDeleteDaoTest {
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

    private fun trade(
        id: String,
        portfolioId: String,
        type: String,
        date: String,
        seq: Long,
        cents: Long,
        reversesId: String? = null,
    ) = TransactionRow(id, portfolioId, "AAPL", type, date, seq, if (type == "REVERSAL" || type == "SPLIT") "0" else "10", cents, reversesId)

    private fun line(
        trade: TransactionRow,
        cents: Long,
    ) = CashEntryRow("c-${trade.id}", trade.date, cents, "TRADE", "", trade.enteredSeq, null, trade.id)

    private val pBuy = trade("pb", "p", "BUY", "2026-01-02", 2L, 100_000L)
    private val pSell = trade("ps", "p", "SELL", "2026-02-01", 3L, 150_000L)
    private val pReversal = trade("pr", "p", "REVERSAL", "2026-02-05", 4L, 0L, reversesId = "ps")
    private val pSplit = trade("px", "p", "SPLIT", "2026-03-01", 5L, 0L).copy(splitTo = 2, splitFrom = 1)
    private val qBuy = trade("qb", "q", "BUY", "2026-01-03", 6L, 50_000L)
    private val opening = CashEntryRow("o1", "2026-01-01", 1_000_000L, "OPENING_BALANCE", "", 1L, null, null)
    private val deposit = CashEntryRow("d1", "2026-01-10", 5_000L, "DEPOSIT", "", 7L, null, null)
    private val undo = CashEntryRow("u1", "2026-01-11", -5_000L, "ADJUSTMENT", "Reversal", 8L, "d1", null)

    /** Every row of the fixture; returns the list value's seq. */
    private suspend fun fill(): Long {
        database.portfolioDao().insert(PortfolioRow("p", "Portfolio P", 1L))
        database.portfolioDao().insert(PortfolioRow("q", "Portfolio Q", 2L))
        val transactions = database.transactionDao()
        for (row in listOf(pBuy, pSell, pReversal, pSplit, qBuy)) transactions.insertTransaction(row)
        transactions.insertAllocations(listOf(SellAllocationRow("ps", "pb", "10")))
        database.cashEntryDao().insertAll(
            listOf(opening, line(pBuy, -100_000L), line(pSell, 150_000L), line(pReversal, -150_000L), line(qBuy, -50_000L), deposit, undo),
        )
        transactions.insertHidden(HiddenHoldingRow("p", "AAPL"))
        transactions.insertHidden(HiddenHoldingRow("q", "AAPL"))
        val instruments = database.instrumentDao()
        instruments.insertListValue(ListValueRow(list = "SECTOR", value = "Technology"))
        val seq = instruments.loadListValues().single().seq
        instruments.upsertOverride(ClassOverrideRow("p", "AAPL", "SECTOR", seq))
        instruments.upsertOverride(ClassOverrideRow("q", "AAPL", "SECTOR", seq))
        return seq
    }

    private val pTransactions = listOf("pb", "ps", "pr", "px")
    private val pLines = listOf("c-pb", "c-ps", "c-pr")

    private suspend fun deleteP(
        transactionIds: List<String> = pTransactions,
        cashEntryIds: List<String> = pLines,
        hiddenCount: Int = 1,
        overrideCount: Int = 1,
    ) = database.transactionDao().deletePortfolio("p", transactionIds, cashEntryIds, hiddenCount, overrideCount)

    // A snapshot of every table the deletion touches, to compare before and after.
    private suspend fun everything(): List<Any> = listOf(
        database.portfolioDao().loadAll(),
        database.transactionDao().loadAll(),
        database.transactionDao().loadAllocations(),
        database.cashEntryDao().loadAll(),
        database.transactionDao().loadHidden(),
        database.instrumentDao().loadOverrides(),
        database.instrumentDao().loadListValues(),
    )

    // 4.7, decision 1: every row of P goes; Q's, the direct entries and the list value stay.
    @Test
    fun deletingP_removesEveryRowOfP_andKeepsEveryOther() = runBlocking {
        val seq = fill()
        deleteP()
        assertEquals(listOf(PortfolioRow("q", "Portfolio Q", 2L)), database.portfolioDao().loadAll())
        assertEquals(listOf(qBuy), database.transactionDao().loadAll())
        assertTrue(database.transactionDao().loadAllocations().isEmpty())
        assertEquals(listOf("o1", "c-qb", "d1", "u1"), database.cashEntryDao().loadAll().map { it.id })
        assertEquals(listOf(HiddenHoldingRow("q", "AAPL")), database.transactionDao().loadHidden())
        assertEquals(listOf(ClassOverrideRow("q", "AAPL", "SECTOR", seq)), database.instrumentDao().loadOverrides())
        assertEquals(1, database.instrumentDao().loadListValues().size)
    }

    // A count that differs from K1's throws inside the database transaction: nothing is removed (the rollback).
    @Test
    fun aWrongCount_removesNothing() = runBlocking {
        fill()
        val before = everything()
        assertThrowsSomething { deleteP(hiddenCount = 2) }
        assertEquals(before, everything())
    }

    // The review's finding 2: a trade line still naming a deleted transaction fails the deletion; nothing is removed.
    @Test
    fun aTradeLineLeftNamingADeletedTransaction_removesNothing() = runBlocking {
        fill()
        val before = everything()
        assertThrowsSomething { deleteP(cashEntryIds = listOf("c-pb", "c-ps")) }
        assertEquals(before, everything())
    }

    // The review's finding 2: an allocation still naming a deleted buy as its lot fails the deletion; nothing is removed.
    @Test
    fun anAllocationLeftNamingADeletedBuy_removesNothing() = runBlocking {
        fill()
        database.transactionDao().insertAllocations(listOf(SellAllocationRow("qs", "pb", "1")))
        val before = everything()
        assertThrowsSomething { deleteP() }
        assertEquals(before, everything())
    }

    private suspend fun assertThrowsSomething(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            return
        }
        fail("The deletion did not throw")
    }
}
