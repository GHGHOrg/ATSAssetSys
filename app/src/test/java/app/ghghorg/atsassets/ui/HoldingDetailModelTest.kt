package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.DayChange
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Lot
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

// 16a step 6: Holding detail's header and lots as the screen draws them (view 6, AC-41, AC-48; CR2).
class HoldingDetailModelTest {
    private val today = LocalDate.of(2026, 6, 1)

    // 2026-10-29 16:00 New York, the close in the price-source note's AAPL quote (10.2).
    private val close = Instant.ofEpochSecond(1_793_304_000L)

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private fun price(
        text: String,
        stale: Boolean = false,
    ): Price = Price(BigDecimal(text), close, stale)

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

    // Example A: 8 AAPL left in Portfolio P, one lot of 2026-02-01 at $150.00, cost $1,200.00.
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
                listOf(SellAllocation("b1", dollars(10)), SellAllocation("b2", dollars(2))),
            ),
        )
    private val aapl = Holdings.holdings(exampleA, today).single()
    private val lots = Holdings.lotsOf(exampleA, "p1", "AAPL", today)

    private fun header(
        holding: Holding = aapl,
        price: Price? = price("160"),
        refreshing: Boolean = false,
        refreshFailed: Boolean = false,
    ): HoldingHeaderModel = HoldingDetail.header(holding, "Portfolio P", price, refreshing, refreshFailed)

    @Test
    fun exampleA_at160_showsTheValue_theGainInGreen_andThePriceWithItsTime() {
        assertEquals(
            HoldingHeaderModel(
                ticker = "AAPL",
                portfolio = "Portfolio P",
                value = "$1,280.00",
                spokenValue = "1,280.00",
                shares = "8",
                oneShare = false,
                cost = "$1,200.00",
                spokenCost = "1,200.00",
                gain = GainModel.Figure("+$80.00", SpokenFigure("80.00", FigureSign.PLUS)),
                price = PriceLineModel("$160.00", "160.00", "2026-10-29 16:00", stale = false),
                refreshing = false,
                refreshFailed = false,
            ),
            header(),
        )
    }

    @Test
    fun aLoss_showsAMinusSign_andIsRed() {
        assertEquals(GainModel.Figure("−$80.00", SpokenFigure("80.00", FigureSign.MINUS)), header(price = price("140")).gain)
    }

    @Test
    fun aStalePrice_keepsItsFigures_andIsMarkedStale() {
        val stale = header(price = price("160", stale = true))
        assertEquals("$1,280.00", stale.value)
        assertEquals(GainModel.Figure("+$80.00", SpokenFigure("80.00", FigureSign.PLUS)), stale.gain)
        assertEquals(true, stale.price!!.stale)
    }

    @Test
    fun noPrice_isValuedAtCost_withNoGain_andNoPriceLine() {
        val atCost = header(price = null)
        assertEquals("$1,200.00", atCost.value)
        assertEquals(GainModel.NoPrice, atCost.gain)
        assertEquals(null, atCost.price)
    }

    @Test
    fun aSoldOutHolding_showsZero_withNoColorAndNoNoPriceFlag() {
        val soldOut = Holding("p1", "AAPL", Fraction.ZERO, Fraction.ZERO)
        for (price in listOf(price("160"), null)) {
            val model = header(holding = soldOut, price = price)
            assertEquals("$0.00", model.value)
            assertEquals(GainModel.Figure("$0.00", SpokenFigure("0.00", FigureSign.NONE)), model.gain)
        }
    }

    @Test
    fun aGainThatRoundsToZero_isShownAsZero_withNoColor() {
        // 1 share at a cost of $1.00 and a price of $1.004: the gain is $0.004, shown as $0.00 (4.10).
        val one = Holding("p1", "X", dollars(1), dollars(1))
        assertEquals(GainModel.Figure("$0.00", SpokenFigure("0.00", FigureSign.NONE)), header(holding = one, price = price("1.004")).gain)
    }

    @Test
    fun aPriceUnder1Dollar_shows4Decimals_andOneShareIsSingular() {
        val one = Holding("p1", "AABB", dollars(1), dollars(1))
        val model = header(holding = one, price = price("0.0089"))
        assertEquals("$0.0089", model.price!!.perShare)
        assertEquals("0.0089", model.price.spokenPerShare)
        assertEquals(true, model.oneShare)
    }

    // CR5 (41), AC-67: the header carries the holding's day line; with no figure it has none.
    @Test
    fun theHeader_carriesTheHoldingsDayLine_orNone() {
        val withDay =
            HoldingDetail.header(
                Holdings.holdings(exampleA, today).single(),
                "Portfolio P",
                price("160"),
                refreshing = false,
                refreshFailed = false,
                day = DayChange(dollars(16), dollars(1_264)),
            )
        assertEquals("+$16.00", withDay.day!!.text)
        assertEquals("+1.27%", withDay.day.percent)
        assertEquals(null, header().day)
    }

    @Test
    fun theRefreshState_passesThrough() {
        assertEquals(true, header(refreshing = true).refreshing)
        assertEquals(true, header(refreshFailed = true).refreshFailed)
    }

    @Test
    fun thePriceTime_isNewYorkTime_inWinterToo() {
        assertEquals("2026-01-15 16:00", HoldingDetail.asOf(Instant.parse("2026-01-15T21:00:00Z")))
        assertEquals("2026-07-15 16:00", HoldingDetail.asOf(Instant.parse("2026-07-15T20:00:00Z")))
    }

    @Test
    fun exampleA_hasOneLot_withItsGainAt160() {
        assertEquals(
            listOf(
                LotRowModel(
                    key = "b2",
                    date = "2026-02-01",
                    left = "8",
                    perShare = "$150.00",
                    spokenPerShare = "150.00",
                    cost = "$1,200.00",
                    spokenCost = "1,200.00",
                    value = "$1,280.00",
                    spokenValue = "1,280.00",
                    gain = GainModel.Figure("+$80.00", SpokenFigure("80.00", FigureSign.PLUS)),
                    stale = false,
                ),
            ),
            HoldingDetail.lots(lots, price("160")),
        )
    }

    @Test
    fun aLot_withAStalePriceIsMarked_andWithNoPriceSaysSo() {
        assertEquals(true, HoldingDetail.lots(lots, price("160", stale = true)).single().stale)
        val unpriced = HoldingDetail.lots(lots, null).single()
        assertEquals(GainModel.NoPrice, unpriced.gain)
        assertEquals(false, unpriced.stale)
    }

    // CR5 (41), AC-69: a stale price still gives the lot's value at that price, with the stale mark.
    @Test
    fun aLotsValue_atAStalePrice_isAtThatPrice() {
        val stale = HoldingDetail.lots(lots, price("140", stale = true)).single()
        assertEquals("$1,120.00", stale.value)
        assertEquals("1,120.00", stale.spokenValue)
        assertEquals(true, stale.stale)
    }

    // CR5 (41), AC-69: a lot without a price is valued at its cost (4.8), and its gain says "no price".
    @Test
    fun aLotsValue_withNoPrice_isItsCost() {
        val unpriced = HoldingDetail.lots(lots, null).single()
        assertEquals("$1,200.00", unpriced.value)
        assertEquals("1,200.00", unpriced.spokenValue)
        assertEquals(GainModel.NoPrice, unpriced.gain)
    }

    // CR5 (41), 4.10: the value is the exact product, rounded once; a remainder that rounds up and one that rounds down.
    @Test
    fun aLotsValue_isRoundedOnce_upAndDown() {
        // A lot of 1 share for $100.00 after a 1 for 3 split: exactly 1/3 share left (Example I's shape).
        val oneThird = Fraction.of(BigInteger.ONE, BigInteger.valueOf(3))
        val third =
            Lot(
                buyId = "b9",
                portfolioId = "p1",
                ticker = "AAPL",
                buyDate = LocalDate.parse("2026-02-01"),
                enteredSeq = 9L,
                originalQuantity = BigDecimal.ONE,
                remainingQuantity = oneThird,
                cost = Fraction.ofCents(10_000),
                originalCost = Fraction.ofCents(10_000),
                splitFactor = oneThird,
            )
        // 1/3 × $100.005 = $33.335 → $33.34 (half away from zero, 4.10).
        assertEquals("$33.34", HoldingDetail.lots(listOf(third), price("100.005")).single().value)
        // 1/3 × $100.00 = $33.333… → $33.33.
        assertEquals("$33.33", HoldingDetail.lots(listOf(third), price("100")).single().value)
    }
}
