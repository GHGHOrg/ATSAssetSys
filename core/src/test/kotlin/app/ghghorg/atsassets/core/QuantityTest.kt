package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class QuantityTest {
    private fun parse(text: String): QuantityResult = Quantity.parse(text)

    private fun exact(text: String): QuantityResult = QuantityResult.Exact(BigDecimal(text))

    private fun refused(reason: QuantityError): QuantityResult = QuantityResult.Refused(reason)

    private fun fraction(
        numerator: Long,
        denominator: Long,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    @Test
    fun parse_wholeNumber_isExact() {
        assertEquals(exact("10"), parse("10"))
    }

    @Test
    fun parse_eightDecimals_isAccepted() {
        assertEquals(exact("0.00000001"), parse("0.00000001"))
        assertEquals(exact("3.33333333"), parse("3.33333333"))
    }

    @Test
    fun parse_keepsTheDecimalsAsTyped() {
        val result = parse("2.50") as QuantityResult.Exact
        assertEquals(2, result.value.scale())
    }

    @Test
    fun parse_spacesAroundText_areTrimmed() {
        assertEquals(exact("2.5"), parse("  2.5 "))
    }

    @Test
    fun parse_ninthDecimal_isRefused() {
        assertEquals(refused(QuantityError.TOO_MANY_DECIMALS), parse("1.123456789"))
        assertEquals(refused(QuantityError.TOO_MANY_DECIMALS), parse("0.000000000"))
    }

    @Test
    fun parse_zero_isRefused() {
        assertEquals(refused(QuantityError.ZERO), parse("0"))
        assertEquals(refused(QuantityError.ZERO), parse("0.00000000"))
    }

    @Test
    fun parse_empty_isRefused() {
        assertEquals(refused(QuantityError.EMPTY), parse(""))
        assertEquals(refused(QuantityError.EMPTY), parse("   "))
    }

    @Test
    fun parse_badCharacters_areRefusedAsBadFormat() {
        val inputs = listOf("-1", "+1", "1,000", "1e3", "10.", ".5", ".", "1.2.3", "1 000", "abc", "١٢")
        for (text in inputs) {
            assertEquals(text, refused(QuantityError.BAD_FORMAT), parse(text))
        }
    }

    @Test
    fun format_dropsTrailingZerosAndGroupsThousands() {
        assertEquals("10", Quantity.format(BigDecimal("10")))
        assertEquals("2.5", Quantity.format(BigDecimal("2.50")))
        assertEquals("1,234,567.12345678", Quantity.format(BigDecimal("1234567.12345678")))
        assertEquals("100", Quantity.format(BigDecimal("100.00000000")))
    }

    @Test
    fun format_fraction_roundsHalfAwayFromZeroToEightPlaces() {
        assertEquals("3.33333333", Quantity.format(fraction(10, 3)))
        assertEquals("6.66666667", Quantity.format(fraction(20, 3)))
    }
}
