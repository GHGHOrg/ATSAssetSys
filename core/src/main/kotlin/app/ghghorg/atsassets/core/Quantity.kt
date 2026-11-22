package app.ghghorg.atsassets.core

import java.math.BigDecimal

/** Why a typed quantity was refused (spec 4.2, 4.3: above 0, up to 8 decimals). */
enum class QuantityError {
    EMPTY,
    BAD_FORMAT,
    TOO_MANY_DECIMALS,
    ZERO,
}

sealed interface QuantityResult {
    data class Exact(
        val value: BigDecimal,
    ) : QuantityResult

    data class Refused(
        val reason: QuantityError,
    ) : QuantityResult
}

/** Typed text to an exact decimal and back (spec 4.10, section 6 number format). No floating point. */
object Quantity {
    const val MAX_DECIMALS = 8

    private val QUANTITY = Regex("[0-9]+(?:\\.([0-9]+))?")

    /**
     * Digits and one dot only, up to 8 decimals, above 0, kept as typed. The first rule that fails decides the
     * reason: empty, bad format, too many decimals, zero.
     */
    fun parse(text: String): QuantityResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return QuantityResult.Refused(QuantityError.EMPTY)
        }
        val match = QUANTITY.matchEntire(trimmed) ?: return QuantityResult.Refused(QuantityError.BAD_FORMAT)
        if (match.groupValues[1].length > MAX_DECIMALS) {
            return QuantityResult.Refused(QuantityError.TOO_MANY_DECIMALS)
        }
        val value = BigDecimal(trimmed)
        if (value.signum() == 0) {
            return QuantityResult.Refused(QuantityError.ZERO)
        }
        return QuantityResult.Exact(value)
    }

    /** Commas between thousands, up to 8 decimals, trailing zeros dropped (10, 2.5). */
    fun format(quantity: BigDecimal): String = format(Fraction.of(quantity))

    private val SMALLEST = BigDecimal("0.00000001")

    /**
     * A quantity as it is shown, picked and judged: rounded half away from zero to 8 decimals, except that a quantity
     * above 0 that would round to 0 is 0.00000001, so a leftover never shows 0 (4.10, CR1).
     */
    fun shown(quantity: Fraction): BigDecimal {
        val rounded = quantity.rounded(MAX_DECIMALS)
        return if (quantity.signum() > 0 && rounded.signum() == 0) SMALLEST else rounded
    }

    /** As [format], rounded as [shown]. */
    fun format(quantity: Fraction): String {
        val plain = shown(quantity).stripTrailingZeros().toPlainString()
        val whole = plain.substringBefore('.')
        val decimals = plain.substringAfter('.', "")
        val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
        return if (decimals.isEmpty()) grouped else "$grouped.$decimals"
    }
}
