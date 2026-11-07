package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Holdings
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
        assertEquals("0.0089", model.price!!.spokenPerShare)
        assertEquals(true, model.oneShare)
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
}
