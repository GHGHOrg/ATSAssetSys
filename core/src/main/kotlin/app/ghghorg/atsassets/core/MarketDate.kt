package app.ghghorg.atsassets.core

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** Dates are US market dates, New York time, with no time of day (spec C13 and 4.1). */
class MarketDate(
    private val clock: Clock,
) {
    /** Today in New York, whatever zone the clock or the phone is in. */
    fun today(): LocalDate = LocalDate.now(clock.withZone(ZONE))

    companion object {
        val ZONE: ZoneId = ZoneId.of("America/New_York")

        /** A real YYYY-MM-DD date (spaces around it are ignored), or null. */
        fun parse(text: String): LocalDate? = try {
            LocalDate.parse(text.trim())
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
