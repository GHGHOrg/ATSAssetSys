package app.ghghorg.atsassets.core

import java.time.LocalDate

/** A holding the user has hidden (spec 3, 4.4): a ticker within a portfolio. Hiding is not a transaction. */
data class HiddenHolding(
    val portfolioId: String,
    val ticker: String,
)

/** K1, rules only: when Hide is offered, which hidden holdings a save unhides, and a row's last date (4.4, view 9). */
object Hiding {
    /**
     * Hide is offered on a holding that exists, is not hidden, and has exactly 0 shares counting every entry, a
     * later-dated one included, so a hidden holding is always at 0 (4.4, section 8; 15 decision 1). A leftover too
     * small to show is not 0 (CR1).
     */
    fun offered(
        transactions: List<Transaction>,
        hidden: Set<HiddenHolding>,
        portfolioId: String,
        ticker: String,
    ): Boolean {
        if (HiddenHolding(portfolioId, ticker) in hidden) return false
        val shares = sharesAfterEveryEntry(transactions, portfolioId, ticker) ?: return false
        return shares.signum() == 0
    }

    /**
     * The hidden holdings that [transactions], as they are after a save, leave with shares counting every entry: the
     * save unhides them (4.4: a new buy, or the reversal of a sell). A holding with no buy has none.
     */
    fun unhiddenBy(
        transactions: List<Transaction>,
        hidden: Collection<HiddenHolding>,
    ): List<HiddenHolding> = hidden.filter { (sharesAfterEveryEntry(transactions, it.portfolioId, it.ticker)?.signum() ?: 0) > 0 }

    /** The date of the holding's last transaction, a reversal included (view 9, marker 2); null with none. */
    fun lastTransactionDate(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
    ): LocalDate? = transactions.filter { it.portfolioId == portfolioId && it.ticker == ticker }.maxOfOrNull { it.date }

    private fun sharesAfterEveryEntry(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
    ): Fraction? = Holdings.holdings(transactions, LocalDate.MAX).firstOrNull { it.portfolioId == portfolioId && it.ticker == ticker }?.shares
}
