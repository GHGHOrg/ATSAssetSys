package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

// 17: Net Worth (Total) as the screen draws it (view 2, wireframe 02 markers 1, 2 and 6; 4.8, 4.10; AC-44; CR2).
class NetWorthModelTest {
    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    // 2026-10-29 16:00 New York, and the close a day earlier.
    private val close = Instant.ofEpochSecond(1_793_304_000L)
    private val dayBefore = Instant.ofEpochSecond(1_793_217_600L)

    private fun price(
        text: String,
        time: Instant = close,
        stale: Boolean = false,
    ): Price = Price(BigDecimal(text), time, stale)

    private val portfolioP = Portfolio("p1", "Portfolio P", 1L)
    private val portfolioQ = Portfolio("p2", "Portfolio Q", 2L)
    private val aapl = Holding("p1", "AAPL", dollars(8), dollars(1_200))
    private val refresh = PriceRefreshModel(refreshing = false, failed = false)

    private fun of(
        cashCents: Long,
        holdings: List<Holding>,
        portfolios: List<Portfolio>,
        prices: Map<String, Price>,
        refresh: PriceRefreshModel = this.refresh,
    ): NetWorthModel = NetWorthTotal.of(cashCents, holdings, portfolios, { id -> holdings.filter { it.portfolioId == id } }, prices, refresh)

    // AC-44: for Example A at $160 the composition is cash $9,600.00 and Portfolio P $1,280.00, adding to $10,880.00.
    @Test
    fun exampleA_at160_isCash9600_andPortfolioP1280_addingTo10880() {
        val model = of(960_000L, listOf(aapl), listOf(portfolioP), mapOf("AAPL" to price("160")))
        assertEquals(
            NetWorthModel(
                netWorth = "$10,880.00",
                spokenNetWorth = "10,880.00",
                prices = PricesAsOfModel("2026-10-29 16:00", someStale = false),
                refresh = refresh,
                composition =
                CompositionModel(
                    cash = "$9,600.00",
                    spokenCash = "9,600.00",
                    portfolios = listOf(CompositionRowModel("p1", "Portfolio P", "$1,280.00", "1,280.00")),
                ),
            ),
            model,
        )
    }

    // 4.10: the parts are rounded down and the cent left over goes to the largest dropped remainder (0.6 over 0.4).
    @Test
    fun aLeftoverCent_goesToTheLargestDroppedRemainder() {
        val holdings = listOf(Holding("p1", "LOW", dollars(1), dollars(1)), Holding("p2", "HIGH", dollars(1), dollars(1)))
        val prices = mapOf("LOW" to price("0.004"), "HIGH" to price("0.006"))
        val composition = of(100L, holdings, listOf(portfolioP, portfolioQ), prices).composition!!
        assertEquals("$1.00", composition.cash)
        assertEquals(listOf("$0.00", "$0.01"), composition.portfolios.map { it.amount })
    }

    // 4.10: equal remainders, the cent goes to the earlier row. Each portfolio alone would show $0.01 (decision 3).
    @Test
    fun aTieForTheLeftoverCent_goesToTheEarlierPortfolio_whileEachAloneWouldShowOneCent() {
        val holdings = listOf(Holding("p1", "ONE", dollars(1), dollars(1)), Holding("p2", "TWO", dollars(1), dollars(1)))
        val prices = mapOf("ONE" to price("0.005"), "TWO" to price("0.005"))
        val model = of(100L, holdings, listOf(portfolioP, portfolioQ), prices)
        assertEquals("$1.01", model.netWorth)
        assertEquals("$1.00", model.composition!!.cash)
        assertEquals(listOf("$0.01", "$0.00"), model.composition!!.portfolios.map { it.amount })
        assertEquals("$0.01", PortfolioFigures.of(holdings.drop(1), prices).value)
    }

    // 4.8: a portfolio with no price is in the composition at cost, and no price is used for the age line.
    @Test
    fun aPortfolioWithNoPrice_isAtCost_andThereIsNoAgeLine() {
        val model = of(960_000L, listOf(aapl), listOf(portfolioP), emptyMap())
        assertEquals("$10,800.00", model.netWorth)
        assertEquals("$1,200.00", model.composition!!.portfolios.single().amount)
        assertEquals(null, model.prices)
    }

    // 16b decision 1 beside net worth, and the refresh state passed on (AC-42).
    @Test
    fun anyStalePriceUsed_makesTheAgeLineSomeStale_theOldestTimeCounts_andTheRefreshStateIsPassedOn() {
        val msft = Holding("p2", "MSFT", dollars(1), dollars(400))
        val prices = mapOf("AAPL" to price("160", stale = true), "MSFT" to price("400", dayBefore))
        val failed = PriceRefreshModel(refreshing = true, failed = true)
        val model = of(960_000L, listOf(aapl, msft), listOf(portfolioP, portfolioQ), prices, failed)
        assertEquals(PricesAsOfModel("2026-10-28 16:00", someStale = true), model.prices)
        assertEquals(failed, model.refresh)
        assertEquals("$11,280.00", model.netWorth)
    }

    // Cash but no portfolio (view 6, first launch): the composition is cash alone, equal to net worth.
    @Test
    fun withNoPortfolio_theCompositionIsCashAlone() {
        val model = of(960_000L, emptyList(), emptyList(), emptyMap())
        assertEquals(CompositionModel("$9,600.00", "9,600.00", emptyList()), model.composition)
        assertEquals("$9,600.00", model.netWorth)
    }

    // Marker 6 and AC-44: net worth of $0.00 shows $0.00 and no composition.
    @Test
    fun aNetWorthOfZero_hasNoComposition() {
        val model = of(0L, emptyList(), emptyList(), emptyMap())
        assertEquals("$0.00", model.netWorth)
        assertEquals(null, model.composition)
    }

    // Decision 4: a net worth above 0 that shows as $0.00 (0.00000001 share at $0.10) has no composition either.
    @Test
    fun aNetWorthAboveZeroThatShowsAsZero_hasNoComposition() {
        val crumb = Holding("p1", "TINY", Fraction.of(BigInteger.ONE, BigInteger.TEN.pow(8)), Fraction.ZERO)
        val model = of(0L, listOf(crumb), listOf(portfolioP), mapOf("TINY" to price("0.10")))
        assertEquals("$0.00", model.netWorth)
        assertEquals(null, model.composition)
    }
}
