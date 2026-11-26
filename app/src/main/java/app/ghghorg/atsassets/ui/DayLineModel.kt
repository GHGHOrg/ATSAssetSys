package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.DayChange

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
    /** The line for [change], null when there is no day figure: the line is left out (4.8, C24). */
    fun of(change: DayChange?): DayLineModel? {
        if (change == null) return null
        val cents = change.change.rounded(2).movePointRight(2).longValueExact()
        // CR6 (43): the percentage is formatted as the unrealized gain/loss's is, in one place (PercentFigure.kt).
        val percent = Percent.of(change.percent)
        return DayLineModel(
            text = signedAmount(cents),
            spoken = SpokenFigure.ofAmount(cents),
            percent = percent?.text,
            spokenPercent = percent?.spoken,
        )
    }
}
