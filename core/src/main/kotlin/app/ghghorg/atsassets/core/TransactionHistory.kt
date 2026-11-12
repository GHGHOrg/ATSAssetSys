package app.ghghorg.atsassets.core

import java.time.LocalDate

/** The type chips of Transaction history (view 8), one at a time; none is ever disabled. */
enum class HistoryChip {
    ALL,
    BUY,
    SELL,
    SPLIT,
    CASH,
}

/**
 * Transaction history's filters (view 8; 27), each optional and combined: a ticker of the portfolio, the date range
 * (both ends included, each open when null) and the chip.
 */
data class HistoryFilter(
    val ticker: String? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val chip: HistoryChip = HistoryChip.ALL,
)

/** One row of Transaction history (view 8; 27). */
sealed interface HistoryRow {
    /**
     * A buy, sell, split or reversal: for a reversal its [original], for a reversed entry its [reversal], and for a buy
     * or sell, or a reversal of one, its own [cashLine] (4.6: the chain is traceable both ways).
     */
    data class Entry(
        val transaction: Transaction,
        val original: Transaction?,
        val reversal: Transaction?,
        val cashLine: CashEntry?,
    ) : HistoryRow {
        /** The type the row counts as for the chips: a reversal counts as the entry it reverses (view 8). */
        val kind: TransactionType get() = original?.type ?: transaction.type
    }

    /** A cash line under the Cash chip: of [transaction], a buy or sell or a reversal of one ([original]). */
    data class Cash(
        val line: CashEntry,
        val transaction: Transaction,
        val original: Transaction?,
    ) : HistoryRow
}

/**
 * K3's Transaction history (spec section 6 view 8, section 14; 27): one portfolio's records, read and never written.
 * The pairs (original and reversal) and the cash lines are found in one pass, so the history grows with its size.
 */
object TransactionHistory {
    /** The tickers with a transaction in the portfolio, hidden holdings included, in alphabetical order. */
    fun tickers(
        transactions: List<Transaction>,
        portfolioId: String,
    ): List<String> = transactions.filter { it.portfolioId == portfolioId }.map { it.ticker }.distinct().sorted()

    /**
     * The portfolio's rows matching [filter], newest first, on a shared date the later entry first (4.1 reversed, as
     * the Cash list). Under Cash, one row per cash line of the portfolio's trades and their reversals; a direct cash
     * entry never shows (AC-10).
     */
    fun rows(
        transactions: List<Transaction>,
        entries: List<CashEntry>,
        portfolioId: String,
        filter: HistoryFilter,
    ): List<HistoryRow> {
        val own = transactions.filter { it.portfolioId == portfolioId }
        val byId = HashMap<String, Transaction>()
        val reversalOf = HashMap<String, Transaction>()
        for (transaction in own) {
            byId.putIfAbsent(transaction.id, transaction)
            val reversed = transaction.reversesId
            if (transaction.type == TransactionType.REVERSAL && reversed != null) reversalOf.putIfAbsent(reversed, transaction)
        }
        val lines = HashMap<String, CashEntry>()
        for (entry in entries) {
            val id = entry.transactionId
            if (entry.type == CashType.TRADE && id != null && id in byId) lines.putIfAbsent(id, entry)
        }
        val shown =
            own
                .filter { matches(it, filter) }
                .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.enteredSeq })
        return if (filter.chip == HistoryChip.CASH) {
            shown.mapNotNull { transaction ->
                lines[transaction.id]?.let { HistoryRow.Cash(it, transaction, transaction.reversesId?.let(byId::get)) }
            }
        } else {
            shown
                .map { HistoryRow.Entry(it, it.reversesId?.let(byId::get), reversalOf[it.id], lines[it.id]) }
                .filter { chipTakes(filter.chip, it.kind) }
        }
    }

    // The ticker and the range (both ends included); a trade's cash line carries its trade's date.
    private fun matches(
        transaction: Transaction,
        filter: HistoryFilter,
    ): Boolean = (filter.ticker == null || transaction.ticker == filter.ticker) &&
        (filter.from == null || !transaction.date.isBefore(filter.from)) &&
        (filter.to == null || !transaction.date.isAfter(filter.to))

    private fun chipTakes(
        chip: HistoryChip,
        kind: TransactionType,
    ): Boolean = when (chip) {
        HistoryChip.ALL -> true
        HistoryChip.BUY -> kind == TransactionType.BUY
        HistoryChip.SELL -> kind == TransactionType.SELL
        HistoryChip.SPLIT -> kind == TransactionType.SPLIT
        HistoryChip.CASH -> false
    }
}
