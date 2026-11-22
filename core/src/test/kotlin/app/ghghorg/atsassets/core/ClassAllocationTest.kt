package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

// 23b step 2: the allocation by sector or geography (4.8, 4.9, C14; AC-12, AC-32, AC-44, AC-46).
class ClassAllocationTest {
    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private val at = Instant.ofEpochSecond(1_793_304_000L)

    private fun price(text: String): Price = Price(BigDecimal(text), at)

    private val cash = dollars(9_600)
    private val aapl = Holding("p1", "AAPL", dollars(8), dollars(1_200))
    private val jnj = Holding("p1", "JNJ", dollars(2), dollars(300))
    private val vt = Holding("p1", "VT", dollars(10), dollars(1_000))
    private val prices = mapOf("AAPL" to price("160"), "JNJ" to price("150"), "VT" to price("110"))
    private val sectors = listOf("Health Care", "Technology")

    // Each holding's value as K10 gives it; VT, an ETF, has none (C14).
    private val sectorOf: (Holding) -> String? = { mapOf("AAPL" to "Technology", "JNJ" to "Health Care")[it.ticker] }

    // AC-44: Example A at $160 by sector is cash 88.2% and Technology 11.8%.
    @Test
    fun exampleA_bySector_isCashAndTechnology_at882_118() {
        val slices = Allocation.byClass(cash, listOf(aapl), prices, sectors, sectorOf)
        assertEquals(listOf(Slice(SliceKey.Cash, dollars(9_600)), Slice(SliceKey.OfClass("Technology"), dollars(1_280))), slices)
        assertEquals(listOf(882, 118), Allocation.tenths(slices.map { it.value }))
    }

    // Decision 1: after cash, the values in their list's order, then "Unclassified" last.
    @Test
    fun theValues_followTheListsOrder_andUnclassifiedComesLast() {
        val slices = Allocation.byClass(cash, listOf(vt, aapl, jnj), prices, sectors, sectorOf)
        assertEquals(
            listOf(
                Slice(SliceKey.Cash, dollars(9_600)),
                Slice(SliceKey.OfClass("Health Care"), dollars(300)),
                Slice(SliceKey.OfClass("Technology"), dollars(1_280)),
                Slice(SliceKey.Unclassified, dollars(1_100)),
            ),
            slices,
        )
    }

    // 4.9 (D7): texts that match are one value, in the list's spelling; the same value across portfolios is one slice.
    @Test
    fun valuesThatMatch_areOneSlice_inTheListsSpelling() {
        val other = Holding("p2", "MSFT", dollars(1), dollars(400))
        val valueOf: (Holding) -> String? = { if (it.ticker == "AAPL") "Technology" else " technology " }
        val slices = Allocation.byClass(cash, listOf(aapl, other), prices + ("MSFT" to price("400")), sectors, valueOf)
        assertEquals(listOf(Slice(SliceKey.Cash, dollars(9_600)), Slice(SliceKey.OfClass("Technology"), dollars(1_680))), slices)
    }

    // My reading: a value not on the list yet comes after the list's values, in the order of the holdings, as given.
    @Test
    fun aValueNotOnTheList_comesAfterTheListsValues() {
        val valueOf: (Holding) -> String? = { if (it.ticker == "AAPL") "Energy" else "Technology" }
        val slices = Allocation.byClass(Fraction.ZERO, listOf(aapl, jnj), prices, sectors, valueOf)
        assertEquals(listOf(SliceKey.OfClass("Technology"), SliceKey.OfClass("Energy")), slices.map { it.key })
    }

    // AC-32: cash is never "Unclassified"; a sold-out holding has no slice (4.8); view 1: only what has value.
    @Test
    fun cashIsNeverUnclassified_andASoldOutHolding_hasNoSlice() {
        val soldOut = Holding("p1", "OLDCO", Fraction.ZERO, Fraction.ZERO)
        val slices = Allocation.byClass(cash, listOf(vt, soldOut), prices, sectors) { null }
        assertEquals(listOf(Slice(SliceKey.Cash, dollars(9_600)), Slice(SliceKey.Unclassified, dollars(1_100))), slices)
        assertEquals(listOf(SliceKey.Unclassified), Allocation.byClass(Fraction.ZERO, listOf(vt), prices, sectors) { null }.map { it.key })
    }

    // 4.9 and 24: the value is a holding's, so one ticker in two portfolios may be in two slices.
    @Test
    fun theValueIsEachHoldings() {
        val other = Holding("p2", "AAPL", dollars(1), dollars(150))
        val valueOf: (Holding) -> String? = { if (it.portfolioId == "p1") "Technology" else "Health Care" }
        val slices = Allocation.byClass(Fraction.ZERO, listOf(aapl, other), prices, sectors, valueOf)
        assertEquals(listOf(Slice(SliceKey.OfClass("Health Care"), dollars(160)), Slice(SliceKey.OfClass("Technology"), dollars(1_280))), slices)
    }

    // AC-46: Portfolio P at $160 by sector is Technology at 100.0%, with no cash (4.8 portfolio level).
    @Test
    fun portfolioLevel_hasNoCash_andExampleA_isTechnologyAt100() {
        val slices = Allocation.portfolioByClass(listOf(aapl), prices, sectors, sectorOf)
        assertEquals(listOf(Slice(SliceKey.OfClass("Technology"), dollars(1_280))), slices)
        assertEquals(listOf(1_000), Allocation.tenths(slices.map { it.value }))
    }
}
