package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.HistoryChip
import app.ghghorg.atsassets.core.HistoryFilter
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionHistory
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 27 step 4: Transaction history's model (view 8, wireframe 08; CR2; AC-50).
class TransactionHistoryModelTest {
    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        seq: Long,
        quantity: String,
        totalCents: Long,
        quantityRatio: SplitRatio? = null,
    ) = Transaction(id, "p", "AAPL", type, LocalDate.parse(date), seq, BigDecimal(quantity), totalCents, quantityRatio = quantityRatio)

    private fun reversal(
        id: String,
        of: Transaction,
        date: String,
        seq: Long,
    ) = Transaction(id, "p", of.ticker, TransactionType.REVERSAL, LocalDate.parse(date), seq, BigDecimal.ZERO, 0L, reversesId = of.id)

    private fun lineOf(
        transaction: Transaction,
        cents: Long,
    ) = CashEntry("c-${transaction.id}", transaction.date, cents, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)

    // Example A and the sample file's Main and AAPL chain (AC-50).
    private val b1 = trade("b1", TransactionType.BUY, "2026-01-02", 2L, "10", 100_000L)
    private val b2 = trade("b2", TransactionType.BUY, "2026-02-01", 3L, "10", 150_000L)
    private val s1 = trade("s1", TransactionType.SELL, "2026-03-01", 4L, "12", 210_000L)
    private val b3 = trade("b3", TransactionType.BUY, "2026-03-20", 5L, "2", 36_000L)
    private val sp = Transaction("sp", "p", "AAPL", TransactionType.SPLIT, LocalDate.parse("2026-04-01"), 6L, BigDecimal.ZERO, 0L, split = SplitRatio(2, 1))
    private val rv = reversal("rv", b3, "2026-04-05", 7L)
    private val transactions = listOf(b1, b2, s1, b3, sp, rv)
    private val lines =
        listOf(lineOf(b1, -100_000L), lineOf(b2, -150_000L), lineOf(s1, 210_000L), lineOf(b3, -36_000L), lineOf(rv, 36_000L))

    private fun model(
        chip: HistoryChip = HistoryChip.ALL,
        all: List<Transaction> = transactions,
        fromText: String = "",
        toText: String = "",
    ) = TransactionHistories.of(
        rows = TransactionHistory.rows(all, lines, "p", HistoryFilter(chip = chip)),
        portfolioName = "Main",
        fromText = fromText,
        toText = toText,
        ticker = null,
        tickers = listOf("AAPL"),
        chip = chip,
        menuOpen = false,
    )

    // AC-50: a sell and a buy with their quantity, total and cash effect, signed and spoken with their sign.
    @Test
    fun aTradeRow_showsItsQuantityTotalAndCashEffect() {
        val rows = model().rows.filterIsInstance<HistoryRowModel.Trade>()
        val sell = rows.single { it.transactionId == "s1" }
        assertEquals(true, sell.sell)
        assertEquals("2026-03-01", sell.date)
        assertEquals(HistoryShares("12", one = false), sell.shares)
        assertEquals("$2,100.00", sell.total)
        assertEquals("2,100.00", sell.spokenTotal)
        assertEquals(HistoryCashEffect("s1", "+$2,100.00", SpokenFigure("2,100.00", FigureSign.PLUS)), sell.cash)
        val buy = rows.single { it.transactionId == "b2" }
        assertEquals(false, buy.sell)
        assertEquals(HistoryCashEffect("b2", signedAmount(-150_000L), SpokenFigure("1,500.00", FigureSign.MINUS)), buy.cash)
        assertNull(buy.reversedOn)
    }

    // AC-50 and marker 4: the reversal names its original by type and date and shows its quantity as traded and its own
    // cash line; the original shows "Reversed on"; the split shows its ratio and has no cash line.
    @Test
    fun theChain_reversalSplitAndOriginal() {
        val rows = model().rows
        assertEquals(listOf("rv", "sp", "b3", "s1", "b2", "b1"), rows.map { it.key })
        val reversal = rows[0] as HistoryRowModel.Reversal
        assertEquals("b3", reversal.originalId)
        assertEquals(TransactionType.BUY, reversal.originalType)
        assertEquals("2026-03-20", reversal.originalDate)
        assertEquals("AAPL", reversal.ticker)
        assertEquals("2026-04-05", reversal.date)
        assertEquals(HistoryShares("2", one = false), reversal.shares)
        assertEquals("$360.00", reversal.total)
        assertEquals("+$360.00", reversal.cash?.text)
        assertNull(reversal.ratio)
        assertEquals(HistoryRowModel.Split("sp", "AAPL", "2026-04-01", SplitRatio(2, 1), reversedOn = null), rows[1])
        assertEquals(HistoryReversedOn("rv", "2026-04-05"), (rows[2] as HistoryRowModel.Trade).reversedOn)
    }

    // A split's reversal: its ratio, no quantity, no total and no cash line.
    @Test
    fun aSplitsReversal_showsItsRatio() {
        val rs = reversal("rs", sp, "2026-04-02", 8L)
        val row = model(all = transactions + rs).rows.single { it.key == "rs" } as HistoryRowModel.Reversal
        assertEquals(TransactionType.SPLIT, row.originalType)
        assertEquals(SplitRatio(2, 1), row.ratio)
        assertNull(row.shares)
        assertNull(row.total)
        assertNull(row.cash)
    }

    // CR1 and 4.10: a re-entered buy shows its quantity as originally traded (boughtQuantity); "1 share" only for "1".
    @Test
    fun theQuantity_asOriginallyTraded() {
        val reentered = trade("r1", TransactionType.BUY, "2026-02-10", 9L, "5", 50_000L, quantityRatio = SplitRatio(2, 1))
        val one = trade("o1", TransactionType.BUY, "2026-02-11", 10L, "1", 10_000L)
        val rows = model(all = transactions + reentered + one).rows.filterIsInstance<HistoryRowModel.Trade>()
        assertEquals(HistoryShares("10", one = false), rows.single { it.transactionId == "r1" }.shares)
        assertEquals(HistoryShares("1", one = true), rows.single { it.transactionId == "o1" }.shares)
    }

    // AC-50 and marker 5: under Cash, the reversal's line "Reversal of buy AAPL", then the buy's and the sell's lines.
    @Test
    fun theCashChip_namesEachLine() {
        val rows = model(chip = HistoryChip.CASH).rows.map { it as HistoryRowModel.CashLine }
        assertEquals(listOf("c-rv", "c-b3", "c-s1", "c-b2", "c-b1"), rows.map { it.lineId })
        assertEquals(HistoryCashKind.REVERSAL_OF_BUY, rows[0].kind)
        // Marker 7: the reversal's line opens the reversal.
        assertEquals("rv", rows[0].transactionId)
        assertEquals("+$360.00", rows[0].amount)
        assertEquals(HistoryCashKind.BUY, rows[1].kind)
        assertEquals("b3", rows[1].transactionId)
        assertEquals(SpokenFigure("360.00", FigureSign.MINUS), rows[1].spoken)
        assertEquals(HistoryCashKind.SELL, rows[2].kind)
        assertEquals("2026-03-01", rows[2].date)
    }

    // The filters as typed and picked, with the fields' lines (the Cash list's), the portfolio and the menu.
    @Test
    fun theFilters_andTheirLines() {
        val shown = model(chip = HistoryChip.SELL, fromText = "20260230", toText = "2026x")
        assertEquals("Main", shown.portfolioName)
        assertEquals("20260230" to "2026x", shown.fromText to shown.toText)
        assertEquals(DateRefusal.NOT_A_DATE, shown.fromRefusal)
        assertEquals(DateRefusal.NOT_DIGITS, shown.toRefusal)
        assertEquals(HistoryChip.SELL, shown.chip)
        assertEquals(listOf("AAPL"), shown.tickers)
        assertEquals(listOf("s1"), shown.rows.map { it.key })
    }
}
