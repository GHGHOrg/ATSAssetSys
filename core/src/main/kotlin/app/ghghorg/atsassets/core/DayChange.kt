package app.ghghorg.atsassets.core

import java.time.LocalDate

/**
 * A 1-day gain/loss (CR5, 41; spec 4.8, C24, C25; K3): [change] over the day and the day's [start]ing value, both exact
 * (4.10). [start] is value − change, over the holdings that have a day figure.
 */
data class DayChange(
    val change: Fraction,
    val start: Fraction,
) {
    /** The change over the day's starting value; none at a start of $0.00 (4.8). */
    val percent: Fraction? get() = if (start.signum() == 0) null else change / start

    operator fun plus(other: DayChange): DayChange = DayChange(change + other.change, start + other.start)
}

object DayChanges {
    /** The day of [price]: its market date, its time in New York (C25). */
    fun dayOf(price: Price): LocalDate = price.time.atZone(MarketDate.ZONE).toLocalDate()

    /**
     * One holding's 1-day figure (4.8) from its [lots] with shares as of today, its [transactions] and its [price]; null
     * for no day figure: no price, no previous close, or a split of the holding dated on the day, or one reversed on it
     * (C24). A lot from a buy dated before the day counts from the previous close; one dated on or after it, from its own
     * cost per share, fees included (4.3). Shares sold are no longer in a lot, so a sale counts nowhere.
     */
    fun ofHolding(
        lots: List<Lot>,
        transactions: List<Transaction>,
        price: Price?,
    ): DayChange? {
        if (price == null) return null
        val previousClose = price.previousClose ?: return null
        val day = dayOf(price)
        if (splitOn(day, transactions)) return null
        val latest = Fraction.of(price.perShare)
        val close = Fraction.of(previousClose)
        return lots.fold(DayChange(Fraction.ZERO, Fraction.ZERO)) { sum, lot ->
            val from = if (lot.buyDate < day) close else lot.costPerShare
            sum + DayChange(lot.remainingQuantity * (latest - from), lot.remainingQuantity * from)
        }
    }

    /**
     * Every one of [holdings]' 1-day figures, by (portfolio id, ticker), each at its ticker's price in [prices]: one
     * ledger for all their lots as of [today], not one per holding (15c).
     */
    fun byHolding(
        transactions: List<Transaction>,
        holdings: List<Holding>,
        prices: Map<String, Price>,
        today: LocalDate,
    ): Map<Pair<String, String>, DayChange?> {
        val lots = Holdings.lots(transactions, today).filter { it.remainingQuantity.signum() > 0 }.groupBy { it.portfolioId to it.ticker }
        val ofHolding = transactions.groupBy { it.portfolioId to it.ticker }
        return holdings.associate { holding ->
            val key = holding.portfolioId to holding.ticker
            key to ofHolding(lots[key].orEmpty(), ofHolding[key].orEmpty(), prices[holding.ticker])
        }
    }

    /** A total's 1-day figure: the sum over the figures there are; null when none has one (the line is left out, 4.8). */
    fun total(figures: List<DayChange?>): DayChange? = figures.filterNotNull().takeIf { it.isNotEmpty() }?.reduce { sum, figure -> sum + figure }

    // A split, or the reversal of one, dated on the day changes the holding's shares that day (C24).
    private fun splitOn(
        day: LocalDate,
        transactions: List<Transaction>,
    ): Boolean {
        val splits = transactions.filter { it.type == TransactionType.SPLIT }.map { it.id }.toSet()
        return transactions.any { transaction ->
            transaction.date == day &&
                (transaction.type == TransactionType.SPLIT || (transaction.type == TransactionType.REVERSAL && transaction.reversesId in splits))
        }
    }
}
