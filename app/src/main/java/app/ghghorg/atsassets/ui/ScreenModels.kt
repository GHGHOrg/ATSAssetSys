package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.Reversals
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType

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
