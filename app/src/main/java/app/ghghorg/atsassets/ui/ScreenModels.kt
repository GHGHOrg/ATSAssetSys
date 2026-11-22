package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashPairResult
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.ReentryResult
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.Reversals
import app.ghghorg.atsassets.core.SoldPiece
import app.ghghorg.atsassets.core.SplitEffect
import app.ghghorg.atsassets.core.SplitPairResult
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.Transactions

/*
 * Screen models (CR2; 15a decision 1): what a screen shows, worked out from the records and unit tested, so the screen
 * only chooses the approved string (strings.xml, D9) and puts these values in. Plain Kotlin: no Compose, no resources.
 */

/** One row of Holding detail's Transactions tab (view 6): a buy or sell, a split, or a reversal. */
sealed interface HoldingRowModel {
    val transactionId: String

    /** The entry's date as YYYY-MM-DD; the screen adds "ET" (section 6). */
    val date: String

    /** A buy or sell: its exact shares (a buy's quantity ratio included, CR1) and its total. */
    data class Trade(
        override val transactionId: String,
        override val date: String,
        val sell: Boolean,
        val shares: String,
        val oneShare: Boolean,
        val amount: String,
    ) : HoldingRowModel

    data class Split(
        override val transactionId: String,
        override val date: String,
        val ratio: SplitRatio,
    ) : HoldingRowModel

    /** A reversal (13b wording 5): the kind and date of the entry it reverses; no amount. */
    data class Reversal(
        override val transactionId: String,
        override val date: String,
        val originalType: TransactionType,
        val originalDate: String,
    ) : HoldingRowModel
}

/** Reverse and re-enter's combined review (4.6, wireframe 10c): what is reversed, part 1, part 2, the combined cash. */
data class ReplaceReviewModel(
    val reverses: ReversedEntry,
    val partOne: ReversalPartModel?,
    val partTwo: ReplacementPartModel?,
    val combinedCents: Long?,
)

/** The entry being replaced: its kind and date. */
sealed interface ReversedEntry {
    data class Trade(
        val type: TransactionType,
        val date: String,
    ) : ReversedEntry

    data class Cash(
        val type: CashType,
        val date: String,
    ) : ReversedEntry
}

/** Lot quantities shown as "10 + 2 shares": the quantities, formatted, and whether the noun is "share" (one lot of 1). */
data class ShareList(
    val quantities: List<String>,
    val oneShare: Boolean,
) {
    companion object {
        /** No lot shows as "0 shares", as the review did before (10c). */
        fun of(quantities: List<Fraction>): ShareList {
            val shown = quantities.map { Quantity.format(it) }.ifEmpty { listOf(Quantity.format(Fraction.ZERO)) }
            return ShareList(shown, oneShare = shown == listOf("1"))
        }
    }
}

/** Part 1: what the reversal does at its date (10c markers 4, 5; frame E). Cash amounts are signed cents. */
sealed interface ReversalPartModel {
    data class OfBuy(
        val sharesRemoved: String,
        val cashCents: Long,
    ) : ReversalPartModel

    data class OfSell(
        val lotsPutBack: ShareList,
        val realizedRemovedCents: Long,
        val cashCents: Long,
    ) : ReversalPartModel

    data class OfSplit(
        val inverse: SplitRatio,
        val before: String,
        val after: String,
    ) : ReversalPartModel

    /** A cash entry's reversal: an Adjustment of the opposite amount with its fixed note (14b decision 1). */
    data class OfCash(
        val note: String,
        val cashCents: Long,
    ) : ReversalPartModel
}

/** Part 2: the new entry as on its own review (10c markers 6, 10, 11; frame E). */
sealed interface ReplacementPartModel {
    data class Buy(
        val ticker: String,
        val portfolio: String,
        val date: String,
        val quantity: String,
        val total: String,
        val pricePerShare: String,
        val cashCents: Long,
    ) : ReplacementPartModel

    data class Sell(
        val ticker: String,
        val portfolio: String,
        val date: String,
        val quantity: String,
        val total: String,
        val lotsUsed: ShareList,
        val cost: String,
        val realizedCents: Long,
        val cashCents: Long,
    ) : ReplacementPartModel

    data class Split(
        val ticker: String,
        val portfolio: String,
        val date: String,
        val ratio: SplitRatio?,
        val before: String?,
        val after: String?,
    ) : ReplacementPartModel

    /** A cash entry: its type, date, signed amount and note as typed, an empty one included (14b phone check 3). */
    data class Cash(
        val type: CashType,
        val date: String,
        val amountCents: Long,
        val note: String,
    ) : ReplacementPartModel
}

object ReplaceReview {
    fun of(
        original: Transaction?,
        originalCash: CashEntry?,
        reversalNote: String,
        reversalEffect: ReversalEffect?,
        trade: ReentryResult.Created?,
        pieces: List<SoldPiece>,
        split: SplitPairResult.Created?,
        splitEffect: SplitEffect?,
        cash: CashPairResult.Created?,
        combinedCents: Long?,
        portfolios: List<Portfolio>,
    ): ReplaceReviewModel? {
        val reverses =
            when {
                originalCash != null -> ReversedEntry.Cash(originalCash.type, originalCash.date.toString())
                original != null -> ReversedEntry.Trade(original.type, original.date.toString())
                else -> return null
            }
        val partOne =
            if (originalCash != null) {
                ReversalPartModel.OfCash(reversalNote, -originalCash.amountCents)
            } else {
                reversalEffect?.let { partOne(it) }
            }
        val partTwo =
            trade?.let { partTwo(it, pieces, portfolios) }
                ?: split?.let { partTwo(it.replacement, splitEffect, portfolios) }
                ?: cash?.replacement?.let { ReplacementPartModel.Cash(it.type, it.date.toString(), it.amountCents, it.note) }
        return ReplaceReviewModel(reverses, partOne, partTwo, combinedCents)
    }

    private fun partOne(effect: ReversalEffect): ReversalPartModel = when (effect) {
        is ReversalEffect.OfBuy -> ReversalPartModel.OfBuy(Quantity.format(effect.sharesRemoved), effect.cashCents)

        is ReversalEffect.OfSell ->
            ReversalPartModel.OfSell(ShareList.of(effect.lots.map { it.quantity }), -centsOf(effect.realizedRemoved), effect.cashCents)

        is ReversalEffect.OfSplit -> ReversalPartModel.OfSplit(effect.inverse, Quantity.format(effect.before), Quantity.format(effect.after))
    }

    private fun partTwo(
        trade: ReentryResult.Created,
        pieces: List<SoldPiece>,
        portfolios: List<Portfolio>,
    ): ReplacementPartModel {
        val replacement = trade.replacement
        val portfolio = portfolios.firstOrNull { it.id == replacement.portfolioId }?.name.orEmpty()
        val date = replacement.date.toString()
        val total = Money.format(replacement.totalCents)
        val cashCents = trade.replacementCashLine.amountCents
        if (replacement.type != TransactionType.SELL) {
            return ReplacementPartModel.Buy(
                ticker = replacement.ticker,
                portfolio = portfolio,
                date = date,
                quantity = Quantity.format(replacement.boughtQuantity),
                total = total,
                pricePerShare = Money.formatPerShare(Transactions.pricePerShare(replacement)),
                cashCents = cashCents,
            )
        }
        return ReplacementPartModel.Sell(
            ticker = replacement.ticker,
            portfolio = portfolio,
            date = date,
            quantity = Quantity.format(replacement.quantity),
            total = total,
            lotsUsed = ShareList.of(pieces.map { it.quantity }),
            cost = Money.format(Money.partsInCents(pieces.map { it.cost }).sum()),
            realizedCents = Money.partsInCents(pieces.map { it.gain }).sum(),
            cashCents = cashCents,
        )
    }

    private fun partTwo(
        replacement: Transaction,
        effect: SplitEffect?,
        portfolios: List<Portfolio>,
    ): ReplacementPartModel = ReplacementPartModel.Split(
        ticker = replacement.ticker,
        portfolio = portfolios.firstOrNull { it.id == replacement.portfolioId }?.name.orEmpty(),
        date = replacement.date.toString(),
        ratio = replacement.split,
        before = effect?.let { Quantity.format(it.before) },
        after = effect?.let { Quantity.format(it.after) },
    )
}

object HoldingRows {
    /** The holding's rows, newest first, and on a shared date the later entry first, as Holdings.transactionsOf. */
    fun of(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
    ): List<HoldingRowModel> = Holdings.transactionsOf(transactions, portfolioId, ticker).map { row(transactions, it) }

    private fun row(
        transactions: List<Transaction>,
        transaction: Transaction,
    ): HoldingRowModel {
        val date = transaction.date.toString()
        val original = Reversals.originalOf(transactions, transaction)
        val ratio = transaction.split
        return when {
            original != null -> HoldingRowModel.Reversal(transaction.id, date, original.type, original.date.toString())

            ratio != null -> HoldingRowModel.Split(transaction.id, date, ratio)

            else -> {
                val shares = Quantity.format(transaction.boughtQuantity)
                HoldingRowModel.Trade(
                    transactionId = transaction.id,
                    date = date,
                    sell = transaction.type == TransactionType.SELL,
                    shares = shares,
                    oneShare = shares == "1",
                    amount = Money.format(transaction.totalCents),
                )
            }
        }
    }
}
