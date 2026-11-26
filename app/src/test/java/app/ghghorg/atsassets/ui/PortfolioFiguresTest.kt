package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.DayChange
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

// 16a decision 1 (your A after step 6): a portfolio's figures at the latest prices, and "at cost" only where it is true.
class PortfolioFiguresTest {
    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    // 2026-10-29 16:00 New York, and the close a day earlier.
    private val close = Instant.ofEpochSecond(1_793_304_000L)
    private val dayBefore = Instant.ofEpochSecond(1_793_217_600L)

    private fun price(
        text: String,
        time: Instant = close,
        stale: Boolean = false,
    ): Price = Price(BigDecimal(text), time, stale)

    private val aapl = Holding("p1", "AAPL", dollars(8), dollars(1_200))
    private val msft = Holding("p1", "MSFT", dollars(1), dollars(400))
    private val oldco = Holding("p1", "OLDCO", Fraction.ZERO, Fraction.ZERO)

    private fun pct(
        text: String,
        digits: String,
        sign: FigureSign,
    ): PercentFigure = PercentFigure(text, SpokenFigure(digits, sign))

    // CR6 (43), AC-72: $80.00 over $1,200.00, and a loss of $80.00 over the priced cost of $1,200.00 (MSFT has no price).
    private val gain80 = GainModel.Figure("+$80.00", SpokenFigure("80.00", FigureSign.PLUS), pct("+6.67%", "6.67", FigureSign.PLUS))
    private val loss80 = GainModel.Figure("−$80.00", SpokenFigure("80.00", FigureSign.MINUS), pct("−6.67%", "6.67", FigureSign.MINUS))

    private val zero = GainModel.Figure("$0.00", SpokenFigure("0.00", FigureSign.NONE))

    // CR5 (41), AC-67: the portfolio's day line is the sum over its holdings that have a figure; one without adds nothing,
    // and with none at all there is no line. Each holding row carries its own; a fully sold one none (wireframe 03 marker 5).
    @Test
    fun theDayLine_sumsTheHoldingsThatHaveOne_andEachRowCarriesItsOwn() {
        val days =
            mapOf(
                ("p1" to "AAPL") to DayChange(dollars(16), dollars(1_264)),
                ("p1" to "MSFT") to null,
                ("p1" to "OLDCO") to DayChange(Fraction.ZERO, Fraction.ZERO),
            )
        val prices = mapOf("AAPL" to price("160"))
        val figures = PortfolioFigures.of(listOf(aapl, msft, oldco), prices, days)
        assertEquals(DayLineModel("+$16.00", SpokenFigure("16.00", FigureSign.PLUS), "+1.27%", SpokenFigure("1.27", FigureSign.PLUS)), figures.day)
        val rows = PortfolioFigures.rows(listOf(aapl, msft, oldco), prices, days)
        assertEquals(listOf("+$16.00", null, null), rows.map { it.day?.text })
        assertEquals(null, PortfolioFigures.of(listOf(msft), prices, days).day)
        assertEquals(null, PortfolioFigures.of(listOf(aapl), prices).day)
    }

    @Test
    fun exampleA_at160_isWorth1280_withAGainOf80() {
        assertEquals(
            PortfolioFiguresModel(
                value = "$1,280.00",
                spokenValue = "1,280.00",
                cost = "$1,200.00",
                spokenCost = "1,200.00",
                gain = gain80,
                allAtCost = false,
                prices = PricesAsOfModel("2026-10-29 16:00", someStale = false),
            ),
            PortfolioFigures.of(listOf(aapl), mapOf("AAPL" to price("160"))),
        )
    }

    // CR6 (43), AC-72: a "no price" holding (cost $800.00) is left out of the percentage, its cost as well: P shows
    // +$80.00 (+6.67%), not +4.00%; its shown cost stays every holding's, $2,000.00.
    @Test
    fun aNoPriceHolding_isLeftOutOfThePercentage() {
        val xyz = Holding("p1", "XYZ", dollars(5), dollars(800))
        val figures = PortfolioFigures.of(listOf(aapl, xyz), mapOf("AAPL" to price("160")))
        assertEquals(gain80, figures.gain)
        assertEquals("$2,000.00", figures.cost)
    }

    @Test
    fun withNoPrices_itIsAllAtCost_withAZeroGain() {
        val figures = PortfolioFigures.of(listOf(aapl, oldco), emptyMap())
        assertEquals("$1,200.00", figures.value)
        assertEquals(zero, figures.gain)
        assertEquals(true, figures.allAtCost)
    }

    @Test
    fun withSomePrices_onlyThePricedHoldingsMove_andItIsNotAllAtCost() {
        val figures = PortfolioFigures.of(listOf(aapl, msft), mapOf("AAPL" to price("140")))
        // AAPL 8 × $140 = $1,120.00, a loss of $80.00; MSFT at its cost of $400.00.
        assertEquals("$1,520.00", figures.value)
        assertEquals("$1,600.00", figures.cost)
        assertEquals(loss80, figures.gain)
        assertEquals(false, figures.allAtCost)
    }

    @Test
    fun anEmptyOrSoldOutPortfolio_isWorthNothing_andNotAtCost() {
        for (holdings in listOf(emptyList(), listOf(oldco))) {
            val figures = PortfolioFigures.of(holdings, emptyMap())
            assertEquals("$0.00", figures.value)
            assertEquals(zero, figures.gain)
            assertEquals(false, figures.allAtCost)
        }
    }

    @Test
    fun eachHoldingRow_showsItsValueAndGain_orNoPrice_orNothingWhenSoldOut() {
        assertEquals(
            listOf(
                PortfolioHoldingRowModel(
                    ticker = "AAPL",
                    shares = "8",
                    oneShare = false,
                    value = "$1,280.00",
                    spokenValue = "1,280.00",
                    gain = gain80,
                    soldOut = false,
                    staleAsOf = null,
                ),
                PortfolioHoldingRowModel("MSFT", "1", true, "$400.00", "400.00", GainModel.NoPrice, soldOut = false, staleAsOf = null),
                PortfolioHoldingRowModel("OLDCO", "0", false, "$0.00", "0.00", zero, soldOut = true, staleAsOf = null),
            ),
            PortfolioFigures.rows(listOf(aapl, msft, oldco), mapOf("AAPL" to price("160"), "OLDCO" to price("5"))),
        )
    }

    // Moved from core's HoldingsTest with Valuation.portfolioValue (16b decision 3, open item 52): each portfolio is worth
    // its own holdings only, at cost while there is no price (4.8).
    @Test
    fun eachPortfolio_isWorthItsOwnHoldingsOnly() {
        fun bought(
            id: String,
            portfolioId: String,
            ticker: String,
            quantity: String,
            totalCents: Long,
        ) = Transaction(
            id = id,
            portfolioId = portfolioId,
            ticker = ticker,
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 1, 2),
            enteredSeq = id.drop(1).toLong(),
            quantity = BigDecimal(quantity),
            totalCents = totalCents,
        )
        val state =
            AppState(
                transactions =
                listOf(
                    bought("b1", "p1", "AAPL", "10", 100_000L),
                    bought("b2", "p1", "SPY", "1", 50_000L),
                    bought("b3", "p2", "AAPL", "1", 70_000L),
                ),
                today = LocalDate.of(2026, 6, 1),
            )
        assertEquals(listOf("$1,500.00", "$700.00", "$0.00"), listOf("p1", "p2", "p3").map { state.portfolioFigures(it).value })
        // CR6 (43), wiring: with AAPL priced at $160, p1's gain is $600.00 over the priced cost of $1,000.00 (SPY has no
        // price and is left out of both), p2's −$540.00 over $700.00 (−77.142…%); p3 has no cost and no percentage.
        val priced = state.copy(instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = price("160"))))
        assertEquals(listOf("+60.00%", "−77.14%", null), listOf("p1", "p2", "p3").map { priced.portfolioFigures(it).gain.percent?.text })
    }

    // 16b: the age line is the oldest price used (4.8), in New York to the minute.
    @Test
    fun theAgeLine_isTheOldestPriceUsed() {
        val prices = mapOf("AAPL" to price("160"), "MSFT" to price("400", dayBefore))
        assertEquals(PricesAsOfModel("2026-10-28 16:00", someStale = false), PortfolioFigures.of(listOf(aapl, msft), prices).prices)
    }

    // 4.8: a holding valued at cost is not counted, and a sold-out holding uses no price.
    @Test
    fun theAgeLine_leavesOutHoldingsAtCostAndSoldOut_andIsMissingWhenNoPriceIsUsed() {
        val withOld = mapOf("AAPL" to price("160"), "OLDCO" to price("5", dayBefore))
        assertEquals(PricesAsOfModel("2026-10-29 16:00", someStale = false), PortfolioFigures.of(listOf(aapl, msft, oldco), withOld).prices)
        assertEquals(null, PortfolioFigures.of(listOf(msft, oldco), mapOf("OLDCO" to price("5"))).prices)
        assertEquals(null, PortfolioFigures.of(emptyList(), emptyMap()).prices)
    }

    // 16b decision 1: ", some stale" when any price used is stale; a sold-out holding's stale price is not used.
    @Test
    fun someStale_whenAnyPriceUsedIsStale_butNotASoldOutHoldings() {
        val oneStale = mapOf("AAPL" to price("160"), "MSFT" to price("400", dayBefore, stale = true))
        assertEquals(PricesAsOfModel("2026-10-28 16:00", someStale = true), PortfolioFigures.of(listOf(aapl, msft), oneStale).prices)
        val soldOutStale = mapOf("AAPL" to price("160"), "OLDCO" to price("5", dayBefore, stale = true))
        assertEquals(PricesAsOfModel("2026-10-29 16:00", someStale = false), PortfolioFigures.of(listOf(aapl, oldco), soldOutStale).prices)
    }

    // Wireframe 03 marker 6: "stale" with its age on a holding with shares; none on a fresh, unpriced or sold-out one.
    @Test
    fun aRow_withAStalePrice_carriesThatPricesOwnTime() {
        val prices = mapOf("AAPL" to price("160"), "MSFT" to price("400", dayBefore, stale = true), "OLDCO" to price("5", dayBefore, stale = true))
        assertEquals(
            listOf(null, "2026-10-28 16:00", null),
            PortfolioFigures.rows(listOf(aapl, msft, oldco), prices).map { it.staleAsOf },
        )
        assertEquals(listOf<String?>(null), PortfolioFigures.rows(listOf(msft), emptyMap()).map { it.staleAsOf })
    }
}
