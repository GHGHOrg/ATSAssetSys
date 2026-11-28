package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.MarketDate
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType

/*
 * The cash entry form, its review, the cash entry detail and the cash reversal as they are drawn (wireframes 10b and
 * 10c; CR2, open item 48; 44a step 8 group (d) part 2): every shown date, name and figure is worked out here; the screens
 * pick the approved string and sign a cents figure as every model's cash effect is signed. Plain Kotlin. The form's
 * messages already have their model (CashFormMessages, 15a).
 */

/** A cash entry as shown on a banner, a detail or a reversal: its type, its date and its signed amount in cents. */
data class CashEntryShown(
    val type: CashType,
    val date: String,
    val amountCents: Long,
)

/** Which title the form has (10b frames A to C; 14b): an entry, the opening balance, or the replacement of an entry. */
enum class CashFormTitle { ENTRY, OPENING, REPLACING }

/**
 * The originals under the fields that differ from the replaced entry (10c marker 2; 14b): its type, date, direction (an
 * adjustment's), amount without its sign, and note, "" when it had none; null where a field matches.
 */
data class CashOriginals(
    val type: CashType? = null,
    val date: OriginalValue? = null,
    val direction: CashDirection? = null,
    val amount: OriginalValue? = null,
    val note: String? = null,
)

/** The cash entry form (wireframe 10b frames A to C; 10c): its title, the replaced entry's banner, and the originals. */
data class CashFormModel(
    val title: CashFormTitle,
    val banner: CashEntryShown?,
    val originals: CashOriginals,
)

object CashForms {
    fun of(
        form: FormState,
        original: CashEntry?,
    ): CashFormModel = CashFormModel(
        title =
        when {
            original != null -> CashFormTitle.REPLACING
            form.type == CashType.OPENING_BALANCE -> CashFormTitle.OPENING
            else -> CashFormTitle.ENTRY
        },
        banner = original?.let { shown(it) },
        originals = original?.let { originals(form, it) } ?: CashOriginals(),
    )

    fun shown(entry: CashEntry): CashEntryShown = CashEntryShown(entry.type, entry.date.toString(), entry.amountCents)

    private fun originals(
        form: FormState,
        original: CashEntry,
    ): CashOriginals {
        val was = if (original.amountCents < 0L) CashDirection.OUT else CashDirection.IN
        val unsigned = Math.absExact(original.amountCents)
        val typed = (Money.parseCents(form.amountText) as? AmountResult.Cents)?.value
        return CashOriginals(
            type = original.type.takeIf { it != form.type },
            date = if (MarketDate.parse(form.dateText.trim()) == original.date) null else OriginalValue(original.date.toString()),
            // An adjustment's direction, only while both are adjustments and it differs.
            direction = was.takeIf { original.type == CashType.ADJUSTMENT && form.type == CashType.ADJUSTMENT && form.direction != it },
            amount = if (typed == unsigned) null else OriginalValue(Money.format(unsigned), dollars = true),
            note = original.note.takeIf { it != form.noteText },
        )
    }
}

/**
 * The cash review (wireframe 10b frame D): the type, the date as it will be saved, the amount with its sign from the type
 * or an adjustment's direction (null while not valid), and the note ("" for none).
 */
data class CashReviewModel(
    val type: CashType,
    val date: String,
    val cashCents: Long?,
    val note: String,
)

object CashReviews {
    fun of(form: FormState): CashReviewModel {
        val cents = (Money.parseCents(form.amountText) as? AmountResult.Cents)?.value
        // The amount is typed positive; the sign comes from the type, or the direction on an adjustment (frame D).
        val outgoing = form.type == CashType.WITHDRAWAL || (form.type == CashType.ADJUSTMENT && form.direction == CashDirection.OUT)
        return CashReviewModel(
            type = form.type,
            date = MarketDate.parse(form.dateText)?.toString() ?: form.dateText.trim(),
            cashCents = cents?.let { if (outgoing) -it else it },
            note = form.noteText,
        )
    }
}

/** The title of a cash entry's detail (10b frame H; 13a wording 3): its type, or a trade line's buy or sell and ticker. */
sealed interface CashDetailTitle {
    data class Entry(
        val type: CashType,
    ) : CashDetailTitle

    data class Trade(
        val title: TradeTitle,
        val ticker: String,
        val portfolio: String,
    ) : CashDetailTitle
}

/** The detail's one status line or link (10b frame H; 05; 13a): what the entry is to its reversal chain. */
sealed interface CashDetailStatus {
    object OpeningNotReversible : CashDetailStatus

    /** A trade line links to its transaction; a buy's or sell's line first says to open it to reverse it. */
    data class TradeLine(
        val hint: Boolean,
        val title: TradeTitle,
        val ticker: String,
        val date: String,
    ) : CashDetailStatus

    data class ReversedBy(
        val id: String,
        val date: String,
    ) : CashDetailStatus

    data class Reverses(
        val id: String,
        val type: CashType,
        val date: String,
    ) : CashDetailStatus

    object NotReversed : CashDetailStatus
}

/** The cash entry detail (wireframe 10b frame H): the title, the entry's rows, its status, and the buttons offered. */
data class CashDetailModel(
    val title: CashDetailTitle,
    val entry: CashEntryShown,
    val note: String,
    val status: CashDetailStatus,
    val reverseOffered: Boolean,
    val reenterOffered: Boolean,
)

object CashDetails {
    fun of(
        entry: CashEntry,
        reversedBy: CashEntry?,
        reverses: CashEntry?,
        trade: Transaction?,
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        reverseOffered: Boolean,
        reenterOffered: Boolean,
    ): CashDetailModel {
        val tradeTitle = trade?.let { CashList.titleOf(it, transactions.associateBy { t -> t.id }) }
        return CashDetailModel(
            title =
            if (trade != null && tradeTitle != null) {
                CashDetailTitle.Trade(tradeTitle, trade.ticker, portfolios.firstOrNull { it.id == trade.portfolioId }?.name.orEmpty())
            } else {
                CashDetailTitle.Entry(entry.type)
            },
            entry = CashForms.shown(entry),
            note = entry.note,
            status =
            when {
                entry.type == CashType.OPENING_BALANCE -> CashDetailStatus.OpeningNotReversible

                trade != null && tradeTitle != null ->
                    // A reversal's line only links to it (frame H: a reversal shows the link to the other entry).
                    CashDetailStatus.TradeLine(trade.type != TransactionType.REVERSAL, tradeTitle, trade.ticker, trade.date.toString())

                reversedBy != null -> CashDetailStatus.ReversedBy(reversedBy.id, reversedBy.date.toString())

                reverses != null -> CashDetailStatus.Reverses(reverses.id, reverses.type, reverses.date.toString())

                else -> CashDetailStatus.NotReversed
            },
            reverseOffered = reverseOffered,
            reenterOffered = reenterOffered,
        )
    }
}
