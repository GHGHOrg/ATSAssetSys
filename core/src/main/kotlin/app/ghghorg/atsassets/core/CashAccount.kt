package app.ghghorg.atsassets.core

/** One row of the Cash account (view 7): an entry and the real balance after it. */
data class CashRow(
    val entry: CashEntry,
    val balanceAfterCents: Long,
)

/** K3, read only: the Cash tab (spec 4.8, view 7). */
object CashAccount {
    /**
     * The balance on the latest entry date, so entries dated after today count (4.8). The sum is not
     * checked for overflow: with one opening balance it cannot overflow. Iteration 03 must guard it.
     */
    fun balanceCents(entries: List<CashEntry>): Long = entries.sumOf { it.amountCents }

    /**
     * The rows newest first: the 4.1 order (date, then entered_seq) reversed, so on a shared date the
     * later entry comes first. Each balance is the running balance in the 4.1 order.
     */
    fun rows(entries: List<CashEntry>): List<CashRow> {
        var running = 0L
        return entries
            .sortedWith(compareBy<CashEntry>({ it.date }, { it.enteredSeq }))
            .map { entry ->
                running += entry.amountCents
                CashRow(entry, running)
            }.asReversed()
    }
}
