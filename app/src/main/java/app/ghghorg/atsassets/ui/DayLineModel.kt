package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.DayChange
import app.ghghorg.atsassets.core.Fraction
import java.math.BigDecimal
import java.math.BigInteger

/*
 * The 1-day line as the screens draw it (CR5, 41; spec 4.8, section 6 number format and the 1-day line; AC-67; CR2).
 * Plain Kotlin: the screen picks the approved label and puts these values in.
 */

/**
 * "+$16.00" with its sign, which also gives the color (green, red or none, section 6), and "+1.27%", 2 decimals, or null
 * when the day's starting value is $0.00 (4.8). Each with its digits and sign for TalkBack.
 */
data class DayLineModel(
    val text: String,
    val spoken: SpokenFigure,
    val percent: String?,
    val spokenPercent: SpokenFigure?,
)

object DayLine {
    private const val MINUS = "−"
    private val HUNDRED = Fraction.of(BigInteger.valueOf(100), BigInteger.ONE)

    /** The line for [change], null when there is no day figure: the line is left out (4.8, C24). */
    fun of(change: DayChange?): DayLineModel? {
        if (change == null) return null
        val cents = change.change.rounded(2).movePointRight(2).longValueExact()
        val percent = change.percent?.let { (it * HUNDRED).rounded(2) }
        return DayLineModel(
            text = signedAmount(cents),
            spoken = SpokenFigure.ofAmount(cents),
            percent = percent?.let { signed(it) + "%" },
            spokenPercent = percent?.let { SpokenFigure(digits(it), signOf(it)) },
        )
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
