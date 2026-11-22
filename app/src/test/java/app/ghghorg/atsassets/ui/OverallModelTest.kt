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

// 16c: Overall as the screen draws it (view 1, wireframe 01 markers 2, 4 and 5; 4.8; CR2).
class OverallModelTest {
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
    private val msft = Holding("p2", "MSFT", dollars(1), dollars(400))
    private val holdings = listOf(aapl, msft)
    private val refresh = PriceRefreshModel(refreshing = false, failed = false)

    private fun of(
        netWorth: Fraction,
        portfolios: List<Portfolio>,
        prices: Map<String, Price>,
        refresh: PriceRefreshModel = this.refresh,
    ): OverallModel = Overall.of(netWorth, holdings, portfolios, { id -> holdings.filter { it.portfolioId == id } }, prices, refresh)

    @Test
    fun exampleA_at160_isWorth10880_withEachPortfolioAsTheListShowsIt() {
        val prices = mapOf("AAPL" to price("160"), "MSFT" to price("400", dayBefore))
        assertEquals(
            OverallModel(
                netWorth = "$10,880.00",
                spokenNetWorth = "10,880.00",
                prices = PricesAsOfModel("2026-10-28 16:00", someStale = false),
                refresh = refresh,
                portfolios =
                listOf(
                    OverallPortfolioRowModel("p1", "Portfolio P", PortfolioFigures.of(listOf(aapl), prices)),
                    OverallPortfolioRowModel("p2", "Portfolio Q", PortfolioFigures.of(listOf(msft), prices)),
                ),
            ),
            of(dollars(10_880), listOf(portfolioP, portfolioQ), prices),
        )
    }

    // 16b decision 1, here beside net worth: ", some stale" when any price used is stale.
    @Test
    fun anyStalePriceUsed_makesTheAgeLineSomeStale_andTheRefreshStateIsPassedOn() {
        val prices = mapOf("AAPL" to price("160"), "MSFT" to price("400", stale = true))
        val failed = PriceRefreshModel(refreshing = true, failed = true)
        val model = of(dollars(10_880), listOf(portfolioP, portfolioQ), prices, failed)
        assertEquals(PricesAsOfModel("2026-10-29 16:00", someStale = true), model.prices)
        assertEquals(failed, model.refresh)
    }

    // 4.8: no price used, no age line; a portfolio's row says "all holdings at cost" as on the Portfolios list.
    @Test
    fun withNoPriceUsed_thereIsNoAgeLine_andTheRowsAreAtCost() {
        val model = of(dollars(11_200), listOf(portfolioP), emptyMap())
        assertEquals(null, model.prices)
        assertEquals(true, model.portfolios.single().figures.allAtCost)
        assertEquals("$1,200.00", model.portfolios.single().figures.value)
    }

    @Test
    fun withNoPortfolio_thereAreNoRows() {
        val model = Overall.of(dollars(9_600), emptyList(), emptyList(), { emptyList() }, emptyMap(), refresh)
        assertEquals(emptyList<OverallPortfolioRowModel>(), model.portfolios)
        assertEquals("$9,600.00", model.netWorth)
        assertEquals(null, model.prices)
    }
}
