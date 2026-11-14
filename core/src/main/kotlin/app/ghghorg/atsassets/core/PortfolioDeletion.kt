package app.ghghorg.atsassets.core

import java.time.LocalDate

/**
 * What deleting a portfolio removes (4.7; 29): the portfolio, its transactions (buys, sells, splits and reversals),
 * their trade cash lines, and its own hidden-holding rows and overrides (decision 1). Ids only; K2 removes them as one
 * action.
 */
data class PortfolioRemoval(
    val portfolioId: String,
    val transactionIds: Set<String>,
    val cashEntryIds: Set<String>,
    val hidden: List<HiddenHolding>,
    val overrides: List<ClassOverride>,
)

/**
 * The summary shown before a deletion (4.7, wireframe 10d frame D): the number of transactions and of trade cash lines
 * removed, the realized gain that goes with the portfolio (a reversed sale left out, view 5), and the cash balance on
 * the latest entry date now and after the removal (4.8).
 */
data class DeletionSummary(
    val portfolio: Portfolio,
    val transactionCount: Int,
    val tradeLineCount: Int,
    val realizedGain: Fraction,
    val balanceNowCents: Long,
    val balanceAfterCents: Long,
)

/** The deletion check of 4.7 (29). */
sealed interface DeletionCheck {
    /** The deletion may go ahead: the summary to show and what it removes. */
    data class Allowed(
        val summary: DeletionSummary,
        val removal: PortfolioRemoval,
    ) : DeletionCheck

    /**
     * Without the portfolio's trade cash lines the cash would be negative from [firstDate]; [largestShortfallCents] is
     * what a deposit adjustment dated before [firstDate] must at least add (4.7, Example E).
     */
    data class Blocked(
        val firstDate: LocalDate,
        val largestShortfallCents: Long,
    ) : DeletionCheck

    /** As [Blocked], but [firstDate] is the opening balance's date, so nothing can fix it (4.7, 4.2). */
    data class CannotDelete(
        val firstDate: LocalDate,
        val largestShortfallCents: Long,
    ) : DeletionCheck

    /** The balance would leave the range of exact cents (4.10). */
    data object TooLarge : DeletionCheck

    data object NotFound : DeletionCheck
}

/**
 * K1's portfolio deletion (spec 4.7, section 14; 29): judges whether a portfolio may be deleted and what that removes.
 * Pure; the store removes the records (K2).
 */
object PortfolioDeletion {
    /**
     * Judges deleting [portfolioId] (4.7). Its transactions are those with its id, reversals included (a reversal takes
     * its original's portfolio, 13b); its trade cash lines are the `trade` entries of those transactions, so reversals'
     * lines go too, while a direct cash entry and its reversal stay. The entries left are replayed in the 4.1 order: the
     * first entry after which the balance is below 0 gives the first date (03's rule, so a deposit dated that date comes
     * too late), and the lowest balance from there on gives the largest shortfall. A first date on the opening
     * balance's date cannot be fixed (4.2).
     */
    fun check(
        portfolios: List<Portfolio>,
        transactions: List<Transaction>,
        entries: List<CashEntry>,
        hidden: List<HiddenHolding>,
        overrides: List<ClassOverride>,
        portfolioId: String,
    ): DeletionCheck {
        val portfolio = portfolios.firstOrNull { it.id == portfolioId } ?: return DeletionCheck.NotFound
        val own = transactions.filter { it.portfolioId == portfolioId }
        val ownIds = own.map { it.id }.toSet()
        val lines = entries.filter { it.type == CashType.TRADE && it.transactionId in ownIds }
        val lineIds = lines.map { it.id }.toSet()
        val left = entries.filter { it.id !in lineIds }
        val (now, after) =
            try {
                CashAccount.balanceCents(entries) to CashAccount.balanceCents(left)
            } catch (e: ArithmeticException) {
                return DeletionCheck.TooLarge
            }
        negative(left)?.let { return it }
        val gain =
            RealizedGains
                .report(transactions, portfolios, null, null)
                .holdings
                .filter { it.portfolioId == portfolioId }
                .fold(Fraction.ZERO) { sum, holding -> sum + holding.total }
        return DeletionCheck.Allowed(
            summary =
            DeletionSummary(
                portfolio = portfolio,
                transactionCount = own.size,
                tradeLineCount = lines.size,
                realizedGain = gain,
                balanceNowCents = now,
                balanceAfterCents = after,
            ),
            removal =
            PortfolioRemoval(
                portfolioId = portfolioId,
                transactionIds = ownIds,
                cashEntryIds = lineIds,
                hidden = hidden.filter { it.portfolioId == portfolioId },
                overrides = overrides.filter { it.portfolioId == portfolioId },
            ),
        )
    }

    /** The typed confirmation (4.7, AC-55): the portfolio's name, case and spaces ignored as for names (D7). */
    fun nameMatches(
        portfolio: Portfolio,
        typed: String,
    ): Boolean = typed.isNotBlank() && Portfolios.nameKey(typed) == Portfolios.nameKey(portfolio.name)

    // The replay of the entries left: null when the balance never goes below 0.
    private fun negative(left: List<CashEntry>): DeletionCheck? {
        val opening = left.firstOrNull { it.type == CashType.OPENING_BALANCE }?.date
        var balance = 0L
        var firstDate: LocalDate? = null
        var lowest = 0L
        try {
            for (entry in left.sortedWith(compareBy<CashEntry>({ it.date }, { it.enteredSeq }))) {
                balance = Math.addExact(balance, entry.amountCents)
                if (balance < 0L) {
                    if (firstDate == null) firstDate = entry.date
                    if (balance < lowest) lowest = balance
                }
            }
        } catch (e: ArithmeticException) {
            return DeletionCheck.TooLarge
        }
        val first = firstDate ?: return null
        val shortfall = -lowest
        return if (first == opening) DeletionCheck.CannotDelete(first, shortfall) else DeletionCheck.Blocked(first, shortfall)
    }
}
