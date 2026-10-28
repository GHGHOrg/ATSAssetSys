package app.ghghorg.atsassets.core

import java.time.LocalDate

/** Why a deposit or withdrawal was refused (spec 4.1, 4.2). Reasons only; the wording is D9. */
sealed interface CashEntryRefusal {
    data object TypeNotAllowed : CashEntryRefusal

    data object NoOpeningBalance : CashEntryRefusal

    data object AmountNotPositive : CashEntryRefusal

    data object NoteHasLineBreak : CashEntryRefusal

    data class BeforeOpeningBalance(
        val openingDate: LocalDate,
    ) : CashEntryRefusal

    /** The first date the cash would be negative, and the shortfall on that date (03 decision 1). */
    data class NegativeCash(
        val date: LocalDate,
        val shortfallCents: Long,
    ) : CashEntryRefusal

    data object TooLarge : CashEntryRefusal
}

sealed interface CashEntryResult {
    data class Created(
        val entry: CashEntry,
    ) : CashEntryResult

    data class Refused(
        val reason: CashEntryRefusal,
    ) : CashEntryResult
}

/** K1: deposits and withdrawals (spec 4.1, 4.2). Interest/dividend and adjustment arrive in 04. */
object CashEntries {
    private val ALLOWED = setOf(CashType.DEPOSIT, CashType.WITHDRAWAL)

    /**
     * [amountCents] is typed as a positive number; the sign comes from the type. The first rule that
     * fails decides the reason: type, opening balance, amount, note, date, then the replay.
     */
    fun create(
        existing: List<CashEntry>,
        id: String,
        type: CashType,
        date: LocalDate,
        amountCents: Long,
        note: String,
    ): CashEntryResult {
        val refusal =
            when {
                type !in ALLOWED -> CashEntryRefusal.TypeNotAllowed
                existing.none { it.type == CashType.OPENING_BALANCE } -> CashEntryRefusal.NoOpeningBalance
                amountCents <= 0L -> CashEntryRefusal.AmountNotPositive
                note.any { it == '\n' || it == '\r' } -> CashEntryRefusal.NoteHasLineBreak
                else -> null
            }
        if (refusal != null) return CashEntryResult.Refused(refusal)
        val openingDate = existing.first { it.type == CashType.OPENING_BALANCE }.date
        if (date.isBefore(openingDate)) {
            return CashEntryResult.Refused(CashEntryRefusal.BeforeOpeningBalance(openingDate))
        }
        val signed = if (type == CashType.WITHDRAWAL) -amountCents else amountCents
        val entry =
            CashEntry(
                id = id,
                date = date,
                amountCents = signed,
                type = type,
                note = note,
                enteredSeq = CashRules.nextSeq(existing),
            )
        val replayed = replay(existing + entry, from = date)
        return if (replayed == null) CashEntryResult.Created(entry) else CashEntryResult.Refused(replayed)
    }

    /**
     * Replays the cash balance in the 4.1 order (date, then entered_seq) and judges it after every entry
     * dated [from] or later (03 decision 5). Null when every entry passes.
     */
    private fun replay(
        entries: List<CashEntry>,
        from: LocalDate,
    ): CashEntryRefusal? {
        var balance = 0L
        try {
            for (entry in entries.sortedWith(compareBy<CashEntry>({ it.date }, { it.enteredSeq }))) {
                balance = Math.addExact(balance, entry.amountCents)
                if (!entry.date.isBefore(from) && balance < 0L) {
                    return CashEntryRefusal.NegativeCash(entry.date, -balance)
                }
            }
        } catch (e: ArithmeticException) {
            return CashEntryRefusal.TooLarge
        }
        return null
    }
}
