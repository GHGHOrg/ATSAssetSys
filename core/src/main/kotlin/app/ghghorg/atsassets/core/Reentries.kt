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

    /** A refusal about the reversal date, which Change on the combined review fixes (14a decision 3). */
    val fixedOnReview: Boolean
        get() = this is OfReversal && (reason is ReversalRefusal.TooEarly || reason is ReversalRefusal.BeforeOriginal)
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
    /** Offered on a buy or sell that is not reversed yet; never on a reversal (4.6). A split and a cash entry come in 14b. */
    fun offered(
        transactions: List<Transaction>,
        transaction: Transaction,
    ): Boolean = (transaction.type == TransactionType.BUY || transaction.type == TransactionType.SELL) &&
        Reversals.offered(transactions, transaction)

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
            is BuyResult.Created -> finish(cashEntries, part, buy.transaction, buy.cashLine)
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
        val sell =
            Sells.sell(
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
                picks = picks,
            )
        return when (sell) {
            is SellResult.Refused -> ReentryResult.Refused(ReentryRefusal.OfSell(sell.reason))
            is SellResult.Created -> finish(cashEntries, part, sell.transaction, sell.cashLine)
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
     * The new entry's own replay ran from its date with the reversal in it; this one starts at the reversal's date, so a
     * reversal dated before the new entry is judged on the days between, where only the reversal acts (4.1).
     */
    private fun finish(
        cashEntries: List<CashEntry>,
        part: FirstPart.Ready,
        replacement: Transaction,
        replacementCashLine: CashEntry,
    ): ReentryResult {
        val all = cashEntries + part.cashLine + replacementCashLine
        return when (val replayed = CashRules.replay(all, from = part.reversal.date)) {
            null -> ReentryResult.Created(part.reversal, part.cashLine, replacement, replacementCashLine)

            is CashEntryRefusal.NegativeCash ->
                ReentryResult.Refused(ReentryRefusal.OfReversal(ReversalRefusal.NegativeCash(replayed.date, replayed.shortfallCents)))

            else -> ReentryResult.Refused(ReentryRefusal.OfReversal(ReversalRefusal.TooLarge))
        }
    }
}
