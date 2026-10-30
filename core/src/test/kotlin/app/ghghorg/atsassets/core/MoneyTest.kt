package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class MoneyTest {
    private fun cents(text: String): AmountResult = Money.parseCents(text)

    private fun refused(reason: AmountError): AmountResult = AmountResult.Refused(reason)

    @Test
    fun parseCents_wholeDollars_returnsCents() {
        assertEquals(AmountResult.Cents(1_000_000L), cents("10000"))
    }

    @Test
    fun parseCents_twoDecimals_returnsCents() {
        assertEquals(AmountResult.Cents(1_000_000L), cents("10000.00"))
        assertEquals(AmountResult.Cents(1L), cents("0.01"))
    }

    @Test
    fun parseCents_oneDecimal_meansTenths() {
        assertEquals(AmountResult.Cents(1_000_050L), cents("10000.5"))
    }

    @Test
    fun parseCents_leadingZeros_areAccepted() {
        assertEquals(AmountResult.Cents(750L), cents("007.50"))
    }

    @Test
    fun parseCents_spacesAroundText_areTrimmed() {
        assertEquals(AmountResult.Cents(1_234L), cents("  12.34 "))
    }

    @Test
    fun parseCents_largestAmount_isAccepted() {
        assertEquals(AmountResult.Cents(Long.MAX_VALUE), cents("92233720368547758.07"))
    }

    @Test
    fun parseCents_oneCentOverLargest_isTooLarge() {
        assertEquals(refused(AmountError.TOO_LARGE), cents("92233720368547758.08"))
        assertEquals(refused(AmountError.TOO_LARGE), cents("99999999999999999999"))
    }

    @Test
    fun parseCents_empty_isRefused() {
        assertEquals(refused(AmountError.EMPTY), cents(""))
        assertEquals(refused(AmountError.EMPTY), cents("   "))
    }

    @Test
    fun parseCents_zero_isRefused() {
        assertEquals(refused(AmountError.ZERO), cents("0"))
        assertEquals(refused(AmountError.ZERO), cents("0.00"))
        assertEquals(refused(AmountError.ZERO), cents("00"))
    }

    @Test
    fun parseCents_thirdDecimal_isRefused() {
        assertEquals(refused(AmountError.TOO_MANY_DECIMALS), cents("10.123"))
        assertEquals(refused(AmountError.TOO_MANY_DECIMALS), cents("10.000"))
    }

    @Test
    fun parseCents_thirdDecimalOnZero_isTooManyDecimalsNotZero() {
        assertEquals(refused(AmountError.TOO_MANY_DECIMALS), cents("0.001"))
    }

    @Test
    fun parseCents_badCharacters_areRefusedAsBadFormat() {
        val inputs =
            listOf(
                "\$10,000.00",
                "10,000",
                "-5",
                "+5",
                "abc",
                "1e3",
                "10.",
                ".5",
                ".",
                "1.2.3",
                "1 000",
                "\u0661\u0662",
            )
        for (text in inputs) {
            assertEquals(text, refused(AmountError.BAD_FORMAT), cents(text))
        }
    }

    @Test
    fun format_wholeAndSmallAmounts() {
        assertEquals("\$10,000.00", Money.format(1_000_000L))
        assertEquals("\$0.00", Money.format(0L))
        assertEquals("\$0.01", Money.format(1L))
        assertEquals("\$0.05", Money.format(5L))
        assertEquals("\$1,234,567.89", Money.format(123_456_789L))
    }

    @Test
    fun format_negative_putsMinusSignBeforeDollarSign() {
        assertEquals("\u2212\$200.00", Money.format(-20_000L))
    }

    @Test
    fun format_limits_doNotOverflow() {
        assertEquals("\$92,233,720,368,547,758.07", Money.format(Long.MAX_VALUE))
        assertEquals("\u2212\$92,233,720,368,547,758.08", Money.format(Long.MIN_VALUE))
    }

    private fun dollars(
        numerator: Long,
        denominator: Long,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    @Test
    fun format_fraction_roundsHalfAwayFromZeroToCents() {
        assertEquals("\$100.01", Money.format(dollars(20_001, 200)))
        assertEquals("\$333.33", Money.format(dollars(1_000, 3)))
        assertEquals("\u2212\$0.01", Money.format(dollars(-1, 200)))
    }

    @Test
    fun format_fractionThatRoundsToZero_hasNoSign() {
        assertEquals("\$0.00", Money.format(dollars(-1, 1_000)))
    }

    @Test
    fun formatPerShare_isTwoDecimalsFromOneDollar() {
        assertEquals("\$150.00", Money.formatPerShare(dollars(150, 1)))
        assertEquals("\$333.33", Money.formatPerShare(dollars(1_000, 3)))
        assertEquals("\$1,234.50", Money.formatPerShare(dollars(2_469, 2)))
        assertEquals("\$1.00", Money.formatPerShare(dollars(1, 1)))
    }

    @Test
    fun formatPerShare_isFourDecimalsUnderOneDollar() {
        assertEquals("\$0.5000", Money.formatPerShare(dollars(1, 2)))
        assertEquals("\$0.3333", Money.formatPerShare(dollars(1, 3)))
        assertEquals("\$0.0089", Money.formatPerShare(dollars(89, 10_000)))
    }
}
