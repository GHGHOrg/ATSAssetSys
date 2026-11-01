package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate

/**
 * A lot (spec section 3), worked out from its buy by replay; there is no lot table (09 decision 7). Sells (11)
 * and splits (12) change the remaining quantity; the cost is the buy's total scaled by remaining over the original
 * quantity with the splits since (4.5: a split never changes a lot's total cost). A sold-out lot has 0 left and cost 0.
 */
data class Lot(
    val buyId: String,
    val portfolioId: String,
    val ticker: String,
    val buyDate: LocalDate,
    val enteredSeq: Long,
    val originalQuantity: BigDecimal,
    val remainingQuantity: Fraction,
    val cost: Fraction,
    val originalCost: Fraction,
    val splitFactor: Fraction,
) {
    /** The buy's total over its quantity with the splits since, exact (spec 3, 4.10); a sell does not change it. */
    val costPerShare: Fraction get() = originalCost / (Fraction.of(originalQuantity) * splitFactor)
}

/** A holding: a ticker within a portfolio (spec section 3), with the shares and cost of its lots (4.8). */
data class Holding(
    val portfolioId: String,
    val ticker: String,
    val shares: Fraction,
    val cost: Fraction,
)

/** A place in the 4.1 order: a date, then the order of entry. */
internal data class Place(
    val date: LocalDate,
    val seq: Long,
) : Comparable<Place> {
    override fun compareTo(other: Place): Int = compareValuesBy(this, other, { it.date }, { it.seq })

    companion object {
        /** After everything dated [date]. */
        fun endOf(date: LocalDate): Place = Place(date, Long.MAX_VALUE)

        /** After everything. */
        val END: Place = endOf(LocalDate.MAX)
    }
}

internal val Transaction.place: Place get() = Place(date, enteredSeq)

/** K3, read only: lots and holdings from the transactions, as of a date (4.8). */
object Holdings {
    /**
     * The lots of the buys dated on or before [asOf], in the 4.1 order (date, then entered_seq), with the splits and
     * sells dated on or before [asOf] applied. Sold-out lots are included, at 0.
     */
    fun lots(
        transactions: List<Transaction>,
        asOf: LocalDate,
    ): List<Lot> {
        val at = Place.endOf(asOf)
        return transactions
            .filter { it.type == TransactionType.BUY && !it.date.isAfter(asOf) }
            .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
            .map { buy ->
                val factor = splitFactor(transactions, buy.portfolioId, buy.ticker, buy.place, at)
                val remaining = quantityAt(transactions, buy, at)
                Lot(
                    buyId = buy.id,
                    portfolioId = buy.portfolioId,
                    ticker = buy.ticker,
                    buyDate = buy.date,
                    enteredSeq = buy.enteredSeq,
                    originalQuantity = buy.quantity,
                    remainingQuantity = remaining,
                    cost = Fraction.ofCents(buy.totalCents) * remaining / (Fraction.of(buy.quantity) * factor),
                    originalCost = Fraction.ofCents(buy.totalCents),
                    splitFactor = factor,
                )
            }
    }

    /**
     * The product of the ratios of one holding's splits placed after [after] and not after [upTo] (4.5): what a
     * quantity in the units of [after] is multiplied by to be in the units of [upTo].
     */
    internal fun splitFactor(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        after: Place,
        upTo: Place,
    ): Fraction = transactions
        .filter { it.type == TransactionType.SPLIT && it.portfolioId == portfolioId && it.ticker == ticker }
        .filter { it.place > after && it.place <= upTo }
        .fold(Fraction.ONE) { factor, split -> factor * (split.split?.factor ?: Fraction.ONE) }

    /**
     * What the lot of [buy] holds at [at], in the units of [at]: its quantity as traded with the splits since, less
     * what each sell placed up to [at] took from it, each allocation (in its sell's units) with the splits since (4.10).
     */
    internal fun quantityAt(
        transactions: List<Transaction>,
        buy: Transaction,
        at: Place,
    ): Fraction {
        fun since(from: Place) = splitFactor(transactions, buy.portfolioId, buy.ticker, from, at)
        return transactions
            .filter { it.type == TransactionType.SELL && it.place <= at }
            .fold(Fraction.of(buy.quantity) * since(buy.place)) { left, sell ->
                sell.allocations.filter { it.lotId == buy.id }.fold(left) { rest, allocation -> rest - allocation.quantity * since(sell.place) }
            }
    }

    /**
     * One holding's transactions for Holding detail (view 6): newest first, and on a shared date the later entry first,
     * as the Cash list (view 7). Entries dated after today are listed too: a history shows everything.
     */
    fun transactionsOf(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
    ): List<Transaction> = transactions
        .filter { it.portfolioId == portfolioId && it.ticker == ticker }
        .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.enteredSeq })

    /** One holding's lots that still have shares as of [asOf], oldest first (4.4, view 6: sold-out lots are not listed). */
    fun lotsOf(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        asOf: LocalDate,
    ): List<Lot> = lots(transactions, asOf).filter {
        it.portfolioId == portfolioId && it.ticker == ticker && it.remainingQuantity.signum() > 0
    }

    /**
     * One holding per portfolio and ticker, with the sums of its lots; by portfolio id, then in ticker order. A sold-out
     * holding stays, at 0 shares and cost 0 (4.4, view 3).
     */
    fun holdings(
        transactions: List<Transaction>,
        asOf: LocalDate,
    ): List<Holding> = lots(transactions, asOf)
        .groupBy { it.portfolioId to it.ticker }
        .map { (key, lots) ->
            Holding(
                portfolioId = key.first,
                ticker = key.second,
                shares = lots.fold(Fraction.ZERO) { sum, lot -> sum + lot.remainingQuantity },
                cost = lots.fold(Fraction.ZERO) { sum, lot -> sum + lot.cost },
            )
        }.sortedWith(compareBy<Holding>({ it.portfolioId }, { it.ticker }))
}
