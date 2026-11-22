package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class HidingTest {
    private fun bought(
        id: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        portfolioId: String = "p1",
    ): Transaction = Transaction(
        id = id,
        portfolioId = portfolioId,
        ticker = "AAPL",
        type = TransactionType.BUY,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
    )

    private fun sold(
        id: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        allocations: List<SellAllocation>,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = TransactionType.SELL,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        allocations = allocations,
    )

    private fun reversalOf(
        original: Transaction,
        date: String,
        seq: Long,
    ): Transaction = Transaction(
        id = "r-${original.id}",
        portfolioId = original.portfolioId,
        ticker = original.ticker,
        type = TransactionType.REVERSAL,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal.ZERO,
        totalCents = 0L,
        reversesId = original.id,
    )

    private fun splitOn(
        id: String,
        date: String,
        to: Int,
        from: Int,
        seq: Long,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = TransactionType.SPLIT,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal.ZERO,
        totalCents = 0L,
        split = SplitRatio(to, from),
    )

    private fun fraction(
        numerator: Long,
        denominator: Long = 1L,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    // Criterion 60(a): buy 10 AAPL for $100 on Jan 2, sell all 10 for $120 on Jan 3.
    private val b1 = bought("b1", "2026-01-02", "10", 10_000L, 2L)
    private val s1 = sold("s1", "2026-01-03", "10", 12_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
    private val aapl = HiddenHolding("p1", "AAPL")

    @Test
    fun hide_isOfferedAtExactlyZeroShares_andNotWhileHeld_orAlreadyHidden() {
        assertFalse(Hiding.offered(listOf(b1), emptySet(), "p1", "AAPL"))
        assertTrue(Hiding.offered(listOf(b1, s1), emptySet(), "p1", "AAPL"))
        assertFalse(Hiding.offered(listOf(b1, s1), setOf(aapl), "p1", "AAPL"))
        assertFalse(Hiding.offered(emptyList(), emptySet(), "p1", "AAPL"))
    }

    // Decision 1: every entry counts, a later-dated one included, so a hidden holding is always at 0 (section 8).
    @Test
    fun hide_isNotOffered_whenALaterDatedBuyGivesTheHoldingShares() {
        val later = bought("b2", "2099-12-31", "1", 1_000L, 4L)
        assertFalse(Hiding.offered(listOf(b1, s1, later), emptySet(), "p1", "AAPL"))
    }

    // CR1, Example H: a leftover of 0.00000003 / 7 is not 0; once sold, Hide is offered.
    @Test
    fun hide_isNotOfferedForALeftover_andIsOnceTheLeftoverIsSold() {
        val one = bought("b1", "2026-01-02", "1", 1_000L, 2L)
        val most = sold("s1", "2026-01-03", "0.99999997", 1_000L, 3L, listOf(SellAllocation("b1", Fraction.of(BigDecimal("0.99999997")))))
        val oneForSeven = splitOn("x1", "2026-01-04", 1, 7, 4L)
        val withLeftover = listOf(one, most, oneForSeven)
        assertFalse(Hiding.offered(withLeftover, emptySet(), "p1", "AAPL"))
        val rest = sold("s2", "2026-01-05", "0.00000001", 0L, 5L, listOf(SellAllocation("b1", fraction(3, 700_000_000))))
        assertTrue(Hiding.offered(withLeftover + rest, emptySet(), "p1", "AAPL"))
    }

    @Test
    fun criterion60a_theSellsReversal_leavesTheHiddenHoldingWith10Shares_andUnhidesIt() {
        val after = listOf(b1, s1, reversalOf(s1, "2026-01-03", 4L))
        assertEquals(listOf(aapl), Hiding.unhiddenBy(after, setOf(aapl)))
        assertEquals(fraction(10), Holdings.holdings(after, LocalDate.of(2026, 1, 3))[0].shares)
    }

    @Test
    fun aNewBuyOfAHiddenTicker_unhidesIt_andABuyInAnotherPortfolioDoesNot() {
        assertEquals(listOf(aapl), Hiding.unhiddenBy(listOf(b1, s1, bought("b3", "2026-02-01", "1", 1_000L, 4L)), setOf(aapl)))
        assertEquals(emptyList<HiddenHolding>(), Hiding.unhiddenBy(listOf(b1, s1, bought("b4", "2026-02-01", "1", 1_000L, 4L, "p2")), setOf(aapl)))
        assertEquals(emptyList<HiddenHolding>(), Hiding.unhiddenBy(listOf(b1, s1), setOf(aapl)))
    }

    // Criterion 60(b): Reverse and re-enter of a sell at 0 ends at 0, so the holding stays hidden.
    @Test
    fun aSellReenteredAtZero_keepsTheHoldingHidden() {
        val reversal = reversalOf(s1, "2026-01-03", 4L)
        val newSell = sold("n1", "2026-01-03", "10", 5_000L, 5L, listOf(SellAllocation("b1", fraction(10))))
        assertEquals(emptyList<HiddenHolding>(), Hiding.unhiddenBy(listOf(b1, s1, reversal, newSell), setOf(aapl)))
    }

    @Test
    fun theLastTransactionDate_includesAReversal_andIsNullWithNone() {
        assertEquals(LocalDate.of(2026, 1, 3), Hiding.lastTransactionDate(listOf(b1, s1), "p1", "AAPL"))
        assertEquals(LocalDate.of(2026, 1, 5), Hiding.lastTransactionDate(listOf(b1, s1, reversalOf(s1, "2026-01-05", 4L)), "p1", "AAPL"))
        assertNull(Hiding.lastTransactionDate(listOf(b1, s1), "p2", "AAPL"))
    }
}
