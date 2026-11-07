package app.ghghorg.atsassets.core

import java.time.LocalDate

/** K3 (spec 4.8). There are no prices before iteration 16, so every holding is valued at cost ("no price"). */
object Valuation {
    /** The cash part of net worth: the cash balance as of today. Since iteration 09 the screens use [netWorth]. */
    fun netWorthCents(
        entries: List<CashEntry>,
        today: LocalDate,
    ): Long = CashRules.balanceOn(entries, today)

    /** The cash balance as of today plus every holding at cost, both as of today (4.8). */
    fun netWorth(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        today: LocalDate,
    ): Fraction = netWorthOf(entries, Holdings.holdings(transactions, today), today)

    /** [netWorth] from [holdings] already worked out as of [today], so the app builds them only once (15c). */
    fun netWorthOf(
        entries: List<CashEntry>,
        holdings: List<Holding>,
        today: LocalDate,
    ): Fraction {
        val cash = Fraction.ofCents(CashRules.balanceOn(entries, today))
        return holdings.fold(cash) { sum, holding -> sum + holding.cost }
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
