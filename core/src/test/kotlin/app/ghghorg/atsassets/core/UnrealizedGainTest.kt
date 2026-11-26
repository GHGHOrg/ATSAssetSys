package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

// 43 step 2 (CR6; spec 4.8, AC-14, AC-72): the unrealized gain/loss over its cost, exact (4.10).
class UnrealizedGainTest {
    private val today = LocalDate.of(2026, 6, 1)

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private fun fraction(
        numerator: Long,
        denominator: Long,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    private fun price(
        text: String,
        stale: Boolean = false,
    ): Price = Price(BigDecimal(text), Instant.ofEpochSecond(1_793_304_000L), stale)

    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        allocations: List<SellAllocation> = emptyList(),
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        allocations = allocations,
    )

    /** Example A: two buys of AAPL, then a sell of 12 from both lots; 8 shares left in lot 2, cost $1,200. */
    private val exampleA =
        listOf(
            trade("b1", TransactionType.BUY, "2026-01-02", "10", 100_000L, 2L),
            trade("b2", TransactionType.BUY, "2026-02-01", "10", 150_000L, 3L),
            trade(
                "s1",
                TransactionType.SELL,
                "2026-03-01",
                "12",
                210_000L,
                4L,
                allocations = listOf(SellAllocation("b1", dollars(10)), SellAllocation("b2", dollars(2))),
            ),
        )

    private val aapl get() = Holdings.holdings(exampleA, today).single()

    private fun choice(
        id: String,
        available: Long,
        costPerShare: Long,
    ): LotChoice = LotChoice(id, LocalDate.of(2026, 1, 2), 2L, dollars(available), dollars(costPerShare), unavailable = null)

    // AC-72: Example A at $160, $80.00 over $1,200.00 = 1/15 (+6.67%).
    @Test
    fun exampleA_holding_is80Over1200() {
        val gain = UnrealizedGains.ofHolding(aapl, price("160"))!!
        assertEquals(UnrealizedGain(dollars(80), dollars(1_200)), gain)
        assertEquals(fraction(1, 15), gain.percent)
    }

    // AC-72 and AC-69: the lot of 2026-02-01, 8 left, over its remaining cost.
    @Test
    fun exampleA_lot_isOverItsRemainingCost() {
        val lot = Holdings.lots(exampleA, today).single { it.remainingQuantity.signum() > 0 }
        val gain = UnrealizedGains.ofLot(lot, price("160"))!!
        assertEquals(UnrealizedGain(dollars(80), dollars(1_200)), gain)
        assertEquals(fraction(1, 15), gain.percent)
        assertEquals(null, UnrealizedGains.ofLot(lot, null))
    }

    // AC-14: before the Mar 1 sale at $160, the picker's lots: $600 over $1,000 (+60.00%), $100 over $1,500 (+6.67%).
    @Test
    fun ac14_sellLotChoices_areOverWhatTheyCost() {
        val lot1 = UnrealizedGains.ofChoice(choice("b1", 10, 100), Fraction.ONE, price("160"))!!
        val lot2 = UnrealizedGains.ofChoice(choice("b2", 10, 150), Fraction.ONE, price("160"))!!
        assertEquals(UnrealizedGain(dollars(600), dollars(1_000)), lot1)
        assertEquals(fraction(3, 5), lot1.percent)
        assertEquals(UnrealizedGain(dollars(100), dollars(1_500)), lot2)
        assertEquals(fraction(1, 15), lot2.percent)
        assertEquals(null, UnrealizedGains.ofChoice(choice("b1", 10, 100), Fraction.ONE, null))
    }

    // A split after the buy (16c decision 4): 5 shares as bought are 10 today at $60; $600 against the $500 they cost.
    @Test
    fun aChoiceAcrossASplit_isValuedInTodaysShares_overItsOriginalCost() {
        val gain = UnrealizedGains.ofChoice(choice("b1", 5, 100), dollars(2), price("60"))!!
        assertEquals(UnrealizedGain(dollars(100), dollars(500)), gain)
        assertEquals(fraction(1, 5), gain.percent)
    }

    // AC-72 / CR6 outcome 4: a "no price" holding (cost $800) is left out of both, so P stays at 1/15, not 80/2000.
    @Test
    fun aTotal_leavesANoPriceHoldingOutOfBoth() {
        val xyz = Holding("p1", "XYZ", dollars(5), dollars(800))
        val total = UnrealizedGains.total(listOf(UnrealizedGains.ofHolding(aapl, price("160")), UnrealizedGains.ofHolding(xyz, null)))
        assertEquals(UnrealizedGain(dollars(80), dollars(1_200)), total)
        assertEquals(fraction(1, 15), total.percent)
    }

    // A total is its own ratio, not an average: 1/15 and 3/5 over their costs give 680/2,200, not their mean.
    @Test
    fun aTotal_isItsOwnRatio_notAnAverage() {
        val total =
            UnrealizedGains.total(
                listOf(UnrealizedGain(dollars(80), dollars(1_200)), UnrealizedGain(dollars(600), dollars(1_000))),
            )
        assertEquals(fraction(680, 2_200), total.percent)
    }

    // AC-72: at $140, −$80.00 over $1,200.00 = −1/15; a stale price counts as for the gain/loss.
    @Test
    fun aLoss_andAStalePrice() {
        assertEquals(fraction(-1, 15), UnrealizedGains.ofHolding(aapl, price("140"))!!.percent)
        assertEquals(fraction(1, 15), UnrealizedGains.ofHolding(aapl, price("160", stale = true))!!.percent)
    }

    // AC-72: a fully sold holding, $0.00 over $0.00, has no percentage; nor has a total with no figure at all.
    @Test
    fun aCostOfZero_hasNoPercentage() {
        val soldOut = Holding("p1", "AAPL", Fraction.ZERO, Fraction.ZERO)
        val gain = UnrealizedGains.ofHolding(soldOut, price("160"))!!
        assertEquals(UnrealizedGain(Fraction.ZERO, Fraction.ZERO), gain)
        assertEquals(null, gain.percent)
        assertEquals(null, UnrealizedGains.ofHolding(aapl, null))
        val none = UnrealizedGains.total(listOf(null, null))
        assertEquals(UnrealizedGain(Fraction.ZERO, Fraction.ZERO), none)
        assertEquals(null, none.percent)
    }

    // Exact (4.10): a gain of $1 on $3 is 1/3, not 0.3333.
    @Test
    fun thePercentage_isExact() {
        val third = Holding("p1", "AABB", dollars(1), dollars(3))
        assertEquals(fraction(1, 3), UnrealizedGains.ofHolding(third, price("4"))!!.percent)
    }
}
