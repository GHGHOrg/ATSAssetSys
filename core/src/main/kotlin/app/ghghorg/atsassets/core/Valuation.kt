package app.ghghorg.atsassets.core

import java.time.LocalDate

/** K3 (spec 4.8). There are no prices before iteration 16, so every holding is valued at cost ("no price"). */
object Valuation {
    /** Cash only; the screens use it until iteration 09 step 7 moves them to [netWorth]. */
    fun netWorthCents(
        entries: List<CashEntry>,
        today: LocalDate,
    ): Long = CashRules.balanceOn(entries, today)

    /** The cash balance as of today plus every holding at cost, both as of today (4.8). */
    fun netWorth(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        today: LocalDate,
    ): Fraction {
        val cash = Fraction.ofCents(CashRules.balanceOn(entries, today))
        return Holdings.holdings(transactions, today).fold(cash) { sum, holding -> sum + holding.cost }
    }

    /** The value of one portfolio's holdings as of today, each at cost while it has no price (4.8). */
    fun portfolioValue(
        transactions: List<Transaction>,
        portfolioId: String,
        today: LocalDate,
    ): Fraction = Holdings
        .holdings(transactions, today)
        .filter { it.portfolioId == portfolioId }
        .fold(Fraction.ZERO) { sum, holding -> sum + holding.cost }
}
