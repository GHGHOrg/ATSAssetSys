package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.TradeSide
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 15d (CR2): the Cash list as a model, worked out without drawing the screen.
class CashListModelTest {
    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)

    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        seq: Long,
        totalCents: Long,
        reversesId: String? = null,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = if (type == TransactionType.REVERSAL) BigDecimal.ZERO else BigDecimal("10"),
        totalCents = totalCents,
        reversesId = reversesId,
        allocations = if (type == TransactionType.SELL) listOf(SellAllocation("b1", Fraction.of(BigDecimal("10")))) else emptyList(),
    )

    private fun line(
        transaction: Transaction,
        cents: Long,
    ): CashEntry = CashEntry("c-${transaction.id}", transaction.date, cents, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)

    private fun direct(
        id: String,
        date: String,
        cents: Long,
        seq: Long,
        type: CashType,
        note: String = "",
    ): CashEntry = CashEntry(id, LocalDate.parse(date), cents, type, note, seq)

    // Example A (AC-49): the opening balance, the Jan 2 and Feb 1 buys, the Mar 1 sell.
    private val b1 = trade("b1", TransactionType.BUY, "2026-01-02", 2L, 100_000L)
    private val b2 = trade("b2", TransactionType.BUY, "2026-02-01", 3L, 150_000L)
    private val s1 = trade("s1", TransactionType.SELL, "2026-03-01", 4L, 210_000L)
    private val opening = direct("o1", "2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE)
    private val transactions = listOf(b1, b2, s1)
    private val entries = listOf(opening, line(b1, -100_000L), line(b2, -150_000L), line(s1, 210_000L))

    private fun model(
        form: CashFilterForm = CashFilterForm(),
        cash: List<CashEntry> = entries,
        trades: List<Transaction> = transactions,
    ): CashListModel = CashList.of(cash, trades, listOf(main), form)

    @Test
    fun exampleA_rowsNewestFirst_eachWithItsSignedAmountAndTheBalanceAfterIt() {
        val model = model()
        assertEquals("$9,600.00", model.balance)
        assertEquals(SpokenFigure("9,600.00", FigureSign.NONE), model.spokenBalance)
        assertTrue(model.hasEntries)
        assertFalse(model.filterActive)
        assertEquals(listOf("c-s1", "c-b2", "c-b1", "o1"), model.rows.map { it.key })
        assertEquals(listOf("+$2,100.00", "−$1,500.00", "−$1,000.00", "+$10,000.00"), model.rows.map { it.amount })
        assertEquals(listOf("$9,600.00", "$7,500.00", "$9,000.00", "$10,000.00"), model.rows.map { it.balanceAfter })
        assertEquals(listOf("2026-03-01", "2026-02-01", "2026-01-02", "2026-01-01"), model.rows.map { it.date })
    }

    @Test
    fun aTradeLine_namesItsKindTickerAndPortfolio_andOpensItsTransaction() {
        val sell = model().rows.first()
        assertEquals(
            CashRowModel.Trade(
                key = "c-s1",
                date = "2026-03-01",
                amount = "+$2,100.00",
                balanceAfter = "$9,600.00",
                spokenAmount = SpokenFigure("2,100.00", FigureSign.PLUS),
                spokenBalanceAfter = SpokenFigure("9,600.00", FigureSign.NONE),
                transactionId = "s1",
                kind = TransactionType.SELL,
                title = TradeTitle.SELL,
                ticker = "AAPL",
                portfolio = "Main",
            ),
            sell,
        )
        val buy = model().rows[1] as CashRowModel.Trade
        assertEquals(TradeTitle.BUY, buy.title)
        assertEquals(SpokenFigure("1,500.00", FigureSign.MINUS), buy.spokenAmount)
    }

    @Test
    fun aDirectEntry_showsItsTypeAndNote_andOpensItsDetail() {
        val deposit = direct("d1", "2026-01-05", 20_000L, 5L, CashType.DEPOSIT, note = "Salary")
        val row = model(cash = entries + deposit).rows.first { it.key == "d1" }
        assertEquals(
            CashRowModel.Entry(
                key = "d1",
                date = "2026-01-05",
                amount = "+$200.00",
                balanceAfter = "$9,200.00",
                spokenAmount = SpokenFigure("200.00", FigureSign.PLUS),
                spokenBalanceAfter = SpokenFigure("9,200.00", FigureSign.NONE),
                type = CashType.DEPOSIT,
                note = "Salary",
            ),
            row,
        )
    }

    @Test
    fun aTradeLineWhoseTransactionIsNotThere_showsAsADirectEntry_asBefore() {
        val row = model(trades = listOf(b1, b2)).rows.first()
        assertTrue(row is CashRowModel.Entry)
        assertEquals(CashType.TRADE, (row as CashRowModel.Entry).type)
    }

    @Test
    fun aReversal_isTitledAfterItsOriginal_andItsTradeLineReadsIt() {
        val r1 = trade("r1", TransactionType.REVERSAL, "2026-03-02", 5L, 0L, reversesId = "s1")
        val r2 = trade("r2", TransactionType.REVERSAL, "2026-03-03", 6L, 0L, reversesId = "b2")
        val rows = model(cash = entries + line(r1, -210_000L) + line(r2, 150_000L), trades = transactions + r1 + r2).rows
        val ofBuy = rows[0] as CashRowModel.Trade
        val ofSell = rows[1] as CashRowModel.Trade
        assertEquals(TradeTitle.REVERSAL_OF_BUY, ofBuy.title)
        assertEquals(TransactionType.REVERSAL, ofBuy.kind)
        assertEquals(TradeTitle.REVERSAL_OF_SELL, ofSell.title)
        assertEquals("$9,000.00", ofBuy.balanceAfter)
    }

    @Test
    fun onASharedDate_theLaterEntryComesFirst() {
        val first = direct("d1", "2026-03-01", 5_000L, 5L, CashType.DEPOSIT)
        val second = direct("w1", "2026-03-01", -2_000L, 6L, CashType.WITHDRAWAL)
        val rows = model(cash = entries + first + second).rows
        assertEquals(listOf("w1", "d1", "c-s1"), rows.take(3).map { it.key })
        assertEquals(listOf("$9,630.00", "$9,650.00", "$9,600.00"), rows.take(3).map { it.balanceAfter })
    }

    @Test
    fun aDateRange_showsOnlyItsRows_withTheRealBalances_andCountsThem() {
        val model = model(CashFilterForm(fromText = "20260115", toText = "20260215"))
        assertTrue(model.filterActive)
        assertEquals(listOf("c-b2"), model.rows.map { it.key })
        assertEquals(listOf("$7,500.00"), model.rows.map { it.balanceAfter })
        assertEquals("$9,600.00", model.balance)
    }

    @Test
    fun aTypeChip_showsOnlyThatType_withTheRealBalances() {
        val deposit = direct("d1", "2026-01-05", 20_000L, 5L, CashType.DEPOSIT)
        val model = model(CashFilterForm(type = CashType.DEPOSIT), cash = entries + deposit)
        assertTrue(model.filterActive)
        assertEquals(listOf("d1"), model.rows.map { it.key })
        assertEquals(listOf("$9,200.00"), model.rows.map { it.balanceAfter })
    }

    @Test
    fun noEntries_hasNoRowsAndNoFilters() {
        val model = model(cash = emptyList(), trades = emptyList())
        assertFalse(model.hasEntries)
        assertEquals(emptyList<CashRowModel>(), model.rows)
        assertEquals("$0.00", model.balance)
    }

    @Test
    fun noMatch_keepsTheFilters_andHasNoRows() {
        val model = model(CashFilterForm(fromText = "20260401", toText = "20260430"))
        assertTrue(model.hasEntries)
        assertTrue(model.filterActive)
        assertEquals(emptyList<CashRowModel>(), model.rows)
    }

    @Test
    fun theDateFields_refuseAnythingButDigits_andEightDigitsThatAreNoDate_andIgnoreAPartialDate() {
        val refused = model(CashFilterForm(fromText = "2026-", toText = "20261340"))
        assertEquals(DateRefusal.NOT_DIGITS, refused.fromRefusal)
        assertEquals(DateRefusal.NOT_A_DATE, refused.toRefusal)
        assertFalse(refused.filterActive)
        assertEquals(4, refused.rows.size)
        val partial = model(CashFilterForm(fromText = "2026"))
        assertNull(partial.fromRefusal)
        assertFalse(partial.filterActive)
    }

    @Test
    fun aNegativeBalance_isReadWithMinus() {
        val overdrawn = listOf(direct("a1", "2026-01-01", -5_000L, 1L, CashType.ADJUSTMENT))
        val model = model(cash = overdrawn, trades = emptyList())
        assertEquals("−$50.00", model.balance)
        assertEquals(SpokenFigure("50.00", FigureSign.MINUS), model.spokenBalance)
        assertEquals(SpokenFigure("50.00", FigureSign.MINUS), model.rows.single().spokenBalanceAfter)
    }

    // 28 step 4: the ticker, the Buy and Sell chips and the direct chips' state (view 7, wireframe 07; AC-49).

    // The sample file's Mar 20 buy of AAPL and its Apr 5 reversal, after Example A.
    private val b3 = trade("b3", TransactionType.BUY, "2026-03-20", 5L, 36_000L)
    private val rv = trade("rv", TransactionType.REVERSAL, "2026-04-05", 6L, 0L, reversesId = "b3")
    private val deposit = direct("d1", "2026-03-10", 5_000L, 7L, CashType.DEPOSIT)
    private val withChain = entries + listOf(line(b3, -36_000L), line(rv, 36_000L), deposit)
    private val chainTrades = transactions + listOf(b3, rv)

    @Test
    fun noTicker_offersEveryTickerWithATradeLine_andEnablesTheDirectChips() {
        val model = model(cash = withChain, trades = chainTrades)
        assertEquals(listOf("AAPL"), model.tickers)
        assertNull(model.ticker)
        assertTrue(model.directChipsEnabled)
    }

    // AC-49: ticker AAPL with All: the trade lines only, no opening balance or deposit, the real balances; the direct
    // chips disabled; the ticker counts as a filter.
    @Test
    fun aTicker_showsOnlyItsTradeLines_disablesTheDirectChips_andCountsAsAFilter() {
        val model = model(CashFilterForm(ticker = "AAPL"), withChain, chainTrades)
        assertEquals("AAPL", model.ticker)
        assertFalse(model.directChipsEnabled)
        assertTrue(model.filterActive)
        assertEquals(listOf("c-rv", "c-b3", "c-s1", "c-b2", "c-b1"), model.rows.map { it.key })
        assertEquals(listOf("$9,650.00", "$9,290.00", "$9,600.00", "$7,500.00", "$9,000.00"), model.rows.map { it.balanceAfter })
    }

    // AC-49 and wireframe 07 frame D: Buy shows the reversal titled "Reversal of buy" with the buys; Sell only the sell.
    @Test
    fun buyAndSell_takeTheTradeLines_theReversalUnderBuy() {
        val buys = model(CashFilterForm(ticker = "AAPL", trade = TradeSide.BUY), withChain, chainTrades)
        assertEquals(listOf("c-rv", "c-b3", "c-b2", "c-b1"), buys.rows.map { it.key })
        assertEquals(TradeTitle.REVERSAL_OF_BUY, (buys.rows.first() as CashRowModel.Trade).title)
        assertEquals("+$360.00", buys.rows.first().amount)
        val sells = model(CashFilterForm(trade = TradeSide.SELL), withChain, chainTrades)
        assertEquals(listOf("c-s1"), sells.rows.map { it.key })
        assertTrue(sells.directChipsEnabled)
        assertTrue(sells.filterActive)
    }

    // AC-49: ticker AAPL with Apr 1 to Apr 30 on Example A: no row.
    @Test
    fun aTickerAndARangeWithNoTrade_haveNoRows() {
        val model = model(CashFilterForm(fromText = "20260401", toText = "20260430", ticker = "AAPL"))
        assertTrue(model.hasEntries)
        assertEquals(emptyList<CashRowModel>(), model.rows)
    }
}
