package app.ghghorg.atsassets.core

import java.time.LocalDate

/** The direction of an adjustment (spec 4.2: either sign). Other types take their sign from the type (04 decision 4). */
enum class CashDirection {
    IN,
    OUT,
}

/** Why an entry cannot be reversed (spec 4.2, 4.6). */
enum class NotReversibleReason {
    OPENING_BALANCE,
    TRADE,
    IS_REVERSAL,
    ALREADY_REVERSED,
}

/** Why a cash entry or a reversal was refused (spec 4.1, 4.2, 4.6). Reasons only; the wording is D9. */
sealed interface CashEntryRefusal {
    data object TypeNotAllowed : CashEntryRefusal

    data object NoOpeningBalance : CashEntryRefusal

    data object AmountNotPositive : CashEntryRefusal

    data object NoteHasLineBreak : CashEntryRefusal

    /** An adjustment needs a note; one of only spaces counts as missing (04 decision 1). */
    data object NoteRequired : CashEntryRefusal

    data class BeforeOpeningBalance(
        val openingDate: LocalDate,
    ) : CashEntryRefusal

    /** The first date the cash would be negative, and the shortfall on that date (03 decision 1). */
    data class NegativeCash(
        val date: LocalDate,
        val shortfallCents: Long,
    ) : CashEntryRefusal

    data object TooLarge : CashEntryRefusal

    data object OriginalNotFound : CashEntryRefusal

    data class NotReversible(
        val reason: NotReversibleReason,
    ) : CashEntryRefusal

    /** A reversal cannot be dated before its original (spec 4.6). */
    data class BeforeOriginal(
        val originalDate: LocalDate,
    ) : CashEntryRefusal
}

sealed interface CashEntryResult {
    data class Created(
        val entry: CashEntry,
    ) : CashEntryResult

    data class Refused(
        val reason: CashEntryRefusal,
    ) : CashEntryResult
}

/** K1: deposits, withdrawals (03), interest/dividend and adjustments (04) (spec 4.1, 4.2). */
object CashEntries {
    private val ALLOWED = setOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT)

    /**
     * [amountCents] is typed as a positive number. The sign comes from the type, or from [direction] for an
     * adjustment only. The first rule that fails decides the reason: type, opening balance, amount, note
     * (line break, then required), date, then the replay. The note is stored as typed.
     */
    fun create(
        existing: List<CashEntry>,
        id: String,
        type: CashType,
        date: LocalDate,
        amountCents: Long,
        note: String,
        direction: CashDirection = CashDirection.IN,
    ): CashEntryResult {
        val refusal =
            when {
                type !in ALLOWED -> CashEntryRefusal.TypeNotAllowed
                existing.none { it.type == CashType.OPENING_BALANCE } -> CashEntryRefusal.NoOpeningBalance
                amountCents <= 0L -> CashEntryRefusal.AmountNotPositive
                note.any { it == '\n' || it == '\r' } -> CashEntryRefusal.NoteHasLineBreak
                type == CashType.ADJUSTMENT && note.isBlank() -> CashEntryRefusal.NoteRequired
                else -> null
            }
        if (refusal != null) return CashEntryResult.Refused(refusal)
        val openingDate = existing.first { it.type == CashType.OPENING_BALANCE }.date
        if (date.isBefore(openingDate)) {
            return CashEntryResult.Refused(CashEntryRefusal.BeforeOpeningBalance(openingDate))
        }
        val outgoing = type == CashType.WITHDRAWAL || (type == CashType.ADJUSTMENT && direction == CashDirection.OUT)
        val signed = if (outgoing) -amountCents else amountCents
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
     * Reverses a direct cash entry (spec 4.6): an Adjustment with the opposite of the stored amount, linked by
     * reversesId. The first rule that fails decides the reason: found, reversible, note (line break, then
     * required: the reversal is an adjustment), date, then the replay. The note is stored as typed.
     */
    fun reverse(
        existing: List<CashEntry>,
        id: String,
        originalId: String,
        date: LocalDate,
        note: String,
    ): CashEntryResult {
        val original = existing.firstOrNull { it.id == originalId } ?: return CashEntryResult.Refused(CashEntryRefusal.OriginalNotFound)
        val notReversible =
            when {
                original.type == CashType.OPENING_BALANCE -> NotReversibleReason.OPENING_BALANCE
                original.type == CashType.TRADE -> NotReversibleReason.TRADE
                original.reversesId != null -> NotReversibleReason.IS_REVERSAL
                existing.any { it.reversesId == original.id } -> NotReversibleReason.ALREADY_REVERSED
                else -> null
            }
        val refusal =
            when {
                notReversible != null -> CashEntryRefusal.NotReversible(notReversible)
                note.any { it == '\n' || it == '\r' } -> CashEntryRefusal.NoteHasLineBreak
                note.isBlank() -> CashEntryRefusal.NoteRequired
                date.isBefore(original.date) -> CashEntryRefusal.BeforeOriginal(original.date)
                else -> null
            }
        if (refusal != null) return CashEntryResult.Refused(refusal)
        val opposite =
            try {
                Math.negateExact(original.amountCents)
            } catch (e: ArithmeticException) {
                return CashEntryResult.Refused(CashEntryRefusal.TooLarge)
            }
        val entry =
            CashEntry(
                id = id,
                date = date,
                amountCents = opposite,
                type = CashType.ADJUSTMENT,
                note = note,
                enteredSeq = CashRules.nextSeq(existing),
                reversesId = original.id,
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
