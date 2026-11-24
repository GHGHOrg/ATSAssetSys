package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.DayChange
import app.ghghorg.atsassets.core.Fraction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

// 41 step 7 (CR5, AC-67): the 1-day line's figures as shown (section 6: dollars rounded once, the percentage with 2 decimals).
class DayLineModelTest {
    private fun fraction(
        numerator: Long,
        denominator: Long = 1,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    @Test
    fun exampleA_isPlus16_andPlus1Point27Percent() {
        assertEquals(
            DayLineModel("+$16.00", SpokenFigure("16.00", FigureSign.PLUS), "+1.27%", SpokenFigure("1.27", FigureSign.PLUS)),
            DayLine.of(DayChange(fraction(16), fraction(1_264))),
        )
    }

    // A loss: the minus sign of section 6 on both figures.
    @Test
    fun aLoss_hasTheMinusSign_onBoth() {
        assertEquals(
            DayLineModel("−$6.00", SpokenFigure("6.00", FigureSign.MINUS), "−1.48%", SpokenFigure("1.48", FigureSign.MINUS)),
            DayLine.of(DayChange(fraction(-6), fraction(406))),
        )
    }

    // Rounded half away from zero (4.10): 9/791 = 1.1378…% shows 1.14%; 1/800 = 0.125% shows 0.13%.
    @Test
    fun thePercentage_isRoundedHalfAwayFromZero_to2Decimals() {
        assertEquals("+1.14%", DayLine.of(DayChange(fraction(18), fraction(1_582)))!!.percent)
        assertEquals("+0.13%", DayLine.of(DayChange(fraction(1), fraction(800)))!!.percent)
        assertEquals("−0.13%", DayLine.of(DayChange(fraction(-1), fraction(800)))!!.percent)
    }

    // Section 6: a figure that rounds to zero shows with no sign and no color.
    @Test
    fun aFigureThatRoundsToZero_hasNoSign() {
        val tiny = DayLine.of(DayChange(fraction(1, 1_000), fraction(1_000_000)))!!
        assertEquals("$0.00", tiny.text)
        assertEquals(FigureSign.NONE, tiny.spoken.sign)
        assertEquals("0.00%", tiny.percent)
        assertEquals(FigureSign.NONE, tiny.spokenPercent!!.sign)
    }

    // 4.8: a start of $0.00 (a sold-out holding) has no percentage; no figure at all has no line.
    @Test
    fun aZeroStart_hasNoPercentage_andNoFigureHasNoLine() {
        val soldOut = DayLine.of(DayChange(Fraction.ZERO, Fraction.ZERO))!!
        assertEquals("$0.00", soldOut.text)
        assertEquals(null, soldOut.percent)
        assertEquals(null, soldOut.spokenPercent)
        assertEquals(null, DayLine.of(null))
    }

    // Thousands in the percentage: a big day over a small start.
    @Test
    fun aLargePercentage_isGroupedByThousands() {
        assertEquals("+1,250.00%", DayLine.of(DayChange(fraction(125), fraction(10)))!!.percent)
    }
}
