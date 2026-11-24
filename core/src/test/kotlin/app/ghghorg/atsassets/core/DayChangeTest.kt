package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

/**
 * The 1-day gain/loss (CR5, 41 step 6; spec 4.8, C24, C25; AC-67). Example A at an AAPL price of $160 with a previous
 * close of $158; the price's market date is 2026-03-02 (16:00 New York), after the sell of Mar 1.
 */
class DayChangeTest {
    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        allocations: List<SellAllocation> = emptyList(),
        split: SplitRatio? = null,
        reversesId: String? = null,
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
        split = split,
        reversesId = reversesId,
    )

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private fun fraction(
        numerator: Long,
        denominator: Long,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    // 2026-03-02 16:00 New York (EST, UTC−5).
    private val close = Instant.parse("2026-03-02T21:00:00Z")

    private fun price(
        perShare: String = "160",
        previousClose: String? = "158",
        time: Instant = close,
        stale: Boolean = false,
    ): Price = Price(BigDecimal(perShare), time, stale, previousClose?.let { BigDecimal(it) })

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

    private fun day(
        transactions: List<Transaction>,
        price: Price?,
        today: LocalDate = LocalDate.of(2026, 3, 2),
    ): DayChange? = DayChanges.ofHolding(Holdings.lotsOf(transactions, "p1", "AAPL", today), transactions, price)

    @Test
    fun theDay_isThePricesMarketDate_inNewYork() {
        assertEquals(LocalDate.of(2026, 3, 2), DayChanges.dayOf(price()))
        // 01:00 UTC on Mar 3 is 20:00 on Mar 2 in New York.
        assertEquals(LocalDate.of(2026, 3, 2), DayChanges.dayOf(price(time = Instant.parse("2026-03-03T01:00:00Z"))))
    }

    // AC-67: 8 shares held at the close, 8 × ($160 − $158) = +$16.00, over $1,264.00 = +1.27%.
    @Test
    fun exampleA_countsFromThePreviousClose() {
        val day = day(exampleA, price())!!
        assertEquals(DayChange(dollars(16), dollars(1_264)), day)
        assertEquals(fraction(1, 79), day.percent)
    }

    // AC-67: a buy of 2 for $318.00 on the day counts from its $159.00: 8 × $2 + 2 × $1 = +$18.00, over $1,582.00.
    @Test
    fun aBuyOnTheDay_countsFromItsBuyPrice() {
        val withBuy = exampleA + trade("b3", TransactionType.BUY, "2026-03-02", "2", 31_800L, 5L)
        val day = day(withBuy, price())!!
        assertEquals(DayChange(dollars(18), dollars(1_582)), day)
        assertEquals(fraction(9, 791), day.percent)
    }

    // AC-67: instead, a sell of 3 from lot 2 on the day leaves 5 held at the close: 5 × $2 = +$10.00; the sale counts nowhere.
    @Test
    fun aSellOnTheDay_isLeftOut() {
        val withSell = exampleA + trade("s2", TransactionType.SELL, "2026-03-02", "3", 48_000L, 5L, listOf(SellAllocation("b2", dollars(3))))
        assertEquals(DayChange(dollars(10), dollars(790)), day(withSell, price()))
    }

    // C25: a buy dated after the price's day (Monday's buy, the price still Friday's) counts from its buy price too.
    @Test
    fun aBuyAfterThePricesDay_countsFromItsBuyPrice() {
        val withBuy = exampleA + trade("b3", TransactionType.BUY, "2026-03-03", "1", 15_500L, 5L)
        assertEquals(DayChange(dollars(16 + 5), dollars(1_264 + 155)), day(withBuy, price(), today = LocalDate.of(2026, 3, 3)))
    }

    // 4.8: a stale price still gives the figure, from its last price and that price's previous close (the screen marks it).
    @Test
    fun aStalePrice_stillGivesTheFigure() {
        assertEquals(DayChange(dollars(16), dollars(1_264)), day(exampleA, price(stale = true)))
    }

    // 4.8, C24: no price, no previous close, or a split of the holding on the day: no figure.
    @Test
    fun noPrice_noPreviousClose_orASplitOnTheDay_giveNoFigure() {
        assertEquals(null, day(exampleA, null))
        assertEquals(null, day(exampleA, price(previousClose = null)))
        val split = trade("x1", TransactionType.SPLIT, "2026-03-02", "0", 0L, 5L, split = SplitRatio(2, 1))
        assertEquals(null, day(exampleA + split, price()))
    }

    // C24: a split reversed on the day changes the shares that day too; a split on another day does not stop the figure.
    @Test
    fun aSplitReversedOnTheDay_givesNoFigure_andASplitOnAnotherDayDoesNotMatter() {
        // A 2 for 1 split on Mar 1, entered after the sell: the 8 shares left become 16.
        val split = trade("x1", TransactionType.SPLIT, "2026-03-01", "0", 0L, 5L, split = SplitRatio(2, 1))
        val reversed = trade("r1", TransactionType.REVERSAL, "2026-03-02", "0", 0L, 6L, reversesId = "x1")
        assertEquals(null, day(exampleA + split + reversed, price()))
        // At $80 with a previous close of $79: 16 × $1 over 16 × $79.
        assertEquals(DayChange(dollars(16), dollars(16 * 79)), day(exampleA + split, price("80", "79")))
    }

    // 4.8: a sold-out holding has no lot: $0.00 over a start of $0.00, and so no percentage.
    @Test
    fun aSoldOutHolding_isZero_withNoPercentage() {
        val soldOut = exampleA + trade("s2", TransactionType.SELL, "2026-03-01", "8", 128_000L, 5L, listOf(SellAllocation("b2", dollars(8))))
        val day = day(soldOut, price())!!
        assertEquals(DayChange(Fraction.ZERO, Fraction.ZERO), day)
        assertEquals(null, day.percent)
    }

    // 4.10: exact over a lot of 1/3 share (Example I's shape): 1/3 × ($100.01 − $100.00), over 1/3 × $100.00.
    @Test
    fun theFigure_isExact_forAFractionOfAShare() {
        val third =
            listOf(
                trade("b1", TransactionType.BUY, "2026-01-02", "1", 10_000L, 2L),
                trade("x1", TransactionType.SPLIT, "2026-01-02", "0", 0L, 3L, split = SplitRatio(1, 3)),
            )
        val day = day(third, price("100.01", "100"))!!
        assertEquals(DayChange(fraction(1, 300), fraction(100, 3)), day)
        assertEquals(fraction(1, 10_000), day.percent)
    }

    // 4.8: a total sums the figures there are; a holding without one adds nothing; with none at all, no line.
    @Test
    fun aTotal_sumsTheFiguresThereAre_andWithNoneHasNoFigure() {
        val a = DayChange(dollars(16), dollars(1_264))
        val b = DayChange(dollars(-6), dollars(406))
        assertEquals(DayChange(dollars(10), dollars(1_670)), DayChanges.total(listOf(a, null, b)))
        assertEquals(null, DayChanges.total(listOf(null, null)))
        assertEquals(null, DayChanges.total(emptyList()))
        assertEquals(DayChange(dollars(10), dollars(1_670)), a + b)
    }
}
