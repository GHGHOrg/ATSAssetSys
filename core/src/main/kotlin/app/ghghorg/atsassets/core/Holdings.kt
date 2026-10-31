package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate

/**
 * A lot (spec section 3), worked out from its buy by replay; there is no lot table (09 decision 7). Sells (11)
 * and splits (12) change the remaining quantity; the cost is the buy's total scaled by remaining over original.
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
) {
    /** Lot cost over the shares left, exact (spec 3, 4.10); rounded only when shown. */
    val costPerShare: Fraction get() = cost / remainingQuantity
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
    /** The lots of the buys dated on or before [asOf], in the 4.1 order (date, then entered_seq). */
    fun lots(
        transactions: List<Transaction>,
        asOf: LocalDate,
    ): List<Lot> = transactions
        .filter { it.type == TransactionType.BUY && !it.date.isAfter(asOf) }
        .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
        .map { buy ->
            Lot(
                buyId = buy.id,
                portfolioId = buy.portfolioId,
                ticker = buy.ticker,
                buyDate = buy.date,
                enteredSeq = buy.enteredSeq,
                originalQuantity = buy.quantity,
                remainingQuantity = Fraction.of(buy.quantity),
                cost = Fraction.ofCents(buy.totalCents),
            )
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

    /** One holding's lots as of [asOf], oldest first (4.4, view 6). */
    fun lotsOf(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        asOf: LocalDate,
    ): List<Lot> = lots(transactions, asOf).filter { it.portfolioId == portfolioId && it.ticker == ticker }

    /** One holding per portfolio and ticker, with the sums of its lots; by portfolio id, then in ticker order. */
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
