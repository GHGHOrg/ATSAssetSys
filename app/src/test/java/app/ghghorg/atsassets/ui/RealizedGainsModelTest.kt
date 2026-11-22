package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.RealizedGains
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 26 step 4: Realized gains' model (view 5, wireframe 05; 4.10; AC-11, AC-23, AC-47; decision 3).
class RealizedGainsModelTest {
    private val p = Portfolio("p1", "Portfolio P", 1L)

    private fun bought(
        id: String,
        ticker: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
    ) = Transaction(id, "p1", ticker, TransactionType.BUY, LocalDate.parse(date), seq, BigDecimal(quantity), totalCents)

    private fun sold(
        id: String,
        ticker: String,
        date: String,
        totalCents: Long,
        seq: Long,
        vararg picks: Pair<String, String>,
    ) = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.SELL,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + BigDecimal(pick.second) },
        totalCents = totalCents,
        allocations = picks.map { SellAllocation(it.first, Fraction.of(BigDecimal(it.second))) },
    )

    // The sample file's sales: AAPL Mar 1 (+$800, 12 for $2,100, lots $1,300) and OLDCO Mar 10 ($0 for $200 of lots).
    private val sample =
        listOf(
            bought("a1", "AAPL", "2026-01-02", "10", 100_000L, 2L),
            bought("a2", "AAPL", "2026-02-01", "10", 150_000L, 3L),
            sold("as", "AAPL", "2026-03-01", 210_000L, 4L, "a1" to "10", "a2" to "2"),
            bought("o1", "OLDCO", "2026-01-03", "5", 20_000L, 5L),
            sold("os", "OLDCO", "2026-03-10", 0L, 6L, "o1" to "5"),
        )

    private fun model(
        transactions: List<Transaction> = sample,
        fromText: String = "",
        toText: String = "",
        expanded: Set<Pair<String, String>> = emptySet(),
    ): RealizedGainsModel {
        val report = RealizedGains.report(transactions, listOf(p), DateDigits.toDate(fromText), DateDigits.toDate(toText))
        return RealizedGainsScreens.of(report, listOf(p), fromText, toText, expanded)
    }

    // AC-47: AAPL +$800.00 green, OLDCO −$200.00 red, the total +$600.00 from two sales on all dates.
    @Test
    fun theSampleFile_totalRowsAndCount() {
        val shown = model()
        assertEquals("+$600.00", shown.total.text)
        assertEquals(1, shown.total.signum)
        assertEquals(SpokenFigure("600.00", FigureSign.PLUS), shown.total.spoken)
        assertEquals(GainsCountLine.AllDates(2), shown.countLine)
        assertEquals(listOf("AAPL", "OLDCO"), shown.holdings.map { it.ticker })
        val aapl = shown.holdings[0]
        assertEquals("Portfolio P", aapl.portfolioName)
        assertEquals(1, aapl.count)
        assertEquals("+$800.00" to 1, aapl.total.text to aapl.total.signum)
        assertEquals("−$200.00" to -1, shown.holdings[1].total.text to shown.holdings[1].total.signum)
        assertEquals(SpokenFigure("200.00", FigureSign.MINUS), shown.holdings[1].total.spoken)
        assertEquals(false, aapl.expanded)
        assertEquals(emptyList<RealizedSaleModel>(), aapl.sales)
    }

    // AC-47 and wireframe 05 frame B: the expanded AAPL's sale, its date, gain, quantity, proceeds and cost of lots used.
    @Test
    fun anExpandedHolding_showsItsSale() {
        val aapl = model(expanded = setOf("p1" to "AAPL")).holdings[0]
        assertEquals(true, aapl.expanded)
        assertEquals(
            listOf(RealizedSaleModel("as", "2026-03-01", GainFigure.ofCents(80_000L), "12", "$2,100.00", "2,100.00", "$1,300.00", "1,300.00")),
            aapl.sales,
        )
    }

    // AC-23 and AC-47: Mar 1 to Mar 1, one sale; Apr 1 to Apr 30, $0.00 uncolored and no holding (the plain line).
    @Test
    fun theRangeCountLines_andTheEmptyRange() {
        val one = model(fromText = "20260301", toText = "20260301")
        assertEquals("+$800.00", one.total.text)
        assertEquals(GainsCountLine.Between(1, "2026-03-01", "2026-03-01"), one.countLine)
        val none = model(fromText = "20260401", toText = "20260430")
        assertEquals("$0.00" to 0, none.total.text to none.total.signum)
        assertEquals(GainsCountLine.Between(0, "2026-04-01", "2026-04-30"), none.countLine)
        assertEquals(emptyList<RealizedHoldingModel>(), none.holdings)
        assertEquals(GainsCountLine.From(1, "2026-03-05"), model(fromText = "20260305").countLine)
        assertEquals(GainsCountLine.UpTo(1, "2026-03-05"), model(toText = "20260305").countLine)
    }

    // Decision 3: a field that is not a date shows its line and counts as open; half typed digits show no line yet.
    @Test
    fun aFieldThatIsNotADate_hasItsLine_andCountsAsOpen() {
        val notReal = model(fromText = "20260230", toText = "20260305")
        assertEquals(DateRefusal.NOT_A_DATE, notReal.fromRefusal)
        assertEquals(null, notReal.toRefusal)
        assertEquals(GainsCountLine.UpTo(1, "2026-03-05"), notReal.countLine)
        val letters = model(toText = "2026x")
        assertEquals(DateRefusal.NOT_DIGITS, letters.toRefusal)
        assertEquals(GainsCountLine.AllDates(2), letters.countLine)
        val half = model(fromText = "202603")
        assertEquals(null, half.fromRefusal)
        assertEquals(GainsCountLine.AllDates(2), half.countLine)
        assertEquals("20260230" to "20260305", notReal.fromText to notReal.toText)
    }

    // 4.10: a holding's sales by largest remainder add up to its shown total; each newest first, ties to the earlier row.
    @Test
    fun theSalesUnderAHolding_addUpToItsTotal_roundingDownAndUp() {
        // 3 shares for $1.00: two sales of 1 for $0.01 lose 1/3 - 1/100 each; the total, -0.6466..., shows -$0.65.
        val losses =
            listOf(
                bought("b", "TRI", "2026-01-02", "3", 100L, 2L),
                sold("s1", "TRI", "2026-02-01", 1L, 3L, "b" to "1"),
                sold("s2", "TRI", "2026-02-02", 1L, 4L, "b" to "1"),
            )
        val down = model(losses, expanded = setOf("p1" to "TRI")).holdings.single()
        assertEquals("−$0.65", down.total.text)
        assertEquals(listOf("s2" to "−$0.32", "s1" to "−$0.33"), down.sales.map { it.sellId to it.gain.text })
        // Two sales of 1 for $0.67 gain 0.67 - 1/3 each; the total, 0.6733..., shows +$0.67.
        val gains =
            listOf(
                bought("b", "TRI", "2026-01-02", "3", 100L, 2L),
                sold("s1", "TRI", "2026-02-01", 67L, 3L, "b" to "1"),
                sold("s2", "TRI", "2026-02-02", 67L, 4L, "b" to "1"),
            )
        val up = model(gains, expanded = setOf("p1" to "TRI")).holdings.single()
        assertEquals("+$0.67", up.total.text)
        assertEquals(listOf("s2" to "+$0.34", "s1" to "+$0.33"), up.sales.map { it.sellId to it.gain.text })
        // Each sale's cost is shown alone, rounded half away from zero: 1/3 of $1.00 is $0.33.
        assertEquals(listOf("$0.33", "$0.33"), up.sales.map { it.cost })
    }

    // Section 6: a zero gain shows $0.00 with no sign and no color.
    @Test
    fun aZeroGain_hasNoSignAndNoColor() {
        val even =
            listOf(
                bought("b", "ZED", "2026-01-02", "2", 20_000L, 2L),
                sold("s", "ZED", "2026-02-01", 10_000L, 3L, "b" to "1"),
            )
        val holding = model(even, expanded = setOf("p1" to "ZED")).holdings.single()
        assertEquals("$0.00" to 0, holding.total.text to holding.total.signum)
        assertEquals(SpokenFigure("0.00", FigureSign.NONE), holding.sales.single().gain.spoken)
    }
}
