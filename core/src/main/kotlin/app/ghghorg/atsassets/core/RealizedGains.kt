package app.ghghorg.atsassets.core

import java.time.LocalDate

/**
 * One sale in the realized gains report (view 5; 26): the [sell], its proceeds (the sell's total) and the cost of the
 * lots it took on its date, each exact (4.4, 4.10).
 */
data class RealizedSale(
    val sell: Transaction,
    val proceeds: Fraction,
    val cost: Fraction,
) {
    val gain: Fraction get() = proceeds - cost
}

/** One holding (a ticker within a portfolio) with its sales in the range, newest first (view 5; 26). */
data class RealizedHolding(
    val portfolioId: String,
    val ticker: String,
    val sales: List<RealizedSale>,
) {
    /** The sum of its sales' exact gains (4.10). */
    val total: Fraction get() = sales.fold(Fraction.ZERO) { sum, sale -> sum + sale.gain }
}

/** The realized gains of a range (view 5; 26): the holdings with a sale in it, in ticker order. */
data class RealizedReport(
    val holdings: List<RealizedHolding>,
) {
    /** The sum of every sale's exact gain (4.10). */
    val total: Fraction get() = holdings.fold(Fraction.ZERO) { sum, holding -> sum + holding.total }

    /** The number of sales in the range. */
    val count: Int get() = holdings.sumOf { it.sales.size }
}

/**
 * K3's realized gains (spec section 6 view 5, section 14; 26): reads the transactions, never writes them. A sell that a
 * reversal names is left out whatever the range (4.6); every other sell counts on its own date.
 */
object RealizedGains {
    /**
     * The sales dated from [from] to [to], both included, each end open when null (view 5), by holding: in ticker order,
     * then in the order of [portfolios] (the order they were created), each holding's sales newest first (then the
     * latest entered first). Hidden holdings count (view 5); a holding with no sale in the range is not listed.
     */
    fun report(
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        from: LocalDate?,
        to: LocalDate?,
    ): RealizedReport {
        val reversed = transactions.filter { it.type == TransactionType.REVERSAL }.mapNotNull { it.reversesId }.toSet()
        val sells =
            transactions.filter { sell ->
                sell.type == TransactionType.SELL &&
                    sell.id !in reversed &&
                    (from == null || !sell.date.isBefore(from)) &&
                    (to == null || !sell.date.isAfter(to))
            }
        if (sells.isEmpty()) return RealizedReport(emptyList())
        val ledger = Ledger(transactions)
        val buys = transactions.filter { it.type == TransactionType.BUY }.associateBy { it.id }
        val order = portfolios.sortedBy { it.createdSeq }.mapIndexed { index, portfolio -> portfolio.id to index }.toMap()
        val holdings =
            sells
                .groupBy { it.portfolioId to it.ticker }
                .map { (key, ofHolding) ->
                    val sales =
                        ofHolding
                            .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.enteredSeq })
                            .map { sell ->
                                val cost = Sells.pieces(ledger, buys, sell).fold(Fraction.ZERO) { sum, piece -> sum + piece.cost }
                                RealizedSale(sell, Fraction.ofCents(sell.totalCents), cost)
                            }
                    RealizedHolding(key.first, key.second, sales)
                }.sortedWith(compareBy<RealizedHolding>({ it.ticker }, { order[it.portfolioId] ?: Int.MAX_VALUE }))
        return RealizedReport(holdings)
    }
}
