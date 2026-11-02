package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate

/** The ids of the four records a Reverse and re-enter pair saves: the reversal, the new entry and their cash lines. */
data class ReentryIds(
    val reversalId: String,
    val reversalCashId: String,
    val replacementId: String,
    val replacementCashId: String,
)

/** Why a pair was refused, naming the part (wireframe 10c frame F, marker 12). Reasons only; the wording is D9. */
sealed interface ReentryRefusal {
    /** Part 1: the reversal, under the rules of 13b; a block from the original itself (Example D) lands here. */
    data class OfReversal(
        val reason: ReversalRefusal,
    ) : ReentryRefusal

    /** Part 2: the new buy, under the rules of 4.1 to 4.3, with the reversal applied. */
    data class OfBuy(
        val reason: BuyRefusal,
    ) : ReentryRefusal

    /** Part 2: the new sell, under the rules of 4.1, 4.2 and 4.4, with the reversal applied. */
    data class OfSell(
        val reason: SellRefusal,
    ) : ReentryRefusal

    /** Part 1 of a cash entry's pair: its reversal, under the rules of 05 (14b). */
    data class OfCashReversal(
        val reason: CashEntryRefusal,
    ) : ReentryRefusal

    /** Part 2: the new cash entry, under the rules of 4.1 and 4.2, with the reversal applied (14b). */
    data class OfCash(
        val reason: CashEntryRefusal,
    ) : ReentryRefusal

    /** Part 2: the new split, under the rules of 4.1 and 4.5, with the reversal applied (14b). */
    data class OfSplit(
        val reason: SplitRefusal,
    ) : ReentryRefusal

    /** A refusal about the reversal date, which Change on the combined review fixes (14a decision 3). */
    val fixedOnReview: Boolean
        get() = when (this) {
            is OfReversal -> reason is ReversalRefusal.TooEarly || reason is ReversalRefusal.BeforeOriginal
            is OfCashReversal -> reason is CashEntryRefusal.BeforeOriginal
            else -> false
        }
}

sealed interface CashPairResult {
    /** The reversal (an Adjustment) and the new cash entry, to be saved together as one batch (4.6, K2). */
    data class Created(
        val reversal: CashEntry,
        val replacement: CashEntry,
    ) : CashPairResult

    data class Refused(
        val reason: ReentryRefusal,
    ) : CashPairResult
}

sealed interface SplitPairResult {
    /** The split's reversal and the new split, to be saved together as one action (4.6, K2); neither has a cash line. */
    data class Created(
        val reversal: Transaction,
        val replacement: Transaction,
    ) : SplitPairResult

    data class Refused(
        val reason: ReentryRefusal,
    ) : SplitPairResult
}

sealed interface ReentryResult {
    /** The reversal, the new entry and their trade lines, to be saved together as one action (4.6, K2). */
    data class Created(
        val reversal: Transaction,
        val reversalCashLine: CashEntry,
        val replacement: Transaction,
        val replacementCashLine: CashEntry,
    ) : ReentryResult

    data class Refused(
        val reason: ReentryRefusal,
    ) : ReentryResult
}

/**
 * K1: Reverse and re-enter of a buy or sell (spec 4.6). The pair is checked as a unit: the reversal under the rules of
 * 13b, then the new entry as a normal buy or sell with the reversal applied, taking the next entry order after it, then
 * one cash replay with both entries from the earlier of their dates (4.1). The first rule that fails decides the reason.
 * The ticker check of 4.3 is K5's, for a new buy only.
 */
object Reentries {
    /** Offered on a buy, sell (14a) or split (14b) that is not reversed yet; never on a reversal (4.6). A cash entry: [cashOffered]. */
    fun offered(
        transactions: List<Transaction>,
        transaction: Transaction,
    ): Boolean = Reversals.offered(transactions, transaction)

    fun reenterBuy(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        ids: ReentryIds,
        originalId: String,
        reversalDate: LocalDate,
        portfolioId: String,
        ticker: String,
        date: LocalDate,
        quantity: BigDecimal,
        totalCents: Long,
    ): ReentryResult {
        val part =
            when (val first = reversalPart(cashEntries, transactions, ids, originalId, TransactionType.BUY, reversalDate)) {
                is FirstPart.Refused -> return ReentryResult.Refused(ReentryRefusal.OfReversal(first.reason))
                is FirstPart.Ready -> first
            }
        val buy =
            Transactions.buy(
                cashEntries = cashEntries + part.cashLine,
                transactions = transactions + part.reversal,
                portfolios = portfolios,
                transactionId = ids.replacementId,
                cashEntryId = ids.replacementCashId,
                portfolioId = portfolioId,
                ticker = ticker,
                date = date,
                quantity = quantity,
                totalCents = totalCents,
            )
        return when (buy) {
            is BuyResult.Refused -> ReentryResult.Refused(ReentryRefusal.OfBuy(buy.reason))

            is BuyResult.Created ->
                finish(cashEntries, part, buy.transaction, buy.cashLine) { date, cents ->
                    ReentryRefusal.OfBuy(BuyRefusal.NegativeCash(date, cents))
                }
        }
    }

    fun reenterSell(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        ids: ReentryIds,
        originalId: String,
        reversalDate: LocalDate,
        portfolioId: String,
        ticker: String,
        date: LocalDate,
        quantity: BigDecimal,
        totalCents: Long,
        picks: List<Pick>,
    ): ReentryResult {
        val part =
            when (val first = reversalPart(cashEntries, transactions, ids, originalId, TransactionType.SELL, reversalDate)) {
                is FirstPart.Refused -> return ReentryResult.Refused(ReentryRefusal.OfReversal(first.reason))
                is FirstPart.Ready -> first
            }
        // Without the reversal's cash line: a sell only adds cash, so its own replay can pass where the pair fails, never
        // the other way; [finish] judges the pair, on a shared date as one point (14a decision 6). The entry order still
        // comes after the reversal, which is in the transactions.
        val sell =
            Sells.sell(
                cashEntries = cashEntries,
                transactions = transactions + part.reversal,
                portfolios = portfolios,
                transactionId = ids.replacementId,
                cashEntryId = ids.replacementCashId,
                portfolioId = portfolioId,
                ticker = ticker,
                date = date,
                quantity = quantity,
                totalCents = totalCents,
                picks = picks,
            )
        return when (sell) {
            is SellResult.Refused -> ReentryResult.Refused(ReentryRefusal.OfSell(sell.reason))

            is SellResult.Created ->
                finish(cashEntries, part, sell.transaction, sell.cashLine) { date, cents ->
                    ReentryRefusal.OfSell(SellRefusal.NegativeCash(date, cents))
                }
        }
    }

    /**
     * A new sell's first picks: the original's allocations, each with the splits between the original and the new
     * sell's place (date [sellDate], entry order [sellSeq]), as the lot picker shows them (4.6, 10c marker 9).
     * [transactions] include the reversal, so the lots are as they will be after it.
     */
    fun startingPicks(
        transactions: List<Transaction>,
        original: Transaction,
        sellDate: LocalDate,
        sellSeq: Long,
    ): List<Pick> {
        val factor = Holdings.splitFactor(transactions, original.portfolioId, original.ticker, original.place, Place(sellDate, sellSeq))
        return original.allocations.map { Pick(it.lotId, Sells.shown(it.quantity * factor)) }
    }

    /**
     * A new buy's first quantity: the original's, with the splits between the original and the new entry's place (date
     * [date], entry order [seq]) applied, as its picks are; to 8 places, as a quantity is typed (4.3). With no split
     * between, the original as typed, so "2.50" stays "2.50" (14a fix after phone check 2; decision B).
     */
    fun startingQuantity(
        transactions: List<Transaction>,
        original: Transaction,
        date: LocalDate,
        seq: Long,
    ): BigDecimal {
        val factor = Holdings.splitFactor(transactions, original.portfolioId, original.ticker, original.place, Place(date, seq))
        return if (factor == Fraction.ONE) original.quantity else Sells.shown(Fraction.of(original.quantity) * factor)
    }

    /** Offered on a direct cash entry that is not a reversal and not reversed; never the opening balance or a trade line (4.6). */
    fun cashOffered(
        existing: List<CashEntry>,
        entry: CashEntry,
    ): Boolean = entry.type != CashType.OPENING_BALANCE &&
        entry.type != CashType.TRADE &&
        entry.reversesId == null &&
        existing.none { it.reversesId == entry.id }

    /**
     * Reverse and re-enter of a direct cash entry (14b; 10c frame E): the reversal, an Adjustment of the opposite
     * amount with [reversalNote] (05), then the new entry as a normal one after it, then one cash replay with both from
     * the earlier of their dates, a shared date judged as one point (14a decision 5). A negative balance before the new
     * entry's date is the reversal's; from that date on it is the new entry's.
     */
    fun reenterCash(
        existing: List<CashEntry>,
        reversalId: String,
        originalId: String,
        reversalDate: LocalDate,
        reversalNote: String,
        replacementId: String,
        type: CashType,
        date: LocalDate,
        amountCents: Long,
        note: String,
        direction: CashDirection,
    ): CashPairResult {
        val reversal =
            when (val first = CashEntries.draftReverse(existing, reversalId, originalId, reversalDate, reversalNote)) {
                is CashEntryResult.Refused -> return CashPairResult.Refused(ReentryRefusal.OfCashReversal(first.reason))
                is CashEntryResult.Created -> first.entry
            }
        val replacement =
            when (val second = CashEntries.draftCreate(existing + reversal, replacementId, type, date, amountCents, note, direction)) {
                is CashEntryResult.Refused -> return CashPairResult.Refused(ReentryRefusal.OfCash(second.reason))
                is CashEntryResult.Created -> second.entry
            }
        val replayed =
            CashRules.replay(
                existing + reversal + replacement,
                from = minOf(reversal.date, replacement.date),
                notJudged = if (reversal.date == replacement.date) setOf(reversal.id) else emptySet(),
            )
        val refusal =
            when {
                replayed == null -> return CashPairResult.Created(reversal, replacement)
                replayed is CashEntryRefusal.NegativeCash && replayed.date.isBefore(replacement.date) -> ReentryRefusal.OfCashReversal(replayed)
                else -> ReentryRefusal.OfCash(replayed)
            }
        return CashPairResult.Refused(refusal)
    }

    /**
     * Reverse and re-enter of a split (14b; 10c marker 11): the reversal under the rules of 13b (the original, whether it
     * can be reversed, the date), then the new split as a normal one after it. The lots are checked once with both
     * splits applied (4.6), so a later sell left short is the new split's: the reversal alone may take a lot below
     * what a later sell took, which the new ratio puts back.
     */
    fun reenterSplit(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        reversalId: String,
        replacementId: String,
        originalId: String,
        reversalDate: LocalDate,
        portfolioId: String,
        ticker: String,
        date: LocalDate,
        ratio: SplitRatio,
    ): SplitPairResult {
        val original =
            transactions.firstOrNull { it.id == originalId && it.type == TransactionType.SPLIT }
                ?: return SplitPairResult.Refused(ReentryRefusal.OfReversal(ReversalRefusal.NotFound))
        val refusal =
            when {
                !Reversals.offered(transactions, original) -> ReversalRefusal.NotReversible
                reversalDate.isBefore(original.date) -> ReversalRefusal.BeforeOriginal(original.date)
                else -> null
            }
        if (refusal != null) return SplitPairResult.Refused(ReentryRefusal.OfReversal(refusal))
        val reversal = Reversals.draft(cashEntries, transactions, original, reversalId, reversalDate)
        val replacement =
            when (val split = Splits.split(cashEntries, transactions + reversal, portfolios, replacementId, portfolioId, ticker, date, ratio)) {
                is SplitResult.Refused -> return SplitPairResult.Refused(ReentryRefusal.OfSplit(split.reason))
                is SplitResult.Created -> split.transaction
            }
        // A new split on another holding leaves the original's with the reversal alone, which Splits.split does not check
        // (14b Deploy finding 1).
        if (portfolioId != original.portfolioId || replacement.ticker != original.ticker) {
            Holdings.firstNegative(transactions + reversal + replacement, original.portfolioId, original.ticker)?.let { short ->
                return SplitPairResult.Refused(ReentryRefusal.OfReversal(ReversalRefusal.SellShort(short.place.date)))
            }
        }
        return SplitPairResult.Created(reversal, replacement)
    }

    private sealed interface FirstPart {
        data class Ready(
            val reversal: Transaction,
            val cashLine: CashEntry,
        ) : FirstPart

        data class Refused(
            val reason: ReversalRefusal,
        ) : FirstPart
    }

    /** The reversal of [originalId], which must be a [type], checked as in 13b except for cash, which [finish] judges. */
    private fun reversalPart(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
        ids: ReentryIds,
        originalId: String,
        type: TransactionType,
        date: LocalDate,
    ): FirstPart {
        val original =
            transactions.firstOrNull { it.id == originalId && it.type == type } ?: return FirstPart.Refused(ReversalRefusal.NotFound)
        if (!Reversals.offered(transactions, original)) return FirstPart.Refused(ReversalRefusal.NotReversible)
        if (date.isBefore(original.date)) return FirstPart.Refused(ReversalRefusal.BeforeOriginal(original.date))
        val reversal = Reversals.draft(cashEntries, transactions, original, ids.reversalId, date)
        val after = transactions + reversal
        Holdings.firstNegative(after, original.portfolioId, original.ticker)?.let { short ->
            return FirstPart.Refused(Reversals.sharesRefusal(after, original, reversal, short))
        }
        val cashLine =
            CashEntry(
                id = ids.reversalCashId,
                date = date,
                amountCents = if (type == TransactionType.BUY) original.totalCents else -original.totalCents,
                type = CashType.TRADE,
                note = "",
                enteredSeq = reversal.enteredSeq,
                transactionId = reversal.id,
            )
        return FirstPart.Ready(reversal, cashLine)
    }

    /**
     * One cash replay with both entries, from the earlier of their dates (4.1, 4.6). On a shared date the reversal is
     * judged with the new entry after it, as one point (14a decision 6). A negative balance before the new entry's date
     * is the reversal's, where only it acts; from that date on it is the new entry's ("with the replacement", frame F).
     */
    private fun finish(
        cashEntries: List<CashEntry>,
        part: FirstPart.Ready,
        replacement: Transaction,
        replacementCashLine: CashEntry,
        replacementShort: (LocalDate, Long) -> ReentryRefusal,
    ): ReentryResult {
        val all = cashEntries + part.cashLine + replacementCashLine
        val sharedDate = part.reversal.date == replacement.date
        val replayed =
            CashRules.replay(
                all,
                from = minOf(part.reversal.date, replacement.date),
                notJudged = if (sharedDate) setOf(part.cashLine.id) else emptySet(),
            )
        return when (replayed) {
            null -> ReentryResult.Created(part.reversal, part.cashLine, replacement, replacementCashLine)

            is CashEntryRefusal.NegativeCash ->
                ReentryResult.Refused(
                    if (replayed.date.isBefore(replacement.date)) {
                        ReentryRefusal.OfReversal(ReversalRefusal.NegativeCash(replayed.date, replayed.shortfallCents))
                    } else {
                        replacementShort(replayed.date, replayed.shortfallCents)
                    },
                )

            else -> ReentryResult.Refused(ReentryRefusal.OfReversal(ReversalRefusal.TooLarge))
        }
    }
}
