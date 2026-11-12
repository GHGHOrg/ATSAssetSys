package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

// 26 step 2: K3's realized gains (view 5, AC-11, AC-23, AC-47; 4.4, 4.6, 4.10).
class RealizedGainsTest {
    private val p = Portfolio("p1", "Portfolio P", 1L)
    private val q = Portfolio("p2", "Second", 2L)
    private val portfolios = listOf(p, q)

    private fun day(text: String): LocalDate = LocalDate.parse(text)

    private fun buy(
        id: String,
        ticker: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        portfolioId: String = "p1",
    ) = Transaction(id, portfolioId, ticker, TransactionType.BUY, day(date), seq, BigDecimal(quantity), totalCents)

    private fun sell(
        id: String,
        ticker: String,
        date: String,
        totalCents: Long,
        seq: Long,
        vararg picks: Pair<String, String>,
        portfolioId: String = "p1",
    ) = Transaction(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        type = TransactionType.SELL,
        date = day(date),
        enteredSeq = seq,
        quantity = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + BigDecimal(pick.second) },
        totalCents = totalCents,
        allocations = picks.map { SellAllocation(it.first, Fraction.of(BigDecimal(it.second))) },
    )

    private fun reversal(
        id: String,
        original: Transaction,
        date: String,
        seq: Long,
    ) = Transaction(id, original.portfolioId, original.ticker, TransactionType.REVERSAL, day(date), seq, BigDecimal.ZERO, 0L, reversesId = original.id)

    private fun split(
        id: String,
        ticker: String,
        date: String,
        to: Int,
        from: Int,
        seq: Long,
    ) = Transaction(id, "p1", ticker, TransactionType.SPLIT, day(date), seq, BigDecimal.ZERO, 0L, split = SplitRatio(to, from))

    private fun dollars(cents: Long): Fraction = Fraction.ofCents(cents)

    // Example A: lot 1 (10 at $1,000), lot 2 (10 at $1,500); Mar 1 sell 12 for $2,100 from lot 1 x 10 and lot 2 x 2.
    private val a1 = buy("a1", "AAPL", "2026-01-02", "10", 100_000L, 2L)
    private val a2 = buy("a2", "AAPL", "2026-02-01", "10", 150_000L, 3L)
    private val aSell = sell("as", "AAPL", "2026-03-01", 210_000L, 4L, "a1" to "10", "a2" to "2")
    private val exampleA = listOf(a1, a2, aSell)

    // The sample file adds OLDCO (hidden in the app): bought for $200, sold Mar 10 for $0; and a Mar 20 AAPL buy reversed Apr 5.
    private val o1 = buy("o1", "OLDCO", "2026-01-03", "5", 20_000L, 5L)
    private val oSell = sell("os", "OLDCO", "2026-03-10", 0L, 6L, "o1" to "5")
    private val a3 = buy("a3", "AAPL", "2026-03-20", "2", 36_000L, 7L)
    private val sample = exampleA + listOf(o1, oSell, a3, reversal("r3", a3, "2026-04-05", 8L))

    @Test
    fun exampleA_theSaleGains800_perSaleHoldingAndTotal() {
        val report = RealizedGains.report(exampleA, portfolios, null, null)
        assertEquals(1, report.holdings.size)
        val holding = report.holdings.single()
        assertEquals("p1" to "AAPL", holding.portfolioId to holding.ticker)
        val sale = holding.sales.single()
        assertEquals(aSell, sale.sell)
        assertEquals(dollars(210_000L), sale.proceeds)
        assertEquals(dollars(130_000L), sale.cost)
        assertEquals(dollars(80_000L), sale.gain)
        assertEquals(dollars(80_000L), holding.total)
        assertEquals(dollars(80_000L), report.total)
        assertEquals(1, report.count)
    }

    @Test
    fun theSampleFile_aaplThenOldco_totalIs600() {
        val report = RealizedGains.report(sample, portfolios, null, null)
        assertEquals(listOf("AAPL", "OLDCO"), report.holdings.map { it.ticker })
        assertEquals(dollars(80_000L), report.holdings[0].total)
        assertEquals(dollars(-20_000L), report.holdings[1].total)
        assertEquals(dollars(60_000L), report.total)
        assertEquals(2, report.count)
    }

    @Test
    fun aRangeOfMar1ToMar1_showsTheOneSale() {
        val report = RealizedGains.report(sample, portfolios, day("2026-03-01"), day("2026-03-01"))
        assertEquals(dollars(80_000L), report.total)
        assertEquals(1, report.count)
        assertEquals(listOf("AAPL"), report.holdings.map { it.ticker })
    }

    @Test
    fun aRangeContainingMar1_showsItsGain_andEachOpenEndWorks() {
        assertEquals(dollars(80_000L), RealizedGains.report(exampleA, portfolios, day("2026-02-15"), day("2026-03-05")).total)
        assertEquals(dollars(80_000L), RealizedGains.report(exampleA, portfolios, null, day("2026-03-01")).total)
        assertEquals(dollars(80_000L), RealizedGains.report(exampleA, portfolios, day("2026-03-01"), null).total)
        assertEquals(0, RealizedGains.report(exampleA, portfolios, null, day("2026-02-28")).count)
        assertEquals(0, RealizedGains.report(exampleA, portfolios, day("2026-03-02"), null).count)
    }

    @Test
    fun aprilHasNoSale_theTotalIs0() {
        val report = RealizedGains.report(sample, portfolios, day("2026-04-01"), day("2026-04-30"))
        assertTrue(report.holdings.isEmpty())
        assertEquals(Fraction.ZERO, report.total)
        assertEquals(0, report.count)
    }

    @Test
    fun fromAfterTo_listsNothing() {
        assertEquals(0, RealizedGains.report(sample, portfolios, day("2026-03-10"), day("2026-03-01")).count)
    }

    @Test
    fun aReversedSell_isLeftOutWhateverTheRange() {
        val history = exampleA + reversal("rs", aSell, "2026-05-01", 9L)
        assertTrue(RealizedGains.report(history, portfolios, null, null).holdings.isEmpty())
        assertTrue(RealizedGains.report(history, portfolios, day("2026-03-01"), day("2026-03-01")).holdings.isEmpty())
        assertTrue(RealizedGains.report(history, portfolios, day("2026-05-01"), null).holdings.isEmpty())
    }

    @Test
    fun aReenteredSell_countsOnItsOwnDate_withItsOwnGain() {
        // Reverse and re-enter (14a): the original reversed on its date, the new sell (12 for $2,400) entered after it.
        val again = sell("as2", "AAPL", "2026-03-15", 240_000L, 10L, "a1" to "10", "a2" to "2")
        val history = exampleA + listOf(reversal("rs", aSell, "2026-03-01", 9L), again)
        val report = RealizedGains.report(history, portfolios, null, null)
        assertEquals(listOf(again), report.holdings.single().sales.map { it.sell })
        assertEquals(dollars(110_000L), report.total)
        assertEquals(0, RealizedGains.report(history, portfolios, day("2026-03-01"), day("2026-03-01")).count)
    }

    @Test
    fun aSplitBeforeTheSale_setsTheCostPerShare_andOneAfterChangesNothing() {
        // 10 for $1,000, a 2 for 1 split, then 10 of the 20 sold for $600: cost $500, gain $100; a later split changes nothing.
        val b = buy("b", "XYZ", "2026-01-02", "10", 100_000L, 2L)
        val history = listOf(b, split("x1", "XYZ", "2026-02-01", 2, 1, 3L), sell("s", "XYZ", "2026-03-01", 60_000L, 4L, "b" to "10"))
        val before = RealizedGains.report(history, portfolios, null, null).holdings.single().sales.single()
        assertEquals(dollars(50_000L), before.cost)
        assertEquals(dollars(10_000L), before.gain)
        val after = RealizedGains.report(history + split("x2", "XYZ", "2026-04-01", 3, 1, 5L), portfolios, null, null)
        assertEquals(before, after.holdings.single().sales.single())
    }

    @Test
    fun holdingsInTickerOrder_thenPortfolioOrder_salesNewestFirst() {
        val m = buy("m", "MSFT", "2026-01-02", "4", 40_000L, 2L)
        val k = buy("k", "AAPL", "2026-01-02", "4", 40_000L, 3L, portfolioId = "p2")
        val j = buy("j", "AAPL", "2026-01-02", "4", 40_000L, 4L)
        val sMsft = sell("sm", "MSFT", "2026-03-01", 50_000L, 5L, "m" to "4")
        val sOther = sell("sk", "AAPL", "2026-03-01", 50_000L, 6L, "k" to "4", portfolioId = "p2")
        val early = sell("sj1", "AAPL", "2026-02-01", 12_000L, 7L, "j" to "1")
        val sameDayFirst = sell("sj2", "AAPL", "2026-03-01", 12_000L, 8L, "j" to "1")
        val sameDayLater = sell("sj3", "AAPL", "2026-03-01", 12_000L, 9L, "j" to "1")
        // Entered out of date order: the list follows the dates, then the entry order.
        val history = listOf(m, k, j, sMsft, sOther, sameDayFirst, sameDayLater, early)
        val report = RealizedGains.report(history, portfolios, null, null)
        assertEquals(listOf("p1" to "AAPL", "p2" to "AAPL", "p1" to "MSFT"), report.holdings.map { it.portfolioId to it.ticker })
        assertEquals(listOf("sj3", "sj2", "sj1"), report.holdings[0].sales.map { it.sell.id })
        assertEquals(5, report.count)
    }

    @Test
    fun aZeroGainAndALoss_areKeptAsTheyAre() {
        val b = buy("b", "ZED", "2026-01-02", "2", 20_000L, 2L)
        val history = listOf(b, sell("s1", "ZED", "2026-02-01", 10_000L, 3L, "b" to "1"), sell("s2", "ZED", "2026-03-01", 4_000L, 4L, "b" to "1"))
        val sales = RealizedGains.report(history, portfolios, null, null).holdings.single().sales
        assertEquals(listOf(dollars(-6_000L), Fraction.ZERO), sales.map { it.gain })
        assertEquals(dollars(-6_000L), RealizedGains.report(history, portfolios, null, null).total)
    }

    @Test
    fun thirdsOfACent_stayExact() {
        // 3 shares for $1.00: each sold alone for $0.01, a gain of 1/100 - 1/3 dollars, kept exact.
        val b = buy("b", "TRI", "2026-01-02", "3", 100L, 2L)
        val history = listOf(b, sell("s1", "TRI", "2026-02-01", 1L, 3L, "b" to "1"), sell("s2", "TRI", "2026-02-02", 1L, 4L, "b" to "1"))
        val third = Fraction.of(BigInteger.ONE, BigInteger.valueOf(3))
        val holding = RealizedGains.report(history, portfolios, null, null).holdings.single()
        assertEquals(listOf(dollars(1L) - third, dollars(1L) - third), holding.sales.map { it.gain })
        assertEquals(dollars(2L) - third - third, holding.total)
    }

    @Test
    fun aSellOfAPortfolioNotInTheList_comesAfterTheListedOnes() {
        // My reading: a portfolio id not in [portfolios] (none in the app; a deleted portfolio's sales go with it, 4.7) sorts last.
        val b = buy("b", "AAPL", "2026-01-02", "1", 10_000L, 2L, portfolioId = "gone")
        val c = buy("c", "AAPL", "2026-01-02", "1", 10_000L, 3L)
        val history = listOf(b, c, sell("sb", "AAPL", "2026-02-01", 11_000L, 4L, "b" to "1", portfolioId = "gone"), sell("sc", "AAPL", "2026-02-01", 11_000L, 5L, "c" to "1"))
        assertEquals(listOf("p1", "gone"), RealizedGains.report(history, portfolios, null, null).holdings.map { it.portfolioId })
    }
}
