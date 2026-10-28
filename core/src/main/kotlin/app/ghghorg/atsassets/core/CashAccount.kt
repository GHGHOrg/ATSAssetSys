package app.ghghorg.atsassets.core

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
}
