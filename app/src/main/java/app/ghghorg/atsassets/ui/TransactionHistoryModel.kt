package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.HistoryChip
import app.ghghorg.atsassets.core.HistoryRow
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType

/*
 * Transaction history as the screen draws it (view 8, wireframe 08; CR2, 27): the portfolio, the filters as typed and
 * picked, the Ticker menu, and the rows with every figure worked out here. Plain Kotlin: the screen picks the approved
 * strings and puts these values in.
 */

/** A buy's or sell's cash-effect line, or a reversal's (marker 3): signed, no color; opens the trade's line. */
data class HistoryCashEffect(
    val transactionId: String,
    val text: String,
    val spoken: SpokenFigure,
)

/** The original's "Reversed on" line (marker 4): the reversal's date; opens the reversal. */
data class HistoryReversedOn(
    val reversalId: String,
    val date: String,
)

/** The quantity as originally traded (4.10; CR1: a buy's boughtQuantity), "1 share" only when shown exactly as "1". */
data class HistoryShares(
    val text: String,
    val one: Boolean,
)

/** One row of the list (wireframe 08 markers 3 to 5, 7). */
sealed interface HistoryRowModel {
    /** The row's own key in the list. */
    val key: String

    /** A buy or sell: "Buy AAPL", "2026-02-01 ET, 10 shares", the total, its cash effect; opens itself. */
    data class Trade(
        val transactionId: String,
        val sell: Boolean,
        val ticker: String,
        val date: String,
        val shares: HistoryShares,
        val total: String,
        val spokenTotal: String,
        val cash: HistoryCashEffect?,
        val reversedOn: HistoryReversedOn?,
    ) : HistoryRowModel {
        override val key: String get() = transactionId
    }

    /** A split: "Split AAPL", its date and its ratio; no cash line; opens itself. */
    data class Split(
        val transactionId: String,
        val ticker: String,
        val date: String,
        val ratio: SplitRatio,
        val reversedOn: HistoryReversedOn?,
    ) : HistoryRowModel {
        override val key: String get() = transactionId
    }

    /**
     * A reversal: "Reversal of the buy of 2026-03-20 ET", its ticker and date, the original's quantity as traded and total
     * (or a split's ratio), and its own cash effect. It opens the reversal itself, whose detail links to the original
     * (27 step 6, phone check 6: your choice of view 8's "A row opens the transaction" over marker 4's "opens it").
     */
    data class Reversal(
        val transactionId: String,
        val originalType: TransactionType,
        val originalDate: String,
        val ticker: String,
        val date: String,
        val shares: HistoryShares?,
        val total: String?,
        val spokenTotal: String?,
        val ratio: SplitRatio?,
        val cash: HistoryCashEffect?,
    ) : HistoryRowModel {
        override val key: String get() = transactionId
    }

    /** Under Cash (marker 5): a trade's line or a reversal's, its date and signed amount; opens its transaction. */
    data class CashLine(
        val lineId: String,
        val transactionId: String,
        val kind: HistoryCashKind,
        val ticker: String,
        val date: String,
        val amount: String,
        val spoken: SpokenFigure,
    ) : HistoryRowModel {
        override val key: String get() = lineId
    }
}

/** What a Cash-chip row names: "Cash line, buy AAPL" or "Reversal of buy AAPL" (marker 5, as the Cash list's marker 8). */
enum class HistoryCashKind {
    BUY,
    SELL,
    REVERSAL_OF_BUY,
    REVERSAL_OF_SELL,
}

/** The whole screen: the portfolio's name, the fields as typed with their lines, the ticker, the chip, the menu and the rows. */
data class TransactionHistoryModel(
    val portfolioName: String,
    val fromText: String,
    val toText: String,
    val fromRefusal: DateRefusal?,
    val toRefusal: DateRefusal?,
    val ticker: String?,
    val tickers: List<String>,
    val chip: HistoryChip,
    val menuOpen: Boolean,
    val rows: List<HistoryRowModel>,
)

object TransactionHistories {
    /** The screen from K3's [rows] for the filters as typed and picked. */
    fun of(
        rows: List<HistoryRow>,
        portfolioName: String,
        fromText: String,
        toText: String,
        ticker: String?,
        tickers: List<String>,
        chip: HistoryChip,
        menuOpen: Boolean,
    ): TransactionHistoryModel = TransactionHistoryModel(
        portfolioName = portfolioName,
        fromText = fromText,
        toText = toText,
        fromRefusal = DateDigits.refusal(fromText),
        toRefusal = DateDigits.toRefusal(fromText, toText),
        ticker = ticker,
        tickers = tickers,
        chip = chip,
        menuOpen = menuOpen,
        rows = rows.map(::row),
    )

    private fun row(row: HistoryRow): HistoryRowModel = when (row) {
        is HistoryRow.Cash -> cashRow(row)
        is HistoryRow.Entry -> entryRow(row)
    }

    private fun entryRow(row: HistoryRow.Entry): HistoryRowModel {
        val transaction = row.transaction
        val date = transaction.date.toString()
        val cash = row.cashLine?.let { HistoryCashEffect(transaction.id, signedAmount(it.amountCents), SpokenFigure.ofAmount(it.amountCents)) }
        val reversedOn = row.reversal?.let { HistoryReversedOn(it.id, it.date.toString()) }
        val original = row.original
        return when {
            original != null ->
                HistoryRowModel.Reversal(
                    transactionId = transaction.id,
                    originalType = original.type,
                    originalDate = original.date.toString(),
                    ticker = transaction.ticker,
                    date = date,
                    shares = if (original.type == TransactionType.SPLIT) null else shares(original),
                    total = if (original.type == TransactionType.SPLIT) null else Money.format(original.totalCents),
                    spokenTotal = if (original.type == TransactionType.SPLIT) null else digits(original.totalCents),
                    ratio = original.split,
                    cash = cash,
                )

            transaction.type == TransactionType.SPLIT ->
                HistoryRowModel.Split(transaction.id, transaction.ticker, date, transaction.split ?: SplitRatio(1, 1), reversedOn)

            else ->
                HistoryRowModel.Trade(
                    transactionId = transaction.id,
                    sell = transaction.type == TransactionType.SELL,
                    ticker = transaction.ticker,
                    date = date,
                    shares = shares(transaction),
                    total = Money.format(transaction.totalCents),
                    spokenTotal = digits(transaction.totalCents),
                    cash = cash,
                    reversedOn = reversedOn,
                )
        }
    }

    private fun cashRow(row: HistoryRow.Cash): HistoryRowModel.CashLine {
        val kind =
            when (row.original?.type ?: row.transaction.type) {
                TransactionType.SELL -> if (row.original != null) HistoryCashKind.REVERSAL_OF_SELL else HistoryCashKind.SELL
                else -> if (row.original != null) HistoryCashKind.REVERSAL_OF_BUY else HistoryCashKind.BUY
            }
        val cents = row.line.amountCents
        return HistoryRowModel.CashLine(
            lineId = row.line.id,
            transactionId = row.transaction.id,
            kind = kind,
            ticker = row.transaction.ticker,
            date = row.line.date.toString(),
            amount = signedAmount(cents),
            spoken = SpokenFigure.ofAmount(cents),
        )
    }

    // The quantity as originally traded: a buy's boughtQuantity (CR1), a sell's quantity; splits not applied.
    private fun shares(transaction: Transaction): HistoryShares {
        val shown = Quantity.format(transaction.boughtQuantity)
        return HistoryShares(shown, one = shown == "1")
    }

    private fun digits(cents: Long): String = Money.format(cents).substringAfter('$')
}
