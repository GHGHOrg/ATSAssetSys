package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

// 43 step 3 (CR6; section 6, number format): a percentage as shown, 2 decimals, rounded once half away from zero (4.10).
class PercentFigureTest {
    private fun fraction(
        numerator: Long,
        denominator: Long = 1,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    // AC-72: 1/15 = 6.666…% shows 6.67% (rounded up); a loss carries the minus sign.
    @Test
    fun exampleA_isPlus6Point67_andALossMinus() {
        assertEquals(PercentFigure("+6.67%", SpokenFigure("6.67", FigureSign.PLUS)), Percent.of(fraction(1, 15)))
        assertEquals(PercentFigure("−6.67%", SpokenFigure("6.67", FigureSign.MINUS)), Percent.of(fraction(-1, 15)))
    }

    // AC-14: 3/5 shows +60.00%.
    @Test
    fun aWholePercentage_keeps2Decimals() {
        assertEquals("+60.00%", Percent.of(fraction(3, 5))!!.text)
    }

    // Rounded once (4.10): 1/3 = 33.333…% down to 33.33%, 2/3 = 66.666…% up to 66.67%, 1/800 = 0.125% half up to 0.13%.
    @Test
    fun rounding_upDownAndHalf() {
        assertEquals("+33.33%", Percent.of(fraction(1, 3))!!.text)
        assertEquals("+66.67%", Percent.of(fraction(2, 3))!!.text)
        assertEquals("+0.13%", Percent.of(fraction(1, 800))!!.text)
        assertEquals("−0.13%", Percent.of(fraction(-1, 800))!!.text)
    }

    // Section 6: a percentage that rounds to zero has no sign, as the dollar figure has none at $0.00.
    @Test
    fun aPercentageThatRoundsToZero_hasNoSign() {
        assertEquals(PercentFigure("0.00%", SpokenFigure("0.00", FigureSign.NONE)), Percent.of(fraction(1, 1_000_000)))
        assertEquals(PercentFigure("0.00%", SpokenFigure("0.00", FigureSign.NONE)), Percent.of(Fraction.ZERO))
    }

    // A large one is grouped by thousands; none gives none.
    @Test
    fun aLargePercentage_isGrouped_andNoneIsNone() {
        assertEquals("+1,250.00%", Percent.of(fraction(25, 2))!!.text)
        assertEquals(null, Percent.of(null))
    }
}
