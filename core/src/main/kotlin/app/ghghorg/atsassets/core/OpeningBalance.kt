package app.ghghorg.atsassets.core

import java.time.LocalDate

enum class OpeningBalanceError {
    ALREADY_EXISTS,
    AMOUNT_NOT_POSITIVE,
    NOTE_HAS_LINE_BREAK,
}

sealed interface OpeningBalanceResult {
    data class Created(
        val entry: CashEntry,
    ) : OpeningBalanceResult

    data class Refused(
        val reason: OpeningBalanceError,
    ) : OpeningBalanceResult
}

/** STUB (iteration 02a, commit A). Commit B implements it. The rule of 4.2. */
object OpeningBalance {
    fun create(
        existing: List<CashEntry>,
        id: String,
        date: LocalDate,
        amountCents: Long,
        note: String,
    ): OpeningBalanceResult = TODO("commit B")
}

/** STUB (iteration 02a, commit A). Commit B implements it. Balance rules of spec section 3. */
object CashRules {
    /** The sum of the entries dated on or before [date]. */
    fun balanceOn(
        entries: List<CashEntry>,
        date: LocalDate,
    ): Long = TODO("commit B")

    /** "App holds data" (spec section 3); in 02a that means any cash entry exists. */
    fun appHoldsData(entries: List<CashEntry>): Boolean = TODO("commit B")
}
