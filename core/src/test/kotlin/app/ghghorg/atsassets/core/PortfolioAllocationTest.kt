package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

// 19 step 2: one portfolio's allocation (4.8 portfolio level, C12; view 4; AC-33, AC-46): no cash, the shares of the
// portfolio value.
class PortfolioAllocationTest {
    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private val at = Instant.ofEpochSecond(1_793_304_000L)

    private fun price(text: String): Price = Price(BigDecimal(text), at)

    private val aapl = Holding("p1", "AAPL", dollars(8), dollars(1_200))
    private val types = mapOf("AAPL" to AssetType.STOCK, "VT" to AssetType.ETF)

    // AC-46: Example A at $160, Portfolio P: Stocks $1,280.00 100.0% and ETFs $0.00 0.0%; AAPL one slice at 100.0%.
    @Test
    fun exampleA_isStocks100_Etfs0_andAapl100_withNoCash() {
        val prices = mapOf("AAPL" to price("160"))
        val byType = Allocation.portfolioByAssetType(listOf(aapl), prices, types)
        assertEquals(listOf(Slice(SliceKey.Type(AssetType.STOCK), dollars(1_280)), Slice(SliceKey.Type(AssetType.ETF), Fraction.ZERO)), byType)
        assertEquals(listOf(1_000, 0), Allocation.tenths(byType.map { it.value }))
        val byHolding = Allocation.portfolioByHolding(listOf(aapl), prices)
        assertEquals(listOf(Slice(SliceKey.OfTicker("AAPL"), dollars(1_280))), byHolding)
        assertEquals(listOf(1_000), Allocation.tenths(byHolding.map { it.value }))
    }

    // 4.8: a holding with no price at cost, a sold-out holding without a slice; holdings by ticker (18 decision 2).
    @Test
    fun anEtfAtItsPrice_aStockAtCost_andASoldOutHolding_byTicker() {
        val holdings = listOf(Holding("p1", "VT", dollars(10), dollars(1_000)), aapl, Holding("p1", "OLDCO", Fraction.ZERO, Fraction.ZERO))
        val prices = mapOf("VT" to price("110"))
        assertEquals(
            listOf(Slice(SliceKey.Type(AssetType.STOCK), dollars(1_200)), Slice(SliceKey.Type(AssetType.ETF), dollars(1_100))),
            Allocation.portfolioByAssetType(holdings, prices, types),
        )
        assertEquals(
            listOf(Slice(SliceKey.OfTicker("AAPL"), dollars(1_200)), Slice(SliceKey.OfTicker("VT"), dollars(1_100))),
            Allocation.portfolioByHolding(holdings, prices),
        )
    }

    // Marker 5: a portfolio whose only holding is sold out has no value; asset type still lists both at $0.00.
    @Test
    fun aPortfolioWithNoValue_hasTwoZeroTypes_andNoHolding() {
        val soldOut = listOf(Holding("p1", "OLDCO", Fraction.ZERO, Fraction.ZERO))
        assertEquals(
            listOf(Slice(SliceKey.Type(AssetType.STOCK), Fraction.ZERO), Slice(SliceKey.Type(AssetType.ETF), Fraction.ZERO)),
            Allocation.portfolioByAssetType(soldOut, emptyMap(), types),
        )
        assertEquals(emptyList<Slice>(), Allocation.portfolioByHolding(soldOut, emptyMap()))
    }
}
