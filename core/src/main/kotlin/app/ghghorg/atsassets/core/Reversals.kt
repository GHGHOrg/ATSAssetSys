package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate

/** A lot a sell's reversal puts back: the lot (its buy's id), its buy date and the quantity, with the splits since the sell. */
data class RestoredLot(
    val lotId: String,
    val buyDate: LocalDate,
    val quantity: Fraction,
)

/** What a reversal does, for the reversal screen (wireframe 10b frame I, marker 26). Worked out from the original (4.10). */
sealed interface ReversalEffect {
    /** The buy's whole quantity as traded with the splits up to the reversal applied, and its total back into cash. */
    data class OfBuy(
        val sharesRemoved: Fraction,
        val cashCents: Long,
    ) : ReversalEffect

    /** The exact lots the sell took, with the splits since; its proceeds out of cash; its realized gain removed. */
    data class OfSell(
        val lots: List<RestoredLot>,
        val cashCents: Long,
        val realizedRemoved: Fraction,
    ) : ReversalEffect

    /** The inverse ratio, and the holding's shares at the reversal's place before and after it; no cash. */
    data class OfSplit(
        val inverse: SplitRatio,
        val before: Fraction,
        val after: Fraction,
    ) : ReversalEffect
}

/** Why a reversal was refused (spec 4.1, 4.2, 4.6). Reasons only; the wording is D9. */
sealed interface ReversalRefusal {
    /** No buy, sell or split with that id; a cash line is never reversed directly (4.6). */
    data object NotFound : ReversalRefusal

    /** A reversal, or an entry that already has one (4.6). */
    data object NotReversible : ReversalRefusal

    data class BeforeOriginal(
        val originalDate: LocalDate,
    ) : ReversalRefusal

    /**
     * A buy's reversal while shares of its lot are sold: the lot would hold [held] (below 0) on [date]; [sellDate] is
     * the earliest sell from the lot not yet reversed (wireframe 10b frame J, the third message; 13b decision 3).
     */
    data class SellNotReversed(
        val date: LocalDate,
        val held: Fraction,
        val sellDate: LocalDate,
    ) : ReversalRefusal

    /**
     * A buy's reversal dated before a reversal of a sell from its lot: the lot would hold [held] on [date], because the
     * reversal, dated [reversalDate], comes first; [earliest] is the latest of those sells' reversal dates, the first
     * date that works (frame J, the fourth message; 13b decision 3).
     */
    data class TooEarly(
        val date: LocalDate,
        val held: Fraction,
        val reversalDate: LocalDate,
        val earliest: LocalDate,
    ) : ReversalRefusal

    /** With the reversal, the entry dated [date] would take more from a lot than the lot holds (4.1). */
    data class SellShort(
        val date: LocalDate,
    ) : ReversalRefusal

    /** The first date the cash would be negative, and the shortfall on that date (4.1, 4.2). */
    data class NegativeCash(
        val date: LocalDate,
        val shortfallCents: Long,
    ) : ReversalRefusal

    data object TooLarge : ReversalRefusal
}

sealed interface ReversalResult {
    /** The reversal and, for a buy or sell, its trade line, to be saved together as one action (4.6). */
    data class Created(
        val transaction: Transaction,
        val cashLine: CashEntry?,
    ) : ReversalResult

    data class Refused(
        val reason: ReversalRefusal,
    ) : ReversalResult
}

/** K1: reversals of buys, sells and splits (spec 4.1, 4.6, 4.10). A reversal never triggers the ticker check (4.6). */
object Reversals {
    /** The reversal of [originalId], if it has one (4.6: at most one). */
    fun reversalOf(
        transactions: List<Transaction>,
        originalId: String,
    ): Transaction? = transactions.firstOrNull { it.type == TransactionType.REVERSAL && it.reversesId == originalId }

    /** The transaction [reversal] reverses, if it is a reversal. */
    fun originalOf(
        transactions: List<Transaction>,
        reversal: Transaction,
    ): Transaction? = Holdings.originalOf(transactions, reversal)

    /** Reverse is offered on a buy, sell or split that is not reversed yet; never on a reversal (4.6). */
    fun offered(
        transactions: List<Transaction>,
        transaction: Transaction,
    ): Boolean = transaction.type != TransactionType.REVERSAL && reversalOf(transactions, transaction.id) == null

    /**
     * The reversal of [originalId] dated [date], and for a buy or sell its trade line, which gives back the original's
     * cash on that date (4.6). The first rule that fails decides the reason: the original, whether it can be reversed,
     * the date, the replay of the holding's lots (4.1), then the cash replay. The reversal takes the next entry order,
     * so on the original's own date it comes after it.
     */
    fun reverse(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
        transactionId: String,
        cashEntryId: String,
        originalId: String,
        date: LocalDate,
    ): ReversalResult {
        val original =
            transactions.firstOrNull { it.id == originalId } ?: return ReversalResult.Refused(ReversalRefusal.NotFound)
        if (!offered(transactions, original)) return ReversalResult.Refused(ReversalRefusal.NotReversible)
        if (date.isBefore(original.date)) return ReversalResult.Refused(ReversalRefusal.BeforeOriginal(original.date))
        val reversal = draft(cashEntries, transactions, original, transactionId, date)
        val seq = reversal.enteredSeq
        val after = transactions + reversal
        Holdings.firstNegative(after, original.portfolioId, original.ticker)?.let { short ->
            return ReversalResult.Refused(sharesRefusal(after, original, reversal, short))
        }
        val cashCents =
            when (original.type) {
                TransactionType.BUY -> original.totalCents
                TransactionType.SELL -> -original.totalCents
                else -> return ReversalResult.Created(reversal, null)
            }
        val cashLine =
            CashEntry(
                id = cashEntryId,
                date = date,
                amountCents = cashCents,
                type = CashType.TRADE,
                note = "",
                enteredSeq = seq,
                transactionId = transactionId,
            )
        return when (val replayed = CashRules.replay(cashEntries + cashLine, from = date)) {
            null -> ReversalResult.Created(reversal, cashLine)
            is CashEntryRefusal.NegativeCash -> ReversalResult.Refused(ReversalRefusal.NegativeCash(replayed.date, replayed.shortfallCents))
            else -> ReversalResult.Refused(ReversalRefusal.TooLarge)
        }
    }

    /**
     * The reversal of [original] dated [date], not checked and not saved: its link, the original's portfolio and
     * ticker, and the next entry order (decision 1). [reverse] checks it; the reversal screen shows its [effect].
     */
    fun draft(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
        original: Transaction,
        transactionId: String,
        date: LocalDate,
    ): Transaction = Transaction(
        id = transactionId,
        portfolioId = original.portfolioId,
        ticker = original.ticker,
        type = TransactionType.REVERSAL,
        date = date,
        enteredSeq = Transactions.nextSeq(cashEntries, transactions),
        quantity = BigDecimal.ZERO,
        totalCents = 0L,
        reversesId = original.id,
    )

    /**
     * Why the lots refuse [reversal]. For a buy: the earliest sell from its lot not yet reversed, else the latest date
     * one of them was reversed (13b decision 3). For a split or a sell: the date an entry would be left short.
     */
    internal fun sharesRefusal(
        transactions: List<Transaction>,
        original: Transaction,
        reversal: Transaction,
        short: Shortfall,
    ): ReversalRefusal {
        if (original.type != TransactionType.BUY) return ReversalRefusal.SellShort(short.place.date)
        val sells =
            transactions
                .filter { sell -> sell.type == TransactionType.SELL && sell.allocations.any { it.lotId == original.id } }
                .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
        val notReversed = sells.firstOrNull { reversalOf(transactions, it.id) == null }
        if (notReversed != null) return ReversalRefusal.SellNotReversed(short.place.date, short.held, notReversed.date)
        val earliest = sells.mapNotNull { reversalOf(transactions, it.id)?.date }.maxOrNull() ?: short.place.date
        return ReversalRefusal.TooEarly(short.place.date, short.held, reversal.date, earliest)
    }

    /** What [reversal], saved or on the screen, does; [transactions] include it. Null if its original is missing. */
    fun effect(
        transactions: List<Transaction>,
        reversal: Transaction,
    ): ReversalEffect? {
        val original = originalOf(transactions, reversal) ?: return null
        fun since(from: Place) = Holdings.splitFactor(transactions, original.portfolioId, original.ticker, from, reversal.place)
        return when (original.type) {
            TransactionType.BUY ->
                ReversalEffect.OfBuy(sharesRemoved = original.boughtQuantity * since(original.place), cashCents = original.totalCents)

            TransactionType.SELL -> {
                val buys = transactions.filter { it.type == TransactionType.BUY }.associateBy { it.id }
                ReversalEffect.OfSell(
                    lots =
                    original.allocations.mapNotNull { allocation ->
                        buys[allocation.lotId]?.let { RestoredLot(it.id, it.date, allocation.quantity * since(original.place)) }
                    },
                    cashCents = -original.totalCents,
                    realizedRemoved = Sells.realizedGain(Sells.pieces(transactions, original)),
                )
            }

            TransactionType.SPLIT -> {
                val ratio = original.split ?: return null
                val justBefore = Place(reversal.date, reversal.enteredSeq - 1)
                val before =
                    transactions
                        .filter { it.type == TransactionType.BUY && it.portfolioId == original.portfolioId && it.ticker == original.ticker }
                        .filter { it.place <= justBefore }
                        .fold(Fraction.ZERO) { sum, buy -> sum + Holdings.quantityAt(transactions, buy, justBefore) }
                val inverse = SplitRatio(ratio.from, ratio.to)
                ReversalEffect.OfSplit(inverse = inverse, before = before, after = before * inverse.factor)
            }

            TransactionType.REVERSAL -> null
        }
    }
}
