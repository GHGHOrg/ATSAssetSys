package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

// 18 step 3: a breakdown as Overall and Net Worth (Total) draw it (view 1, view 2; 4.10; AC-32, AC-43; CR2).
class AllocationModelTest {
    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private val at = Instant.ofEpochSecond(1_793_304_000L)

    private fun price(text: String): Price = Price(BigDecimal(text), at)

    private val portfolioP = Portfolio("p1", "Portfolio P", 1L)
    private val portfolioQ = Portfolio("p2", "Portfolio Q", 2L)
    private val aapl = Holding("p1", "AAPL", dollars(8), dollars(1_200))
    private val types = mapOf("AAPL" to AssetType.STOCK)

    private fun of(
        by: AllocationBy,
        cashCents: Long,
        holdings: List<Holding>,
        prices: Map<String, Price>,
        portfolios: List<Portfolio> = listOf(portfolioP, portfolioQ),
    ): AllocationModel? = Allocations.of(by, cashCents, holdings, portfolios, prices, types)

    // AC-43: Example A at $160 on Asset type: 88.2% and 11.8% on the donut, the rows $9,600.00, $1,280.00 and $0.00,
    // and ETFs at 0.0% in its row, with no arc.
    @Test
    fun exampleA_byAssetType_matchesAC43() {
        assertEquals(
            AllocationModel(
                AllocationBy.ASSET_TYPE,
                listOf(
                    AllocationRowModel(SliceName.Cash, "$9,600.00", "9,600.00", "88.2", percentInRow = false, tenths = 882),
                    AllocationRowModel(SliceName.Type(AssetType.STOCK), "$1,280.00", "1,280.00", "11.8", percentInRow = false, tenths = 118),
                    AllocationRowModel(SliceName.Type(AssetType.ETF), "$0.00", "0.00", "0.0", percentInRow = true, tenths = 0),
                ),
            ),
            of(AllocationBy.ASSET_TYPE, 960_000L, listOf(aapl), mapOf("AAPL" to price("160"))),
        )
    }

    // View 1: a slice under 5% shows its percentage in its row; one of 5% or more on the donut.
    @Test
    fun aSliceUnder5Percent_showsItsPercentageInItsRow() {
        val one = Holding("p1", "AAPL", dollars(1), dollars(150))
        val rows = of(AllocationBy.HOLDING, 960_000L, listOf(one), mapOf("AAPL" to price("160")))!!.rows
        // $160 of $9,760: 1.6%.
        assertEquals(listOf("98.4" to false, "1.6" to true), rows.map { it.percent to it.percentInRow })
        assertEquals(listOf(SliceName.Cash, SliceName.Named("AAPL")), rows.map { it.name })
    }

    // Decision 3: the amounts by largest remainder add up to the net worth shown, as the composition does (17).
    @Test
    fun theAmounts_areByLargestRemainder_likeTheComposition() {
        val holdings = listOf(Holding("p1", "ONE", dollars(1), dollars(1)), Holding("p2", "TWO", dollars(1), dollars(1)))
        val prices = mapOf("ONE" to price("0.005"), "TWO" to price("0.005"))
        val rows = of(AllocationBy.PORTFOLIO, 100L, holdings, prices)!!.rows
        assertEquals(listOf("$1.00", "$0.01", "$0.00"), rows.map { it.amount })
        assertEquals(listOf(SliceName.Cash, SliceName.Named("Portfolio P"), SliceName.Named("Portfolio Q")), rows.map { it.name })
    }

    // Section 6: each breakdown's percentages add up to exactly 100.0%.
    @Test
    fun eachBreakdown_addsUpTo100Percent() {
        val holdings = listOf(aapl, Holding("p2", "MSFT", dollars(1), dollars(400)))
        val prices = mapOf("AAPL" to price("160"), "MSFT" to price("400"))
        AllocationBy.entries.forEach { by ->
            assertEquals("$by", 1_000, of(by, 960_000L, holdings, prices)!!.rows.sumOf { it.tenths })
        }
    }

    // 4.8 and 17 decision 4: no breakdown while net worth shows as $0.00, also when it is above 0 but rounds to it.
    @Test
    fun noBreakdown_whileNetWorthShowsAsZero() {
        assertEquals(null, of(AllocationBy.ASSET_TYPE, 0L, emptyList(), emptyMap()))
        val crumb = Holding("p1", "TINY", Fraction.of(BigInteger.ONE, BigInteger.TEN.pow(8)), Fraction.ZERO)
        assertEquals(null, of(AllocationBy.HOLDING, 0L, listOf(crumb), mapOf("TINY" to price("0.10"))))
    }
}
