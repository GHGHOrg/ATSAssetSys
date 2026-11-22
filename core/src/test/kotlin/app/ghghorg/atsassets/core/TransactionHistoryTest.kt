package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 27 step 2: K3's Transaction history (view 8, wireframe 08; AC-10, AC-50).
class TransactionHistoryTest {
    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        seq: Long,
        quantity: String,
        totalCents: Long,
        ticker: String = "AAPL",
        portfolioId: String = "p",
        quantityRatio: SplitRatio? = null,
    ) = Transaction(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        quantityRatio = quantityRatio,
    )

    private fun split(
        id: String,
        date: String,
        seq: Long,
        ticker: String = "AAPL",
    ) = Transaction(id, "p", ticker, TransactionType.SPLIT, LocalDate.parse(date), seq, BigDecimal.ZERO, 0L, split = SplitRatio(2, 1))

    private fun reversal(
        id: String,
        of: Transaction,
        date: String,
        seq: Long,
    ) = Transaction(id, of.portfolioId, of.ticker, TransactionType.REVERSAL, LocalDate.parse(date), seq, BigDecimal.ZERO, 0L, reversesId = of.id)

    // A trade's line: the buy's cash out, the sell's in; a reversal's line gives the original's cash back.
    private fun lineOf(
        transaction: Transaction,
        original: Transaction? = null,
    ): CashEntry {
        val source = original ?: transaction
        val out = if (source.type == TransactionType.BUY) -source.totalCents else source.totalCents
        return CashEntry(
            "c-${transaction.id}",
            transaction.date,
            if (original == null) out else -out,
            CashType.TRADE,
            "",
            transaction.enteredSeq,
            transactionId = transaction.id,
        )
    }

    private val opening = CashEntry("open", LocalDate.parse("2026-01-01"), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)

    // Example A (spec 5) in Portfolio P: two buys and the Mar 1 sell.
    private val b1 = trade("b1", TransactionType.BUY, "2026-01-02", 2L, "10", 100_000L)
    private val b2 = trade("b2", TransactionType.BUY, "2026-02-01", 3L, "10", 150_000L)
    private val s1 = trade("s1", TransactionType.SELL, "2026-03-01", 4L, "12", 210_000L)
    private val exampleA = listOf(b1, b2, s1)
    private val exampleALines = listOf(opening) + exampleA.map { lineOf(it) }

    // The sample file's Main and AAPL (AC-50): the Mar 20 buy, the Apr 1 split, the Apr 5 reversal of the buy.
    private val b3 = trade("b3", TransactionType.BUY, "2026-03-20", 5L, "2", 36_000L)
    private val sp = split("sp", "2026-04-01", 6L)
    private val rv = reversal("rv", b3, "2026-04-05", 7L)
    private val sample = exampleA + listOf(b3, sp, rv)
    private val sampleLines = exampleALines + listOf(lineOf(b3), lineOf(rv, b3))

    private fun all(
        transactions: List<Transaction> = exampleA,
        entries: List<CashEntry> = exampleALines,
        filter: HistoryFilter = HistoryFilter(),
    ) = TransactionHistory.rows(transactions, entries, "p", filter)

    private fun ids(rows: List<HistoryRow>): List<String> = rows.map {
        when (it) {
            is HistoryRow.Entry -> it.transaction.id
            is HistoryRow.Cash -> it.line.id
        }
    }

    // AC-50: Example A with no filter: the sell, the Feb 1 buy and the Jan 2 buy, newest first, each with its cash line.
    @Test
    fun exampleA_newestFirst_eachTradeWithItsCashLine() {
        val rows = all().map { it as HistoryRow.Entry }
        assertEquals(listOf("s1", "b2", "b1"), rows.map { it.transaction.id })
        assertEquals(listOf(210_000L, -150_000L, -100_000L), rows.map { it.cashLine?.amountCents })
        assertEquals(listOf(null, null, null), rows.map { it.reversal })
    }

    // AC-10 and AC-50: the Cash chip shows the three trade lines, never the opening balance or another direct entry.
    @Test
    fun theCashChip_showsOnlyTheTradeLines() {
        val deposit = CashEntry("dep", LocalDate.parse("2026-03-01"), 5_000L, CashType.DEPOSIT, "", 10L)
        val rows = all(entries = exampleALines + deposit, filter = HistoryFilter(chip = HistoryChip.CASH))
        assertEquals(listOf("c-s1", "c-b2", "c-b1"), ids(rows))
        assertEquals(listOf("s1", "b2", "b1"), rows.map { (it as HistoryRow.Cash).transaction.id })
    }

    // AC-50: the sample file's chain: the reversal names the buy, the buy names its reversal, each has its cash line.
    @Test
    fun theSampleChain_isTraceableBothWays() {
        val rows = all(sample, sampleLines, HistoryFilter(ticker = "AAPL")).map { it as HistoryRow.Entry }
        assertEquals(listOf("rv", "sp", "b3", "s1", "b2", "b1"), rows.map { it.transaction.id })
        val reversalRow = rows[0]
        assertEquals("b3", reversalRow.original?.id)
        assertEquals(TransactionType.BUY, reversalRow.kind)
        assertEquals(36_000L, reversalRow.cashLine?.amountCents)
        val buyRow = rows[2]
        assertEquals("rv", buyRow.reversal?.id)
        assertEquals(-36_000L, buyRow.cashLine?.amountCents)
        assertNull(rows[1].cashLine)
    }

    // AC-50: the Cash chip with Main and AAPL: the reversal's line, the Mar 20 buy's and the Mar 1 sell's, newest first.
    @Test
    fun theCashChip_withTheSampleChain_listsTheReversalsLine() {
        val rows = all(sample, sampleLines, HistoryFilter(ticker = "AAPL", chip = HistoryChip.CASH)).map { it as HistoryRow.Cash }
        assertEquals(listOf("c-rv", "c-b3", "c-s1", "c-b2", "c-b1"), rows.map { it.line.id })
        assertEquals("b3", rows[0].original?.id)
        assertEquals(listOf(36_000L, -36_000L, 210_000L), rows.take(3).map { it.line.amountCents })
    }

    // View 8: a reversal sits under the chip of the entry it reverses: a buy's under Buy, a split's under Split.
    @Test
    fun aReversalSitsUnderItsOriginalsChip() {
        val spReversal = reversal("rs", sp, "2026-04-02", 8L)
        val transactions = sample + spReversal
        val lines = sampleLines
        assertEquals(listOf("rv", "b3", "b2", "b1"), ids(all(transactions, lines, HistoryFilter(chip = HistoryChip.BUY))))
        assertEquals(listOf("s1"), ids(all(transactions, lines, HistoryFilter(chip = HistoryChip.SELL))))
        assertEquals(listOf("rs", "sp"), ids(all(transactions, lines, HistoryFilter(chip = HistoryChip.SPLIT))))
    }

    // A sell's reversal under Sell, with its line giving the proceeds back; the sell shows its reversal.
    @Test
    fun aSellsReversal_underSell_withItsLine() {
        val rs = reversal("rs", s1, "2026-03-05", 8L)
        val rows = all(exampleA + rs, exampleALines + lineOf(rs, s1), HistoryFilter(chip = HistoryChip.SELL)).map { it as HistoryRow.Entry }
        assertEquals(listOf("rs", "s1"), rows.map { it.transaction.id })
        assertEquals(-210_000L, rows[0].cashLine?.amountCents)
        assertEquals("rs", rows[1].reversal?.id)
    }

    // AC-50: Apr 1 to Apr 30 on Example A matches nothing; Mar 1 to Mar 1 the sell alone; a ticker and a range combine.
    @Test
    fun theRangeAndTheTicker_combine() {
        val april = HistoryFilter(from = LocalDate.parse("2026-04-01"), to = LocalDate.parse("2026-04-30"))
        assertEquals(emptyList<String>(), ids(all(filter = april)))
        val march1 = HistoryFilter(from = LocalDate.parse("2026-03-01"), to = LocalDate.parse("2026-03-01"))
        assertEquals(listOf("s1"), ids(all(filter = march1)))
        val msft = trade("m1", TransactionType.BUY, "2026-02-01", 9L, "1", 30_000L, ticker = "MSFT")
        val from = HistoryFilter(ticker = "MSFT", from = LocalDate.parse("2026-02-01"))
        assertEquals(listOf("m1"), ids(all(exampleA + msft, exampleALines + lineOf(msft), from)))
        assertEquals(listOf("b2", "b1"), ids(all(exampleA + msft, exampleALines, HistoryFilter(ticker = "AAPL", to = LocalDate.parse("2026-02-01")))))
    }

    // View 8: one portfolio only; another portfolio's entries and lines never show.
    @Test
    fun anotherPortfoliosEntries_neverShow() {
        val other = trade("o1", TransactionType.BUY, "2026-02-15", 9L, "3", 9_000L, portfolioId = "q")
        val rows = all(exampleA + other, exampleALines + lineOf(other))
        assertEquals(listOf("s1", "b2", "b1"), ids(rows))
        assertEquals(listOf("c-s1", "c-b2", "c-b1"), ids(all(exampleA + other, exampleALines + lineOf(other), HistoryFilter(chip = HistoryChip.CASH))))
    }

    // 4.1: on a shared date the later entry comes first, whatever the order of the list.
    @Test
    fun onASharedDate_theLaterEntryComesFirst() {
        val early = trade("e1", TransactionType.BUY, "2026-02-01", 20L, "1", 1_000L)
        val late = trade("e2", TransactionType.BUY, "2026-02-01", 21L, "1", 1_000L)
        assertEquals(listOf("s1", "e2", "e1", "b2", "b1"), ids(all(listOf(late) + exampleA + early, exampleALines)))
    }

    // CR1: a re-entered buy with a quantity ratio keeps it on the row's transaction, for its quantity as traded.
    @Test
    fun aReenteredBuy_keepsItsQuantityRatio() {
        val ratio = SplitRatio(2, 1)
        val reentered = trade("r1", TransactionType.BUY, "2026-02-10", 9L, "5", 50_000L, quantityRatio = ratio)
        val row = all(exampleA + reentered, exampleALines).map { it as HistoryRow.Entry }.single { it.transaction.id == "r1" }
        assertEquals(Fraction.of(BigDecimal("10")), row.transaction.boughtQuantity)
    }

    // The ticker picker: this portfolio's tickers only, each once, in alphabetical order.
    @Test
    fun theTickers_ofThisPortfolio_inOrder() {
        val msft = trade("m1", TransactionType.BUY, "2026-02-01", 9L, "1", 30_000L, ticker = "MSFT")
        val amzn = trade("z1", TransactionType.BUY, "2026-02-02", 10L, "1", 30_000L, ticker = "AMZN")
        val other = trade("o1", TransactionType.BUY, "2026-02-15", 11L, "3", 9_000L, ticker = "TSLA", portfolioId = "q")
        assertEquals(listOf("AAPL", "AMZN", "MSFT"), TransactionHistory.tickers(listOf(msft) + exampleA + amzn + other, "p"))
        assertEquals(emptyList<String>(), TransactionHistory.tickers(exampleA, "q"))
    }
}
