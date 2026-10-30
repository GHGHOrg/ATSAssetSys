package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class FractionTest {
    private fun fraction(
        numerator: Long,
        denominator: Long,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    @Test
    fun of_reducesByTheGreatestCommonDivisor() {
        val half = fraction(2, 4)
        assertEquals(BigInteger.ONE, half.numerator)
        assertEquals(BigInteger.valueOf(2), half.denominator)
    }

    @Test
    fun of_negativeDenominator_movesTheSignToTheNumerator() {
        val value = fraction(1, -3)
        assertEquals(BigInteger.valueOf(-1), value.numerator)
        assertEquals(BigInteger.valueOf(3), value.denominator)
    }

    @Test
    fun of_zero_isZeroOverOne() {
        assertEquals(Fraction.ZERO, fraction(0, 7))
        assertEquals(BigInteger.ONE, fraction(0, 7).denominator)
    }

    @Test(expected = IllegalArgumentException::class)
    fun of_zeroDenominator_throws() {
        fraction(1, 0)
    }

    @Test
    fun of_decimal_isExact() {
        assertEquals(fraction(5, 2), Fraction.of(BigDecimal("2.50")))
        assertEquals(fraction(1, 100_000_000), Fraction.of(BigDecimal("0.00000001")))
    }

    @Test
    fun ofCents_isDollars() {
        assertEquals(fraction(1_500, 1), Fraction.ofCents(150_000L))
        assertEquals(fraction(1, 100), Fraction.ofCents(1L))
    }

    @Test
    fun arithmetic_isExact() {
        assertEquals(fraction(5, 6), fraction(1, 2) + fraction(1, 3))
        assertEquals(fraction(1, 6), fraction(1, 2) - fraction(1, 3))
        assertEquals(fraction(1, 6), fraction(1, 2) * fraction(1, 3))
        assertEquals(fraction(3, 2), fraction(1, 2) / fraction(1, 3))
    }

    @Test
    fun compareTo_ordersByValue() {
        assertTrue(fraction(1, 3) < fraction(1, 2))
        assertTrue(fraction(-1, 2) < Fraction.ZERO)
        assertEquals(0, fraction(2, 4).compareTo(fraction(1, 2)))
    }

    @Test
    fun rounded_isHalfAwayFromZero() {
        assertEquals(BigDecimal("0.01"), fraction(1, 200).rounded(2))
        assertEquals(BigDecimal("-0.01"), fraction(-1, 200).rounded(2))
        assertEquals(BigDecimal("0.00"), fraction(1, 201).rounded(2))
        assertEquals(BigDecimal("333.33"), fraction(1_000, 3).rounded(2))
    }
}
