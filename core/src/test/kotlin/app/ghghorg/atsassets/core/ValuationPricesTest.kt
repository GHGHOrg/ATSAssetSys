package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

/** K3 with prices (16a; spec 4.8, AC-41, AC-48): Example A at an AAPL price of $160. */
class ValuationPricesTest {
    private val today = LocalDate.of(2026, 6, 1)

    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        ticker: String = "AAPL",
        allocations: List<SellAllocation> = emptyList(),
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        allocations = allocations,
    )

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private fun fraction(
        numerator: Long,
        denominator: Long,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    private fun price(
        text: String,
        seconds: Long = 1_793_304_000L,
        stale: Boolean = false,
    ): Price = Price(BigDecimal(text), Instant.ofEpochSecond(seconds), stale)

    /** Example A: two buys of AAPL in Portfolio P, then a sell of 12 from both lots; 8 shares left, cost $1,200. */
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

    private val cash = listOf(CashEntry("o1", LocalDate.of(2026, 1, 1), 960_000L, CashType.OPENING_BALANCE, "", 1L))

    private val aapl get() = Holdings.holdings(exampleA, today).single()

    @Test
    fun aHolding_atItsLatestPrice_isSharesTimesPrice_withItsUnrealizedGain() {
        val valued = Valuation.valued(aapl, price("160"))
        assertEquals(dollars(1_280), valued.value)
        assertEquals(dollars(80), valued.gain)
        assertEquals(price("160"), valued.price)
    }

    @Test
    fun aStalePrice_keepsItsFigures_andIsMarkedStale() {
        val valued = Valuation.valued(aapl, price("160", stale = true))
        assertEquals(dollars(1_280), valued.value)
        assertEquals(dollars(80), valued.gain)
        assertEquals(true, valued.price!!.stale)
    }

    @Test
    fun aHoldingWithNoPrice_isValuedAtCost_withNoGain() {
        val valued = Valuation.valued(aapl, null)
        assertEquals(dollars(1_200), valued.value)
        assertEquals(null, valued.gain)
        assertEquals(null, valued.price)
    }

    @Test
    fun aPriceBelowCost_givesALoss() {
        assertEquals(dollars(-80), Valuation.valued(aapl, price("140")).gain)
    }

    @Test
    fun aSoldOutHolding_isWorthNothing_withAZeroGainOrNone() {
        val soldOut = Holding("p1", "AAPL", Fraction.ZERO, Fraction.ZERO)
        assertEquals(Fraction.ZERO, Valuation.valued(soldOut, price("160")).value)
        assertEquals(Fraction.ZERO, Valuation.valued(soldOut, price("160")).gain)
        assertEquals(Fraction.ZERO, Valuation.valued(soldOut, null).value)
        assertEquals(null, Valuation.valued(soldOut, null).gain)
    }

    @Test
    fun theFiguresStayExact_forPricesWith4Decimals_andSharesInThirds() {
        val third = Holding("p1", "AABB", fraction(10, 3), dollars(1))
        // 10/3 × 0.0089 = 0.089/3 = 89/3000; the gain 89/3000 − 1 = −2911/3000 (4.10).
        assertEquals(fraction(89, 3_000), Valuation.valued(third, price("0.0089")).value)
        assertEquals(fraction(-2_911, 3_000), Valuation.valued(third, price("0.0089")).gain)
        assertEquals(fraction(34_891, 30), Valuation.valued(third, price("348.9100")).value)
    }

    @Test
    fun aLot_atItsHoldingsPrice_hasItsOwnUnrealizedGain() {
        val lot = Holdings.lots(exampleA, today).single { it.remainingQuantity.signum() > 0 }
        assertEquals(dollars(80), Valuation.lotGain(lot, price("160")))
        assertEquals(dollars(-80), Valuation.lotGain(lot, price("140", stale = true)))
        assertEquals(null, Valuation.lotGain(lot, null))
    }

    @Test
    fun netWorth_isCashPlusEachHoldingAtItsPrice_orAtCostWithoutOne() {
        val holdings = Holdings.holdings(exampleA, today)
        assertEquals(dollars(10_880), Valuation.netWorthOf(cash, holdings, mapOf("AAPL" to price("160")), today))
        assertEquals(dollars(10_800), Valuation.netWorthOf(cash, holdings, emptyMap(), today))
        assertEquals(dollars(10_800), Valuation.netWorthOf(cash, holdings, mapOf("SPY" to price("500")), today))
        assertEquals(Valuation.netWorthOf(cash, holdings, today), Valuation.netWorthOf(cash, holdings, emptyMap(), today))
    }

    @Test
    fun thePriceAge_isTheOldestPriceUsed_leavingOutHoldingsAtCostAndSoldOut() {
        val holdings =
            listOf(
                Holding("p1", "AAPL", dollars(8), dollars(1_200)),
                Holding("p1", "SPY", dollars(2), dollars(1_000)),
                Holding("p1", "MSFT", dollars(1), dollars(400)),
                Holding("p1", "OLD", Fraction.ZERO, Fraction.ZERO),
            )
        val prices =
            mapOf(
                "AAPL" to price("160", seconds = 2_000L),
                "SPY" to price("500", seconds = 1_000L, stale = true),
                "OLD" to price("1", seconds = 10L),
            )
        assertEquals(Instant.ofEpochSecond(1_000L), Valuation.oldestPriceTime(holdings, prices))
        assertEquals(null, Valuation.oldestPriceTime(holdings, emptyMap()))
        assertEquals(null, Valuation.oldestPriceTime(emptyList(), prices))
    }
}
