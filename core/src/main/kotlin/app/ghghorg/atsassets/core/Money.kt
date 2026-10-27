package app.ghghorg.atsassets.core

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
}
