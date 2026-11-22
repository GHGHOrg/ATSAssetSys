package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SourceProfile
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

// 19 step 3: Allocation, one portfolio, as the screen draws it (view 4, wireframe 04; 4.8; AC-33, AC-46; CR2).
class PortfolioAllocationModelTest {
    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    // 2026-10-29 16:00 New York.
    private val close = Instant.ofEpochSecond(1_793_304_000L)

    private val portfolioP = Portfolio("p1", "Portfolio P", 1L)
    private val portfolioQ = Portfolio("p2", "Portfolio Q", 2L)
    private val aapl = Holding("p1", "AAPL", dollars(8), dollars(1_200))
    private val oldco = Holding("p2", "OLDCO", Fraction.ZERO, Fraction.ZERO)
    private val holdings = listOf(aapl, oldco)
    private val prices = mapOf("AAPL" to Price(BigDecimal("160"), close))
    private val types = mapOf("AAPL" to AssetType.STOCK, "OLDCO" to AssetType.STOCK)
    private val refresh = PriceRefreshModel(refreshing = false, failed = false)

    // 23b: AAPL's source gave "Technology" and "US" (23a, R1).
    private val classes = HoldingClasses(ClassLists(listOf("Technology"), listOf("US")), mapOf("AAPL" to SourceProfile("Technology", "US")))

    private fun of(
        portfolios: List<Portfolio>,
        shownId: String?,
        refresh: PriceRefreshModel = this.refresh,
        prices: Map<String, Price> = this.prices,
    ): PortfolioAllocationModel = PortfolioAllocation.of(portfolios, shownId, { id -> holdings.filter { it.portfolioId == id } }, prices, types, refresh, classes)

    // AC-46: Example A at $160, Portfolio P: $1,280.00 with its age; Stocks 100.0% and ETFs 0.0% (in its row); AAPL 100.0%;
    // from 23b, Technology and the geography 100.0% each, the geography as the source spells it (decision 2).
    @Test
    fun exampleA_portfolioP_matchesAC46() {
        val choices = listOf(PortfolioChoiceModel("p1", "Portfolio P"), PortfolioChoiceModel("p2", "Portfolio Q"))
        assertEquals(
            PortfolioAllocationModel(
                portfolios = choices,
                shown = choices.first(),
                value = "$1,280.00",
                spokenValue = "1,280.00",
                prices = PricesAsOfModel("2026-10-29 16:00", someStale = false),
                refresh = refresh,
                breakdowns =
                listOf(
                    AllocationModel(
                        AllocationBy.ASSET_TYPE,
                        listOf(
                            AllocationRowModel(SliceName.Type(AssetType.STOCK), "$1,280.00", "1,280.00", "100.0", percentInRow = false, tenths = 1_000),
                            AllocationRowModel(SliceName.Type(AssetType.ETF), "$0.00", "0.00", "0.0", percentInRow = true, tenths = 0),
                        ),
                    ),
                    AllocationModel(
                        AllocationBy.HOLDING,
                        listOf(AllocationRowModel(SliceName.Named("AAPL"), "$1,280.00", "1,280.00", "100.0", percentInRow = false, tenths = 1_000)),
                    ),
                    AllocationModel(
                        AllocationBy.SECTOR,
                        listOf(AllocationRowModel(SliceName.Named("Technology"), "$1,280.00", "1,280.00", "100.0", percentInRow = false, tenths = 1_000)),
                    ),
                    AllocationModel(
                        AllocationBy.GEOGRAPHY,
                        listOf(AllocationRowModel(SliceName.Named("US"), "$1,280.00", "1,280.00", "100.0", percentInRow = false, tenths = 1_000)),
                    ),
                ),
            ),
            of(listOf(portfolioP, portfolioQ), "p1"),
        )
    }

    // View 4: from More, or for a portfolio no longer there, the first portfolio as created.
    @Test
    fun withNoOrAnUnknownPortfolio_theFirstIsShown() {
        assertEquals("p1", of(listOf(portfolioP, portfolioQ), null).shown?.id)
        assertEquals("p1", of(listOf(portfolioP, portfolioQ), "gone").shown?.id)
    }

    // Marker 5 and AC-46: a portfolio whose only holding is fully sold is worth $0.00 and has no breakdown.
    @Test
    fun aPortfolioWorthZero_showsZero_andNoBreakdown() {
        val model = of(listOf(portfolioP, portfolioQ), "p2")
        assertEquals("$0.00", model.value)
        assertEquals(null, model.breakdowns)
        assertEquals(null, model.prices)
    }

    // Marker 4: no portfolio.
    @Test
    fun withNoPortfolio_nothingIsShown() {
        val model = of(emptyList(), null)
        assertEquals(emptyList<PortfolioChoiceModel>(), model.portfolios)
        assertEquals(null, model.shown)
        assertEquals(null, model.breakdowns)
    }

    // Marker 2 and AC-42: "some stale" and the refresh state passed on.
    @Test
    fun aStalePrice_makesTheAgeLineSomeStale_andTheRefreshStateIsPassedOn() {
        val failed = PriceRefreshModel(refreshing = true, failed = true)
        val model = of(listOf(portfolioP), "p1", failed, mapOf("AAPL" to Price(BigDecimal("160"), close, stale = true)))
        assertEquals(PricesAsOfModel("2026-10-29 16:00", someStale = true), model.prices)
        assertEquals(failed, model.refresh)
    }
}
