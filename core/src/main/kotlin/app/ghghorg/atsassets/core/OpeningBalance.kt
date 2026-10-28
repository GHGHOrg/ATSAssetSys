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

/** The opening balance rule of spec 4.2 (K1). Any date is allowed, including a future one. */
object OpeningBalance {
    /** The first rule that fails decides the reason: already exists, amount, note. */
    fun create(
        existing: List<CashEntry>,
        id: String,
        date: LocalDate,
        amountCents: Long,
        note: String,
    ): OpeningBalanceResult {
        if (existing.any { it.type == CashType.OPENING_BALANCE }) {
            return OpeningBalanceResult.Refused(OpeningBalanceError.ALREADY_EXISTS)
        }
        if (amountCents <= 0L) {
            return OpeningBalanceResult.Refused(OpeningBalanceError.AMOUNT_NOT_POSITIVE)
        }
        if (note.any { it == '\n' || it == '\r' }) {
            return OpeningBalanceResult.Refused(OpeningBalanceError.NOTE_HAS_LINE_BREAK)
        }
        val nextSeq = (existing.maxOfOrNull { it.enteredSeq } ?: 0L) + 1L
        val entry =
            CashEntry(
                id = id,
                date = date,
                amountCents = amountCents,
                type = CashType.OPENING_BALANCE,
                note = note,
                enteredSeq = nextSeq,
            )
        return OpeningBalanceResult.Created(entry)
    }
}

/** Balance rules of spec section 3. */
object CashRules {
    /** The sum of the entries dated on or before [date]. Throws ArithmeticException instead of wrapping (4.10). */
    fun balanceOn(
        entries: List<CashEntry>,
        date: LocalDate,
    ): Long = sumExact(entries.filter { !it.date.isAfter(date) })

    /** The exact sum of the amounts. Throws ArithmeticException if it would leave the range of Long (4.10). */
    fun sumExact(entries: List<CashEntry>): Long = entries.fold(0L) { sum, entry -> Math.addExact(sum, entry.amountCents) }

    /** "App holds data" (spec section 3); in 02a that means any cash entry exists. */
    fun appHoldsData(entries: List<CashEntry>): Boolean = entries.isNotEmpty()
}
