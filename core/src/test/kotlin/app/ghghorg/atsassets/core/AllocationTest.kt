package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

// 18 step 2: the allocation of net worth (4.8, C12; section 6, number format; AC-32, AC-43).
class AllocationTest {
    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private val at = Instant.ofEpochSecond(1_793_304_000L)

    private fun price(text: String): Price = Price(BigDecimal(text), at)

    private val cash = dollars(9_600)
    private val aapl = Holding("p1", "AAPL", dollars(8), dollars(1_200))
    private val prices = mapOf("AAPL" to price("160"))
    private val types = mapOf("AAPL" to AssetType.STOCK)

    // AC-32 and AC-43: Example A at $160 is cash 88.2%, stocks 11.8% and ETFs 0.0%, listed even at $0.00.
    @Test
    fun exampleA_byAssetType_isCashStocksAndEtfs_at882_118_and0() {
        val slices = Allocation.byAssetType(cash, listOf(aapl), prices, types)
        assertEquals(
            listOf(
                Slice(SliceKey.Cash, dollars(9_600)),
                Slice(SliceKey.Type(AssetType.STOCK), dollars(1_280)),
                Slice(SliceKey.Type(AssetType.ETF), Fraction.ZERO),
            ),
            slices,
        )
        assertEquals(listOf(882, 118, 0), Allocation.tenths(slices.map { it.value }))
    }

    // Section 6: each breakdown shows exactly 100.0%: rounded down, the leftover tenth to the largest dropped remainder.
    @Test
    fun theLeftoverTenth_goesToTheLargestDroppedRemainder() {
        // 333.33 and 666.67 tenths: 333 + 666 = 999, the tenth to the second.
        assertEquals(listOf(333, 667), Allocation.tenths(listOf(dollars(1), dollars(2))))
    }

    // 4.10's tie rule: equal remainders, the leftover tenth to the earlier row.
    @Test
    fun aTieForTheLeftoverTenth_goesToTheEarlierRow() {
        assertEquals(listOf(334, 333, 333), Allocation.tenths(listOf(dollars(1), dollars(1), dollars(1))))
    }

    // 4.8: a base of $0.00 has no shares.
    @Test
    fun aBaseOfZero_hasNoShares() {
        assertEquals(listOf(0, 0, 0), Allocation.tenths(listOf(Fraction.ZERO, Fraction.ZERO, Fraction.ZERO)))
    }

    // C12: on the net worth level the same ticker in two portfolios is one slice; holdings follow by ticker (decision 2).
    @Test
    fun byHolding_mergesATickerAcrossPortfolios_inTickerOrder_afterCash() {
        val holdings =
            listOf(
                Holding("p1", "MSFT", dollars(1), dollars(400)),
                Holding("p1", "AAPL", dollars(4), dollars(600)),
                Holding("p2", "AAPL", dollars(4), dollars(600)),
            )
        val slices = Allocation.byHolding(cash, holdings, prices + ("MSFT" to price("400")))
        assertEquals(
            listOf(
                Slice(SliceKey.Cash, dollars(9_600)),
                Slice(SliceKey.OfTicker("AAPL"), dollars(1_280)),
                Slice(SliceKey.OfTicker("MSFT"), dollars(400)),
            ),
            slices,
        )
    }

    // Decision 2: portfolios as given (the order created); one with no value has no slice (view 1: only what has value).
    @Test
    fun byPortfolio_followsTheGivenOrder_andLeavesOutAPortfolioWithNoValue() {
        val portfolios = listOf(Portfolio("p2", "Q", 1L), Portfolio("p1", "P", 2L), Portfolio("p3", "Empty", 3L))
        val holdings = listOf(aapl, Holding("p2", "MSFT", dollars(1), dollars(400)))
        val slices = Allocation.byPortfolio(cash, portfolios, holdings, prices)
        assertEquals(
            listOf(
                Slice(SliceKey.Cash, dollars(9_600)),
                Slice(SliceKey.OfPortfolio("p2"), dollars(400)),
                Slice(SliceKey.OfPortfolio("p1"), dollars(1_280)),
            ),
            slices,
        )
    }

    // 4.8: a holding with no price counts at cost; a sold-out holding has no slice.
    @Test
    fun aHoldingWithNoPrice_countsAtCost_andASoldOutHolding_hasNoSlice() {
        val soldOut = Holding("p1", "OLDCO", Fraction.ZERO, Fraction.ZERO)
        val slices = Allocation.byHolding(cash, listOf(aapl, soldOut), emptyMap())
        assertEquals(listOf(Slice(SliceKey.Cash, dollars(9_600)), Slice(SliceKey.OfTicker("AAPL"), dollars(1_200))), slices)
    }

    // The asset type comes from the ticker's instrument; a ticker without one counts as a stock (risk 4).
    @Test
    fun anEtf_isSlicedAsAnEtf_andATickerWithNoType_asAStock() {
        val holdings = listOf(aapl, Holding("p1", "VT", dollars(10), dollars(1_000)), Holding("p1", "NEW", dollars(1), dollars(50)))
        val slices = Allocation.byAssetType(cash, holdings, prices + ("VT" to price("110")), types + ("VT" to AssetType.ETF))
        assertEquals(
            listOf(
                Slice(SliceKey.Cash, dollars(9_600)),
                Slice(SliceKey.Type(AssetType.STOCK), dollars(1_330)),
                Slice(SliceKey.Type(AssetType.ETF), dollars(1_100)),
            ),
            slices,
        )
    }

    // View 1: options other than asset type list only what has value, cash included; asset type lists all three.
    @Test
    fun withNoCash_onlyAssetTypeListsCash() {
        assertEquals(
            listOf(SliceKey.Cash, SliceKey.Type(AssetType.STOCK), SliceKey.Type(AssetType.ETF)),
            Allocation.byAssetType(Fraction.ZERO, listOf(aapl), prices, types).map { it.key },
        )
        assertEquals(listOf(SliceKey.OfTicker("AAPL")), Allocation.byHolding(Fraction.ZERO, listOf(aapl), prices).map { it.key })
        assertEquals(
            listOf(SliceKey.OfPortfolio("p1")),
            Allocation.byPortfolio(Fraction.ZERO, listOf(Portfolio("p1", "P", 1L)), listOf(aapl), prices).map { it.key },
        )
    }
}
