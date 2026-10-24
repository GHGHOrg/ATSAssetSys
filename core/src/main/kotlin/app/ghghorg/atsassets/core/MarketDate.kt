package app.ghghorg.atsassets.core

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** STUB (iteration 02a, commit A). Commit B implements it. Dates are New York dates (C13). */
class MarketDate(
    private val clock: Clock,
) {
    fun today(): LocalDate = TODO("commit B")

    companion object {
        val ZONE: ZoneId = ZoneId.of("America/New_York")

        /** A real YYYY-MM-DD date, or null. */
        fun parse(text: String): LocalDate? = TODO("commit B")
    }
}
