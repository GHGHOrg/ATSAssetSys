package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashAccount
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashFilter
import app.ghghorg.atsassets.core.CashRow
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType

/**
 * The Cash account (view 7) as the screen draws it (CR2; 15d): the balance, the filter's state and the rows, worked
 * out in one pass. The screen picks the approved strings (D9) and puts these values in.
 */
data class CashListModel(
    // The balance on the latest entry date (4.8), as shown and as read by TalkBack.
    val balance: String,
    val spokenBalance: SpokenFigure,
    // Some entry exists; until then the screen shows its one line and no filters.
    val hasEntries: Boolean,
    // Why each date bound is refused, if it is (06 decision 1).
    val fromRefusal: DateRefusal?,
    val toRefusal: DateRefusal?,
    // A valid date bound, a chip other than All or a ticker: the count line and Clear filters show (06 decision 3; 28).
    val filterActive: Boolean,
    val rows: List<CashRowModel>,
    // 28: the Ticker menu's tickers (each with a trade line, alphabetically), the ticker picked (null: All tickers), and
    // whether the four direct-entry chips are enabled; while they are not, the screen shows the line saying why (view 7).
    val tickers: List<String> = emptyList(),
    val ticker: String? = null,
    val directChipsEnabled: Boolean = true,
)

/** A cash figure for TalkBack: its digits without the dollar sign, and whether the screen says "plus" or "minus". */
data class SpokenFigure(
    val digits: String,
    val sign: FigureSign,
) {
    companion object {
        /** An amount: "plus" above 0, "minus" below, as spokenSigned. */
        fun ofAmount(cents: Long): SpokenFigure = SpokenFigure(digitsOf(cents), FigureSign.of(cents, plus = true))

        /** A balance: "minus" below 0 only, as spokenDollars. */
        fun ofBalance(cents: Long): SpokenFigure = SpokenFigure(digitsOf(cents), FigureSign.of(cents, plus = false))

        private fun digitsOf(cents: Long): String = Money.format(cents).substringAfter('$')
    }
}

enum class FigureSign {
    PLUS,
    MINUS,
    NONE,
    ;

    companion object {
        fun of(
            cents: Long,
            plus: Boolean,
        ): FigureSign = when {
            cents < 0L -> MINUS
            plus && cents > 0L -> PLUS
            else -> NONE
        }
    }
}

/** One row of the Cash list, newest first, with the real balance after it, whatever the filters (view 7). */
sealed interface CashRowModel {
    // The entry's id: the row's key in the list.
    val key: String

    // The entry's date as YYYY-MM-DD; the screen adds "ET" (section 6).
    val date: String

    // The amount with its sign and no color, and the balance after it (section 6).
    val amount: String
    val balanceAfter: String
    val spokenAmount: SpokenFigure
    val spokenBalanceAfter: SpokenFigure

    /** A direct entry (or a trade line whose transaction is not there): its type and note; a tap opens its detail. */
    data class Entry(
        override val key: String,
        override val date: String,
        override val amount: String,
        override val balanceAfter: String,
        override val spokenAmount: SpokenFigure,
        override val spokenBalanceAfter: SpokenFigure,
        val type: CashType,
        val note: String,
    ) : CashRowModel

    /** A trade line (09, 11, 13b): its title, ticker and portfolio name; a tap opens its transaction (13a). */
    data class Trade(
        override val key: String,
        override val date: String,
        override val amount: String,
        override val balanceAfter: String,
        override val spokenAmount: SpokenFigure,
        override val spokenBalanceAfter: SpokenFigure,
        val transactionId: String,
        val kind: TransactionType,
        val title: TradeTitle,
        val ticker: String,
        val portfolio: String,
    ) : CashRowModel
}

/** A trade line's title (09 wording 3, 11 wording 4, 13b wording 5), as on transaction detail. */
enum class TradeTitle {
    BUY,
    SELL,
    SPLIT,
    REVERSAL_OF_BUY,
    REVERSAL_OF_SELL,
    REVERSAL_OF_SPLIT,
}

/**
 * Builds the Cash list's model in one pass: one sort of the entries (CashAccount.rows, 4.1), then each row's
 * transaction and portfolio found by id, not by a search per row (15d).
 */
object CashList {
    fun of(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        form: CashFilterForm,
    ): CashListModel {
        val filter =
            CashFilter(
                from = DateDigits.toDate(form.fromText),
                to = DateDigits.toBound(form.fromText, form.toText),
                type = form.type,
                ticker = form.ticker,
                trade = form.trade,
            )
        val all = CashAccount.rows(entries)
        // The first record with each id, as the searches by id before 15d found.
        val byId = buildMap { transactions.forEach { putIfAbsent(it.id, it) } }
        val names = buildMap { portfolios.forEach { putIfAbsent(it.id, it.name) } }
        val balance = CashAccount.balanceCents(entries)
        return CashListModel(
            balance = Money.format(balance),
            spokenBalance = SpokenFigure.ofBalance(balance),
            hasEntries = all.isNotEmpty(),
            fromRefusal = DateDigits.refusal(form.fromText),
            toRefusal = DateDigits.toRefusal(form.fromText, form.toText),
            filterActive = filter.isActive,
            rows = all.filter { matches(filter, it.entry, byId) }.map { row(it, byId, names) },
            tickers = CashAccount.tickers(entries, transactions),
            ticker = form.ticker,
            directChipsEnabled = form.ticker == null,
        )
    }

    // Core's rule (28 decision 1), judged with the line's trade and, for a reversal, its original, found by id.
    private fun matches(
        filter: CashFilter,
        entry: CashEntry,
        byId: Map<String, Transaction>,
    ): Boolean {
        val trade = if (entry.type == CashType.TRADE) entry.transactionId?.let(byId::get) else null
        val original = if (trade?.type == TransactionType.REVERSAL) trade.reversesId?.let(byId::get) else null
        return filter.matches(entry, trade, original)
    }

    private fun row(
        row: CashRow,
        byId: Map<String, Transaction>,
        names: Map<String, String>,
    ): CashRowModel {
        val entry = row.entry
        val date = entry.date.toString()
        val amount = signedAmount(entry.amountCents)
        val balanceAfter = Money.format(row.balanceAfterCents)
        val spokenAmount = SpokenFigure.ofAmount(entry.amountCents)
        val spokenBalance = SpokenFigure.ofBalance(row.balanceAfterCents)
        val trade =
            entry.transactionId?.let { byId[it] }
                ?: return CashRowModel.Entry(entry.id, date, amount, balanceAfter, spokenAmount, spokenBalance, entry.type, entry.note)
        return CashRowModel.Trade(
            key = entry.id,
            date = date,
            amount = amount,
            balanceAfter = balanceAfter,
            spokenAmount = spokenAmount,
            spokenBalanceAfter = spokenBalance,
            transactionId = trade.id,
            kind = trade.type,
            title = titleOf(trade, byId),
            ticker = trade.ticker,
            portfolio = names[trade.portfolioId].orEmpty(),
        )
    }

    // As transactionTitle on transaction detail: a reversal is titled after its original, if that is there.
    private fun titleOf(
        trade: Transaction,
        byId: Map<String, Transaction>,
    ): TradeTitle {
        val original = if (trade.type == TransactionType.REVERSAL) trade.reversesId?.let { byId[it] } else null
        return when {
            original?.type == TransactionType.SELL -> TradeTitle.REVERSAL_OF_SELL
            original?.type == TransactionType.SPLIT -> TradeTitle.REVERSAL_OF_SPLIT
            original != null -> TradeTitle.REVERSAL_OF_BUY
            trade.type == TransactionType.SELL -> TradeTitle.SELL
            trade.type == TransactionType.SPLIT -> TradeTitle.SPLIT
            else -> TradeTitle.BUY
        }
    }
}
