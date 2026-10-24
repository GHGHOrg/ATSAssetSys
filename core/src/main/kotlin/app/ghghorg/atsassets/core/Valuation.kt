package app.ghghorg.atsassets.core

import java.time.LocalDate

/** K3, cash only (spec 4.8): net worth is the cash balance as of today. */
object Valuation {
    fun netWorthCents(
        entries: List<CashEntry>,
        today: LocalDate,
    ): Long = CashRules.balanceOn(entries, today)
}
