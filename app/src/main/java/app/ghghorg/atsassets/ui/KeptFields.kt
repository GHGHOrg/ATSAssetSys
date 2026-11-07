package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Hiding
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Lot
import app.ghghorg.atsassets.core.LotChoice
import app.ghghorg.atsassets.core.Reversals
import app.ghghorg.atsassets.core.Sells
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.Transactions
import app.ghghorg.atsassets.core.Valuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * The fields of [AppState] the kept fields are worked out from (15c), as they were when [AppState.keptFrom] was taken.
 * A kept field is worked out again only when one of its own inputs differs.
 */
data class KeptInputs(
    val entries: List<CashEntry>,
    val transactions: List<Transaction>,
    val hidden: List<HiddenHolding>,
    val today: LocalDate,
    val portfolioId: String?,
    val holdingTicker: String?,
    val transactionId: String?,
    val sellPortfolioId: String?,
    val sellTicker: String?,
    val sellDateText: String,
    val replacingId: String?,
    val replaceReversalDate: String,
)

/** The kept fields of [AppState] (15c), each worked out here by one function, from its inputs only. */
internal object KeptFields {
    /** As of today (4.8), by portfolio, then ticker. */
    fun holdings(
        transactions: List<Transaction>,
        today: LocalDate,
    ): List<Holding> = Holdings.holdings(transactions, today)

    /** Cash as of today plus every holding at cost (4.8), from [holdings] already worked out: one ledger, not two. */
    fun netWorth(
        entries: List<CashEntry>,
        holdings: List<Holding>,
        today: LocalDate,
    ): Fraction = Valuation.netWorthOf(entries, holdings, today)

    /** The tickers [portfolioId] holds with shares as of today, a hidden holding left out (4.4), in ticker order. */
    fun heldTickers(
        holdings: List<Holding>,
        hidden: List<HiddenHolding>,
        portfolioId: String,
    ): List<String> = holdings
        .filter { it.portfolioId == portfolioId && HiddenHolding(portfolioId, it.ticker) !in hidden && it.shares.signum() > 0 }
        .map { it.ticker }

    /** The shown holding's lots with shares as of today, oldest first (view 6, 4.4). */
    fun holdingLots(
        transactions: List<Transaction>,
        portfolioId: String?,
        ticker: String?,
        today: LocalDate,
    ): List<Lot> {
        if (portfolioId == null || ticker == null) return emptyList()
        return Holdings.lotsOf(transactions, portfolioId, ticker, today)
    }

    /** Hide on the shown holding: only at exactly 0 shares counting every entry, and not while hidden (15 decision 1). */
    fun hideOffered(
        transactions: List<Transaction>,
        hidden: List<HiddenHolding>,
        portfolioId: String?,
        ticker: String?,
    ): Boolean {
        if (portfolioId == null || ticker == null) return false
        return Hiding.offered(transactions, hidden.toSet(), portfolioId, ticker)
    }

    /**
     * The tickers the sell's portfolio holds, with shares as of today, in ticker order (11 decision 1). While a sell is
     * replaced, with its reversal applied, so a sold-out holding is offered again (14a).
     */
    fun sellTickers(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        holdings: List<Holding>,
        hidden: List<HiddenHolding>,
        sellPortfolioId: String?,
        replacingId: String?,
        replaceReversalDate: String,
        today: LocalDate,
    ): List<String> {
        val id = sellPortfolioId ?: return emptyList()
        if (replacingId == null) return heldTickers(holdings, hidden, id)
        val after = sellTransactions(entries, transactions, replacingId, replaceReversalDate)
        return Holdings.holdings(after, today).filter { it.portfolioId == id && it.shares.signum() > 0 }.map { it.ticker }
    }

    /**
     * The sell's lot list (4.4) for its date, or for today while the date is not a whole date. While a sell is
     * replaced, the lots are as they will be after the reversal (10c marker 9).
     */
    fun sellLotChoices(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        sellPortfolioId: String?,
        sellTicker: String?,
        sellDateText: String,
        replacingId: String?,
        replaceReversalDate: String,
        today: LocalDate,
    ): List<LotChoice> {
        val id = sellPortfolioId ?: return emptyList()
        val ticker = sellTicker ?: return emptyList()
        val date = DateDigits.toDate(sellDateText) ?: today
        val after = sellTransactions(entries, transactions, replacingId, replaceReversalDate)
        return Sells.lotChoices(after, id, ticker, date, Transactions.nextSeq(entries, after))
    }

    /** What is left of the shown buy's lot as of today, split-adjusted; null for a buy dated after today (13a decision 2). */
    fun transactionLot(
        transactions: List<Transaction>,
        transactionId: String?,
        today: LocalDate,
    ): Lot? {
        val buy = transactionId?.let { id -> transactions.firstOrNull { it.id == id } }?.takeIf { it.type == TransactionType.BUY } ?: return null
        return Holdings.lots(transactions, today).firstOrNull { it.buyId == buy.id }
    }

    // The transactions a sell is entered against: with the replaced sell's reversal drafted at its date (14a).
    private fun sellTransactions(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        replacingId: String?,
        replaceReversalDate: String,
    ): List<Transaction> {
        val original = replacingId?.let { id -> transactions.firstOrNull { it.id == id } } ?: return transactions
        val date = DateDigits.toDate(replaceReversalDate) ?: original.date
        return transactions + Reversals.draft(entries, transactions, original, PREVIEW_ID, date)
    }
}

/** The inputs of the kept fields as they are now. */
internal fun AppState.keptInputs(): KeptInputs = KeptInputs(
    entries = entries,
    transactions = transactions,
    hidden = hidden,
    today = today,
    portfolioId = portfolioId,
    holdingTicker = holdingTicker,
    transactionId = transactionId,
    sellPortfolioId = sellForm.portfolioId,
    sellTicker = sellForm.ticker,
    sellDateText = sellForm.dateText,
    replacingId = replacingId,
    replaceReversalDate = replaceReversalDate,
)

/**
 * This state with each kept field worked out again whose inputs differ from [AppState.keptFrom]; every other kept field
 * stays the same object. A second call changes nothing, so it is safe at any point.
 */
internal fun AppState.refreshed(): AppState {
    val now = keptInputs()
    val was = keptFrom
    if (now == was) return this

    fun changed(vararg inputs: (KeptInputs) -> Any?): Boolean = inputs.any { it(now) != it(was) }
    val holdingsChanged = changed({ it.transactions }, { it.today })
    val holdings = if (holdingsChanged) KeptFields.holdings(transactions, today) else holdings
    return copy(
        holdings = holdings,
        netWorth = if (holdingsChanged || changed({ it.entries })) KeptFields.netWorth(entries, holdings, today) else netWorth,
        holdingLots =
        if (changed({ it.transactions }, { it.portfolioId }, { it.holdingTicker }, { it.today })) {
            KeptFields.holdingLots(transactions, portfolioId, holdingTicker, today)
        } else {
            holdingLots
        },
        hideOffered =
        if (changed({ it.transactions }, { it.hidden }, { it.portfolioId }, { it.holdingTicker })) {
            KeptFields.hideOffered(transactions, hidden, portfolioId, holdingTicker)
        } else {
            hideOffered
        },
        sellTickers =
        if (
            changed(
                { it.entries },
                { it.transactions },
                { it.today },
                { it.hidden },
                { it.sellPortfolioId },
                { it.replacingId },
                { it.replaceReversalDate },
            )
        ) {
            KeptFields.sellTickers(entries, transactions, holdings, hidden, sellForm.portfolioId, replacingId, replaceReversalDate, today)
        } else {
            sellTickers
        },
        sellLotChoices =
        if (
            changed(
                { it.entries },
                { it.transactions },
                { it.today },
                { it.sellPortfolioId },
                { it.sellTicker },
                { it.sellDateText },
                { it.replacingId },
                { it.replaceReversalDate },
            )
        ) {
            KeptFields.sellLotChoices(
                entries,
                transactions,
                sellForm.portfolioId,
                sellForm.ticker,
                sellForm.dateText,
                replacingId,
                replaceReversalDate,
                today,
            )
        } else {
            sellLotChoices
        },
        transactionLot =
        if (changed({ it.transactions }, { it.transactionId }, { it.today })) {
            KeptFields.transactionLot(transactions, transactionId, today)
        } else {
            transactionLot
        },
        keptFrom = now,
    )
}

/**
 * The screen state's only way in (15c): every write is [refreshed], so no action can leave a kept field stale. Its
 * members have MutableStateFlow's names, so the actions read as before.
 */
internal class KeptStateFlow(
    initial: AppState,
) {
    private val flow = MutableStateFlow(initial.refreshed())

    val readOnly: StateFlow<AppState> = flow.asStateFlow()

    var value: AppState
        get() = flow.value
        set(next) {
            flow.value = next.refreshed()
        }

    fun update(transform: (AppState) -> AppState) = flow.update { transform(it).refreshed() }
}
