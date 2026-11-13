package app.ghghorg.atsassets.core

import java.time.LocalDate

/** The Buy and Sell chips of the Cash list (view 7; 28): the trade lines of buys or of sells, reversals included. */
enum class TradeSide {
    BUY,
    SELL,
}

/**
 * The Cash list filters (view 7): a date range, both ends included, one type ([type] for a direct entry's chip or
 * [trade] for Buy and Sell; 28) and a ticker (28); null means no bound, All and All tickers. Combined: a row must
 * match all of them. Never stored (iteration 06 decision 4).
 */
data class CashFilter(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val type: CashType? = null,
    val ticker: String? = null,
    val trade: TradeSide? = null,
) {
    val isActive: Boolean get() = from != null || to != null || type != null || ticker != null || trade != null

    /** Without the transactions: the date and type only; a ticker or a trade side then matches nothing. */
    fun matches(entry: CashEntry): Boolean = ticker == null && trade == null && matchesDateAndType(entry)

    /**
     * With the line's trade ([transaction], null for a direct entry or a line whose trade is not there) and, for a
     * reversal, the entry it reverses ([original]): a ticker takes only a trade's line of that ticker; Buy and Sell take
     * the lines of buys or sells and of their reversals, under the chip of the entry reversed (view 7, C4).
     */
    fun matches(
        entry: CashEntry,
        transaction: Transaction?,
        original: Transaction?,
    ): Boolean = matchesDateAndType(entry) &&
        (ticker == null || transaction?.ticker == ticker) &&
        (trade == null || sideOf(transaction, original) == trade)

    private fun matchesDateAndType(entry: CashEntry): Boolean = (from == null || !entry.date.isBefore(from)) &&
        (to == null || !entry.date.isAfter(to)) &&
        (type == null || entry.type == type)

    // A buy or sell, or a reversal of one, counts as the entry reversed; a split, a reversal of a split and a reversal
    // whose original is not there have no side.
    private fun sideOf(
        transaction: Transaction?,
        original: Transaction?,
    ): TradeSide? = when ((if (transaction?.type == TransactionType.REVERSAL) original else transaction)?.type) {
        TransactionType.BUY -> TradeSide.BUY
        TransactionType.SELL -> TradeSide.SELL
        else -> null
    }
}

/** One row of the Cash account (view 7): an entry and the real balance after it. */
data class CashRow(
    val entry: CashEntry,
    val balanceAfterCents: Long,
)

/** K3, read only: the Cash tab (spec 4.8, view 7). */
object CashAccount {
    /**
     * The balance on the latest entry date, so entries dated after today count (4.8). Throws
     * ArithmeticException instead of wrapping (4.10).
     */
    fun balanceCents(entries: List<CashEntry>): Long = CashRules.sumExact(entries)

    /**
     * The rows newest first: the 4.1 order (date, then entered_seq) reversed, so on a shared date the
     * later entry comes first. Each balance is the running balance in the 4.1 order. Throws
     * ArithmeticException instead of wrapping (4.10).
     */
    fun rows(entries: List<CashEntry>): List<CashRow> {
        var running = 0L
        return entries
            .sortedWith(compareBy<CashEntry>({ it.date }, { it.enteredSeq }))
            .map { entry ->
                running = Math.addExact(running, entry.amountCents)
                CashRow(entry, running)
            }.asReversed()
    }

    /**
     * The rows that match [filter], each still with the real balance after it: the balances are worked out on
     * the whole list first, then the rows are filtered (view 7: "the real balance, whatever the filters").
     */
    fun rows(
        entries: List<CashEntry>,
        filter: CashFilter,
    ): List<CashRow> = rows(entries).filter { filter.matches(it.entry) }

    /**
     * The rows that match [filter], each still with the real balance after it, judged with each trade line's
     * transaction (28: the ticker and the Buy and Sell chips). The transactions are found through one id map, so this
     * grows with the number of entries and transactions, not their product (15d).
     */
    fun rows(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        filter: CashFilter,
    ): List<CashRow> {
        val byId = byId(transactions)
        return rows(entries).filter { row ->
            val trade = tradeOf(row.entry, byId)
            val original = if (trade?.type == TransactionType.REVERSAL) trade.reversesId?.let(byId::get) else null
            filter.matches(row.entry, trade, original)
        }
    }

    /** The ticker picker (view 7): each ticker with a trade line, from every portfolio, once, in alphabetical order. */
    fun tickers(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
    ): List<String> {
        val byId = byId(transactions)
        return entries.mapNotNull { tradeOf(it, byId)?.ticker }.distinct().sorted()
    }

    // The first record with each id, as the Cash list's model finds them (15d).
    private fun byId(transactions: List<Transaction>): Map<String, Transaction> = buildMap {
        transactions.forEach { putIfAbsent(it.id, it) }
    }

    private fun tradeOf(
        entry: CashEntry,
        byId: Map<String, Transaction>,
    ): Transaction? = if (entry.type == CashType.TRADE) entry.transactionId?.let(byId::get) else null
}
