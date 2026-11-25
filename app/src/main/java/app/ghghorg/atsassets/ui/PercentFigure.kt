package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import java.math.BigDecimal
import java.math.BigInteger

/*
 * A single percentage as the screens show it (spec section 6, number format; CR5 for the 1-day line, CR6 for the
 * unrealized gain/loss; CR2): 2 decimals, rounded once half away from zero (4.10), a comma between thousands, and the sign
 * of its own rounded value, so one that rounds to zero has none. Plain Kotlin: the screen puts these values in.
 */

/** "+6.67%", "−6.67%" or "0.00%", with its digits and sign for TalkBack. */
data class PercentFigure(
    val text: String,
    val spoken: SpokenFigure,
)

object Percent {
    private const val MINUS = "−"
    private val HUNDRED = Fraction.of(BigInteger.valueOf(100), BigInteger.ONE)

    /** [ratio] as a percentage (1/15 shows "+6.67%"); null when there is none. */
    fun of(ratio: Fraction?): PercentFigure? {
        if (ratio == null) return null
        val value = (ratio * HUNDRED).rounded(2)
        return PercentFigure(signed(value) + "%", SpokenFigure(digits(value), signOf(value)))
    }

    // "+1.27", "−1.48" or "0.00": a figure that rounds to zero has no sign (section 6).
    private fun signed(value: BigDecimal): String = when (value.signum()) {
        1 -> "+" + digits(value)
        -1 -> MINUS + digits(value)
        else -> digits(value)
    }

    // The magnitude with a comma between thousands and 2 decimals.
    private fun digits(value: BigDecimal): String {
        val plain = value.abs().setScale(2).toPlainString()
        val whole = plain.substringBefore('.').reversed().chunked(3).joinToString(",").reversed()
        return whole + "." + plain.substringAfter('.')
    }

    private fun signOf(value: BigDecimal): FigureSign = when (value.signum()) {
        1 -> FigureSign.PLUS
        -1 -> FigureSign.MINUS
        else -> FigureSign.NONE
    }
}
