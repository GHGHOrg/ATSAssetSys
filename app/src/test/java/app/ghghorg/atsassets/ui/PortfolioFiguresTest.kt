package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Price
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

// 16a decision 1 (your A after step 6): a portfolio's figures at the latest prices, and "at cost" only where it is true.
class PortfolioFiguresTest {
    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private fun price(text: String): Price = Price(BigDecimal(text), Instant.ofEpochSecond(1_793_304_000L))

    private val aapl = Holding("p1", "AAPL", dollars(8), dollars(1_200))
    private val msft = Holding("p1", "MSFT", dollars(1), dollars(400))
    private val oldco = Holding("p1", "OLDCO", Fraction.ZERO, Fraction.ZERO)

    private val zero = GainModel.Figure("$0.00", SpokenFigure("0.00", FigureSign.NONE))

    @Test
    fun exampleA_at160_isWorth1280_withAGainOf80() {
        assertEquals(
            PortfolioFiguresModel(
                value = "$1,280.00",
                spokenValue = "1,280.00",
                cost = "$1,200.00",
                spokenCost = "1,200.00",
                gain = GainModel.Figure("+$80.00", SpokenFigure("80.00", FigureSign.PLUS)),
                allAtCost = false,
            ),
            PortfolioFigures.of(listOf(aapl), mapOf("AAPL" to price("160"))),
        )
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
        assertEquals(GainModel.Figure("−$80.00", SpokenFigure("80.00", FigureSign.MINUS)), figures.gain)
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
                    gain = GainModel.Figure("+$80.00", SpokenFigure("80.00", FigureSign.PLUS)),
                    soldOut = false,
                ),
                PortfolioHoldingRowModel("MSFT", "1", true, "$400.00", "400.00", GainModel.NoPrice, soldOut = false),
                PortfolioHoldingRowModel("OLDCO", "0", false, "$0.00", "0.00", zero, soldOut = true),
            ),
            PortfolioFigures.rows(listOf(aapl, msft, oldco), mapOf("AAPL" to price("160"), "OLDCO" to price("5"))),
        )
    }
}
