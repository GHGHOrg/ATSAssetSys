package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.math.BigInteger

/** Why a typed amount was refused (spec 4.2 and the number rules of section 8). */
enum class AmountError {
    EMPTY,
    BAD_FORMAT,
    TOO_MANY_DECIMALS,
    ZERO,
    TOO_LARGE,
}

sealed interface AmountResult {
    data class Cents(
        val value: Long,
    ) : AmountResult

    data class Refused(
        val reason: AmountError,
    ) : AmountResult
}

/** Typed text to integer cents and back (spec 4.10, section 6 number format). No floating point. */
object Money {
    private const val MINUS = "\u2212"
    private val AMOUNT = Regex("([0-9]+)(?:\\.([0-9]+))?")
    private val HUNDRED = BigInteger.valueOf(100)
    private val LARGEST = BigInteger.valueOf(Long.MAX_VALUE)

    /**
     * Digits and one dot only, up to 2 decimals, above 0. The first rule that fails decides the
     * reason: empty, bad format, too many decimals, too large, zero.
     */
    fun parseCents(text: String): AmountResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return AmountResult.Refused(AmountError.EMPTY)
        }
        val match = AMOUNT.matchEntire(trimmed) ?: return AmountResult.Refused(AmountError.BAD_FORMAT)
        val fraction = match.groupValues[2]
        if (fraction.length > 2) {
            return AmountResult.Refused(AmountError.TOO_MANY_DECIMALS)
        }
        val cents =
            BigInteger(match.groupValues[1])
                .multiply(HUNDRED)
                .add(BigInteger(fraction.padEnd(2, '0')))
        if (cents > LARGEST) {
            return AmountResult.Refused(AmountError.TOO_LARGE)
        }
        if (cents.signum() == 0) {
            return AmountResult.Refused(AmountError.ZERO)
        }
        return AmountResult.Cents(cents.toLong())
    }

    /** US style: a dollar sign, commas between thousands, 2 decimals, a minus sign before a negative. */
    fun format(cents: Long): String {
        val magnitude = BigInteger.valueOf(cents).abs()
        val dollars = magnitude.divide(HUNDRED).toString()
        val hundredths = magnitude.remainder(HUNDRED).toInt().toString().padStart(2, '0')
        val grouped = dollars.reversed().chunked(3).joinToString(",").reversed()
        val sign = if (cents < 0L) MINUS else ""
        return "$sign\$$grouped.$hundredths"
    }

    /**
     * An exact dollar value, rounded half away from zero to cents (4.10). A value that rounds to 0.00 shows $0.00
     * with no sign.
     */
    fun format(dollars: Fraction): String = formatDecimal(dollars.rounded(2))

    /** A price per share or cost per share (section 6): 2 decimals, or 4 when under $1. */
    fun formatPerShare(dollars: Fraction): String {
        val underOneDollar = dollars.abs() < Fraction.of(BigInteger.ONE, BigInteger.ONE)
        return formatDecimal(dollars.rounded(if (underOneDollar) 4 else 2))
    }

    /**
     * Parts shown beside the total they add up to, in cents, by largest remainder (4.10): each part rounded down to a
     * cent, and the cents left over to reach the rounded total given one each to the parts with the largest dropped
     * remainders, ties to the earlier row.
     */
    fun partsInCents(parts: List<Fraction>): List<Long> {
        val hundred = Fraction.of(HUNDRED, BigInteger.ONE)
        val scaled = parts.map { it * hundred }
        val floors = scaled.map { floorOf(it) }
        val total = parts.fold(Fraction.ZERO) { sum, part -> sum + part }.rounded(2).movePointRight(2).toBigIntegerExact()
        val leftover = (total - floors.fold(BigInteger.ZERO) { sum, floor -> sum + floor }).toInt()
        val remainders = scaled.indices.map { scaled[it] - Fraction.of(floors[it], BigInteger.ONE) }
        val topped = scaled.indices.sortedWith(compareByDescending<Int> { remainders[it] }.thenBy { it }).take(leftover).toSet()
        return floors.mapIndexed { index, floor -> (if (index in topped) floor + BigInteger.ONE else floor).longValueExact() }
    }

    private fun floorOf(value: Fraction): BigInteger {
        val (quotient, remainder) = value.numerator.divideAndRemainder(value.denominator)
        return if (remainder.signum() < 0) quotient - BigInteger.ONE else quotient
    }

    private fun formatDecimal(value: BigDecimal): String {
        val plain = value.abs().toPlainString()
        val grouped = plain.substringBefore('.').reversed().chunked(3).joinToString(",").reversed()
        val sign = if (value.signum() < 0) MINUS else ""
        return "$sign\$$grouped.${plain.substringAfter('.')}"
    }
}
