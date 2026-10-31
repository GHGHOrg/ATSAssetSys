package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate

/**
 * A lot (spec section 3), worked out from its buy by replay; there is no lot table (09 decision 7). Sells (11)
 * and splits (12) change the remaining quantity; the cost is the buy's total scaled by remaining over original.
 * A sold-out lot has 0 left and cost 0.
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
) {
    /** The buy's total over its quantity, exact (spec 3, 4.10); a sell does not change it. Rounded only when shown. */
    val costPerShare: Fraction get() = originalCost / Fraction.of(originalQuantity)
}

/** A holding: a ticker within a portfolio (spec section 3), with the shares and cost of its lots (4.8). */
data class Holding(
    val portfolioId: String,
    val ticker: String,
    val shares: Fraction,
    val cost: Fraction,
)

/** K3, read only: lots and holdings from the transactions, as of a date (4.8). */
object Holdings {
    /**
     * The lots of the buys dated on or before [asOf], in the 4.1 order (date, then entered_seq), less what the sells
     * dated on or before [asOf] took from them. Sold-out lots are included, at 0.
     */
    fun lots(
        transactions: List<Transaction>,
        asOf: LocalDate,
    ): List<Lot> {
        val taken = takenFromLots(transactions.filter { it.type == TransactionType.SELL && !it.date.isAfter(asOf) })
        return transactions
            .filter { it.type == TransactionType.BUY && !it.date.isAfter(asOf) }
            .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
            .map { buy ->
                val original = Fraction.of(buy.quantity)
                val remaining = original - (taken[buy.id] ?: Fraction.ZERO)
                Lot(
                    buyId = buy.id,
                    portfolioId = buy.portfolioId,
                    ticker = buy.ticker,
                    buyDate = buy.date,
                    enteredSeq = buy.enteredSeq,
                    originalQuantity = buy.quantity,
                    remainingQuantity = remaining,
                    cost = Fraction.ofCents(buy.totalCents) * remaining / original,
                    originalCost = Fraction.ofCents(buy.totalCents),
                )
            }
    }

    /** The exact quantity the [sells] took from each lot, by the lot's buy id. */
    internal fun takenFromLots(sells: List<Transaction>): Map<String, Fraction> = sells
        .flatMap { it.allocations }
        .groupingBy { it.lotId }
        .fold(Fraction.ZERO) { sum, allocation -> sum + allocation.quantity }

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
