package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.MarketDate
import java.time.LocalDate

/**
 * The digits-only date input of the Cash filters (iteration 06, after phone check 12): eight digits, YYYYMMDD,
 * shown with the dashes of YYYY-MM-DD as they arrive. A text with any other character is shown as typed and
 * refused at once. Plain Kotlin, so the rules are tested on the JVM; the Compose field only applies them.
 */
object DateDigits {
    const val LENGTH = 8

    // After the year (4 digits) and the month (2 more) a dash is shown.
    private const val YEAR = 4
    private const val YEAR_MONTH = 6

    fun isDigits(text: String): Boolean = text.all { it in '0'..'9' }

    /** The shown text: dashes inserted only when the text is all digits. */
    fun format(text: String): String {
        if (!isDigits(text)) return text
        return buildString {
            text.forEachIndexed { index, digit ->
                if (index == YEAR || index == YEAR_MONTH) append('-')
                append(digit)
            }
        }
    }

    /** A cursor position in the typed text, as a position in the shown text. */
    fun toShownOffset(
        offset: Int,
        text: String,
    ): Int {
        if (!isDigits(text)) return offset
        val dashes =
            when {
                offset <= YEAR -> 0
                offset <= YEAR_MONTH -> 1
                else -> 2
            }
        return minOf(offset + dashes, format(text).length)
    }

    /** A cursor position in the shown text, as a position in the typed text. */
    fun toTypedOffset(
        offset: Int,
        text: String,
    ): Int {
        if (!isDigits(text)) return offset
        val dashes =
            when {
                offset <= YEAR -> 0
                offset <= YEAR_MONTH + 1 -> 1
                else -> 2
            }
        return minOf(offset - dashes, text.length)
    }

    /** The date, or null unless the text is eight digits of a real date. */
    fun toDate(text: String): LocalDate? {
        if (text.length != LENGTH || !isDigits(text)) return null
        return MarketDate.parse(format(text))
    }

    /** NOT_DIGITS at once for any other character; NOT_A_DATE only for eight digits that are no real date. */
    fun refusal(text: String): DateRefusal? = when {
        !isDigits(text) -> DateRefusal.NOT_DIGITS
        text.length == LENGTH && toDate(text) == null -> DateRefusal.NOT_A_DATE
        else -> null
    }
}
