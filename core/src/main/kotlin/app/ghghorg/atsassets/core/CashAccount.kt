package app.ghghorg.atsassets.core

import java.time.LocalDate

/**
 * The Cash list filters (view 7): a date range, both ends included, and one type; null means no bound and All.
 * Combined: a row must match all of them. Never stored (iteration 06 decision 4).
 */
data class CashFilter(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val type: CashType? = null,
) {
    val isActive: Boolean get() = from != null || to != null || type != null

    fun matches(entry: CashEntry): Boolean = (from == null || !entry.date.isBefore(from)) &&
        (to == null || !entry.date.isAfter(to)) &&
        (type == null || entry.type == type)
}

/** One row of the Cash account (view 7): an entry and the real balance after it. */
data class CashRow(
    val entry: CashEntry,
    val balanceAfterCents: Long,
)

/** K3, read only: the Cash tab (spec 4.8, view 7). */
object CashAccount {
    /**
     * The balance on the latest entry date, so entries dated after today count (4.8). Throws
     * ArithmeticException instead of wrapping (4.10).
     */
    fun balanceCents(entries: List<CashEntry>): Long = CashRules.sumExact(entries)

    /**
     * The rows newest first: the 4.1 order (date, then entered_seq) reversed, so on a shared date the
     * later entry comes first. Each balance is the running balance in the 4.1 order. Throws
     * ArithmeticException instead of wrapping (4.10).
     */
    fun rows(entries: List<CashEntry>): List<CashRow> {
        var running = 0L
        return entries
            .sortedWith(compareBy<CashEntry>({ it.date }, { it.enteredSeq }))
            .map { entry ->
                running = Math.addExact(running, entry.amountCents)
                CashRow(entry, running)
            }.asReversed()
    }

    /**
     * The rows that match [filter], each still with the real balance after it: the balances are worked out on
     * the whole list first, then the rows are filtered (view 7: "the real balance, whatever the filters").
     */
    fun rows(
        entries: List<CashEntry>,
        filter: CashFilter,
    ): List<CashRow> = rows(entries).filter { filter.matches(it.entry) }
}
