package app.ghghorg.atsassets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.BuyRefusal
import app.ghghorg.atsassets.core.BuyResult
import app.ghghorg.atsassets.core.CashAccount
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntries
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashEntryResult
import app.ghghorg.atsassets.core.CashFilter
import app.ghghorg.atsassets.core.CashRow
import app.ghghorg.atsassets.core.CashRules
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Lot
import app.ghghorg.atsassets.core.LotChoice
import app.ghghorg.atsassets.core.MarketDate
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.OpeningBalance
import app.ghghorg.atsassets.core.OpeningBalanceError
import app.ghghorg.atsassets.core.OpeningBalanceResult
import app.ghghorg.atsassets.core.Pick
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PortfolioRefusal
import app.ghghorg.atsassets.core.PortfolioRenameResult
import app.ghghorg.atsassets.core.PortfolioResult
import app.ghghorg.atsassets.core.Portfolios
import app.ghghorg.atsassets.core.PriceSource
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.QuantityError
import app.ghghorg.atsassets.core.QuantityResult
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.Reentries
import app.ghghorg.atsassets.core.ReentryIds
import app.ghghorg.atsassets.core.ReentryRefusal
import app.ghghorg.atsassets.core.ReentryResult
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.ReversalRefusal
import app.ghghorg.atsassets.core.ReversalResult
import app.ghghorg.atsassets.core.Reversals
import app.ghghorg.atsassets.core.SellRefusal
import app.ghghorg.atsassets.core.SellResult
import app.ghghorg.atsassets.core.Sells
import app.ghghorg.atsassets.core.SoldPiece
import app.ghghorg.atsassets.core.SplitEffect
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.SplitRefusal
import app.ghghorg.atsassets.core.SplitResult
import app.ghghorg.atsassets.core.Splits
import app.ghghorg.atsassets.core.TickerCheck
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.Transactions
import app.ghghorg.atsassets.core.UnreadableDataException
import app.ghghorg.atsassets.core.Valuation
import app.ghghorg.atsassets.prices.CheckRefusal
import app.ghghorg.atsassets.prices.TickerVerifier
import app.ghghorg.atsassets.prices.Verification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.Locale

/** The one screen shown at a time. TABS shows the selected tab under the bottom bar. */
enum class Screen {
    LOADING,
    UNREADABLE,
    TABS,
    FORM,
    REVIEW,
    DETAIL,
    REVERSAL,

    // One portfolio (view 3, wireframe 3 frame B), opened from its row (iteration 08).
    PORTFOLIO,

    // Buy entry and its review (wireframe 10a frames A and B, iteration 09).
    BUY,
    BUY_REVIEW,

    // One holding (view 6, wireframe 06), opened from a portfolio's holdings (iteration 10).
    HOLDING,

    // Sell entry with the lot picker, and its review (wireframe 10a frames C to E, iteration 11).
    SELL,
    SELL_REVIEW,

    // Split entry and its review (wireframe 10b frames E and F, iteration 12), opened from a holding's header only.
    SPLIT,
    SPLIT_REVIEW,

    // A buy, sell or split (wireframe 10b frame G, iteration 13a), opened from Holding detail or a cash trade line.
    TRANSACTION,

    // The reversal of the shown transaction (wireframe 10b frame I, iteration 13b): one screen, no review.
    REVERSE_TRANSACTION,

    // Reverse and re-enter's combined review (wireframe 10c frames B and D, iteration 14a); the entry is BUY or SELL.
    REPLACE_REVIEW,
}

// The id of the unsaved draft the reversal screen shows the effect of; never saved.
private const val PREVIEW_ID = "preview"

// The ids of a pair K1 checks at Review or while the reversal date is typed; never saved. Distinct, because the cash
// replay leaves the reversal's line unjudged by its id on a shared date (14a decision 5).
private val PROBE_IDS = ReentryIds("check-r", "check-rc", "check-n", "check-nc")

/** The pair at Review (14a decision 3): it passes, opens the review with a reversal-date refusal, or stays on the entry. */
private sealed interface PairReview {
    val created: ReentryResult.Created?

    data class Passes(
        override val created: ReentryResult.Created,
    ) : PairReview

    data class OnReview(
        override val created: ReentryResult.Created,
        val refusal: ReentryRefusal,
    ) : PairReview

    /** [reason] is null when a field does not parse; the field refusals say why. */
    data class OnEntry(
        val reason: ReentryRefusal?,
    ) : PairReview {
        override val created: ReentryResult.Created? get() = null
    }
}

// Leaves Reverse and re-enter: the pair's state is cleared and the entry text it filled in is not kept (14a).
private fun AppState.leftReplace(): AppState = if (replacingId == null) {
    this
} else {
    copy(
        replacingId = null,
        replaceReversalDate = "",
        replaceDateEditing = false,
        replaceRefusals = ReplaceRefusals(),
        replaceReview = null,
        replaceStartingPicks = emptyMap(),
        buyForm = BuyForm(),
        sellForm = SellForm(),
    )
}

/** Why a transaction's reversal was refused, reasons only (D9): the date field, K1's rule, or the save. */
data class TransactionReversalRefusals(
    val date: DateRefusal? = null,
    val rule: ReversalRefusal? = null,
    val save: SaveRefusal? = null,
)

/**
 * Why a Reverse and re-enter pair was refused, reasons only (D9): the reversal date field on the combined review, a
 * rule of the reversal part (the new entry's own rules use the entry screen's refusals), or the save.
 */
data class ReplaceRefusals(
    val date: DateRefusal? = null,
    val rule: ReentryRefusal? = null,
    val save: SaveRefusal? = null,
)

/** Holding detail's tabs (view 6). Classification joins in 23 (10 decision 1). Never saved. */
enum class HoldingTab {
    TRANSACTIONS,
    LOTS,
}

/**
 * A line the Buy/Sell menu shows instead of an entry screen (wireframe 10a frame G; 09 decision 2). Reasons only;
 * the wording is D9 and lives in the screens.
 */
enum class BuyNotice {
    NEEDS_OPENING_BALANCE,
    NEEDS_PORTFOLIO,
    NOTHING_TO_SELL,

    // Sell in the header of a holding at 0 shares (11 decision 2), shown on Holding detail.
    NOTHING_TO_SELL_IN_HOLDING,
}

/**
 * The sell entry as typed (wireframe 10a frame C): the portfolio and ticker chosen with chips (11 decision 1), the
 * typed text, and the picks as typed, by lot (the lot's buy id), in the order they were made.
 */
data class SellForm(
    val portfolioId: String? = null,
    val ticker: String? = null,
    val dateText: String = "",
    val quantityText: String = "",
    val totalText: String = "",
    val picks: Map<String, String> = emptyMap(),
)

/**
 * Why a sell was refused, reasons only (D9). The field rules show together, with a refusal per pick; [rule] (K1)
 * runs with stand-ins whenever the date and total pass, so a field error does not hide a rule (CLAUDE.md pitfall).
 */
data class SellRefusals(
    val tickerMissing: Boolean = false,
    val date: DateRefusal? = null,
    val quantity: QuantityError? = null,
    val total: AmountError? = null,
    val picks: Map<String, QuantityError> = emptyMap(),
    val rule: SellRefusal? = null,
    val save: SaveRefusal? = null,
)

/**
 * The split entry as typed (wireframe 10b frame E): the portfolio and a ticker with a buy there, chosen with chips
 * (12 decision 1), the date, and the ratio "to for from" as typed.
 */
data class SplitForm(
    val portfolioId: String? = null,
    val ticker: String? = null,
    val dateText: String = "",
    val toText: String = "",
    val fromText: String = "",
)

/** Why a ratio number was refused as typed (4.5, 12 decision 3). Reasons only; the wording is D9. */
enum class RatioError {
    EMPTY,

    // Anything but the digits 0 to 9: a sign, a dot, a space.
    NOT_WHOLE,
    ZERO,

    // Above Splits.MAX_RATIO.
    TOO_LARGE,
}

/**
 * Why a split was refused, reasons only (D9). The field rules show together; [rule] (K1) runs with stand-ins whenever
 * the date passes, so a bad number does not hide the date rule (CLAUDE.md pitfall).
 */
data class SplitRefusals(
    val tickerMissing: Boolean = false,
    val date: DateRefusal? = null,
    val to: RatioError? = null,
    val from: RatioError? = null,
    val rule: SplitRefusal? = null,
    val save: SaveRefusal? = null,
)

/** A ratio number as typed: digits only, a whole number from 1 to [Splits.MAX_RATIO]; leading zeros are allowed. */
internal fun ratioError(text: String): RatioError? {
    if (text.isEmpty()) return RatioError.EMPTY
    if (!text.all { it in '0'..'9' }) return RatioError.NOT_WHOLE
    val digits = text.trimStart('0')
    return when {
        digits.isEmpty() -> RatioError.ZERO
        digits.length > Splits.MAX_RATIO.toString().length || digits.toInt() > Splits.MAX_RATIO -> RatioError.TOO_LARGE
        else -> null
    }
}

/** The number [ratioError] accepts, or null. */
internal fun ratioNumber(text: String): Int? = if (ratioError(text) == null) text.trimStart('0').toInt() else null

/** The buy entry as typed (wireframe 10a frame A). The ticker is kept in capitals; the rest as typed. */
data class BuyForm(
    val portfolioId: String? = null,
    val tickerText: String = "",
    val dateText: String = "",
    val quantityText: String = "",
    val totalText: String = "",
)

/**
 * Why a buy was refused, reasons only (D9). The field rules show together; [rule] (K1) only when the fields pass;
 * [check] (K4) only when the rules pass, so a typo never costs a look-up (iteration 09 step 7).
 */
data class BuyRefusals(
    val tickerEmpty: Boolean = false,
    val date: DateRefusal? = null,
    val quantity: QuantityError? = null,
    val total: AmountError? = null,
    val rule: BuyRefusal? = null,
    val check: CheckRefusal? = null,
    val save: SaveRefusal? = null,
)

/** A sell's total (4.4): as a buy's, except that zero is allowed. */
internal fun sellTotal(text: String): AmountResult = when (val parsed = Money.parseCents(text)) {
    is AmountResult.Refused -> if (parsed.reason == AmountError.ZERO) AmountResult.Cents(0L) else parsed
    is AmountResult.Cents -> parsed
}

/** The four bottom bar tabs (spec 6, navigation outline). */
enum class Tab {
    MORE,
    CASH,
    PORTFOLIOS,
    OVERALL,
}

/** The default order, left to right. Iteration 22 makes the order a setting. */
val DEFAULT_TAB_ORDER = listOf(Tab.MORE, Tab.CASH, Tab.PORTFOLIOS, Tab.OVERALL)

enum class DateRefusal {
    EMPTY,
    NOT_A_DATE,

    // A character other than a digit in a digits-only date field (the Cash filters, iteration 06).
    NOT_DIGITS,
}

enum class SaveRefusal {
    ALREADY_EXISTS,
    SAVE_FAILED,
}

/**
 * The cash entry form: the typed text, kept as typed and never trimmed or changed, the type and the direction.
 * The type is the opening balance until it exists, then one of the four entry types (03, 04). The direction is
 * used for an adjustment only and is kept across a type switch (04 decisions 2 and 4).
 */
data class FormState(
    val dateText: String = "",
    val amountText: String = "",
    val noteText: String = "",
    val type: CashType = CashType.OPENING_BALANCE,
    val direction: CashDirection = CashDirection.IN,
)

/** The reversal screen's typed text (spec 4.6): the date and the pre-filled, editable note (05 decision 1). */
data class ReversalForm(
    val dateText: String = "",
    val noteText: String = "",
)

/**
 * The Cash list filters as typed (view 7): From and To text and one type, null for All. Held in memory only, so
 * they last while the app runs and are never stored (06 decision 4).
 */
data class CashFilterForm(
    val fromText: String = "",
    val toText: String = "",
    val type: CashType? = null,
)

/**
 * The New portfolio dialog (wireframe 10d frame A): the typed name, kept as typed except that a line break becomes
 * a space (07 decision 4), and the reason it was refused, if it was. Reasons only, no wording (D9).
 */
data class PortfolioDialog(
    val nameText: String = "",
    val refusal: PortfolioRefusal? = null,
    val save: SaveRefusal? = null,
)

/** Reasons only, no wording (D9). A null or false field is fine. [cash] holds K1's date and balance rules. */
data class Refusals(
    val amount: AmountError? = null,
    val date: DateRefusal? = null,
    val noteHasLineBreak: Boolean = false,
    val save: SaveRefusal? = null,
    val cash: CashEntryRefusal? = null,
    val noteRequired: Boolean = false,
)

data class AppState(
    val screen: Screen = Screen.LOADING,
    // The selected tab. While the form or review is open, it is the tab the form was opened from.
    val tab: Tab = Tab.OVERALL,
    val entries: List<CashEntry> = emptyList(),
    val form: FormState = FormState(),
    val refusals: Refusals = Refusals(),
    val saving: Boolean = false,
    // Cash as of today plus every holding at cost, as of today (4.8; no prices before iteration 16).
    val netWorth: Fraction = Fraction.ZERO,
    val transactions: List<Transaction> = emptyList(),
    val instruments: List<Instrument> = emptyList(),
    // As of today (4.8), by portfolio, then ticker.
    val holdings: List<Holding> = emptyList(),
    val buySellMenuOpen: Boolean = false,
    val buyNotice: BuyNotice? = null,
    val buyForm: BuyForm = BuyForm(),
    val buyRefusals: BuyRefusals = BuyRefusals(),
    // The ticker check (K4) is running after Review.
    val checking: Boolean = false,
    // Create a portfolio was tapped from the Buy notice: after the portfolio is created, Buy opens (criterion 24).
    val buyAfterPortfolio: Boolean = false,
    // The holding on HOLDING: this ticker in the portfolio of portfolioId (iteration 10), and its tab.
    val holdingTicker: String? = null,
    val holdingTab: HoldingTab = HoldingTab.TRANSACTIONS,
    // Buy was opened from the holding's header: Back and a save return to it (10 decision 2).
    val buyFromHolding: Boolean = false,
    val sellForm: SellForm = SellForm(),
    val sellRefusals: SellRefusals = SellRefusals(),
    // A date change dropped picks that no longer pass the lot rules (wireframe 10a marker 11); cleared by the next pick.
    val sellPicksDropped: Boolean = false,
    // Sell was opened from the holding's header: Back and a save return to it (11 decision 2).
    val sellFromHolding: Boolean = false,
    // The sell shown on SELL_REVIEW, as K1 made it at Review.
    val sellReview: SellResult.Created? = null,
    // The split entry; Back and a save return to the holding it was opened from (12 decision 1).
    val splitForm: SplitForm = SplitForm(),
    val splitRefusals: SplitRefusals = SplitRefusals(),
    // The split shown on SPLIT_REVIEW, as K1 made it at Review.
    val splitReview: SplitResult.Created? = null,
    // Today in New York at the last load or save; holdings and lots are as of this date (4.8).
    val today: LocalDate = LocalDate.MIN,
    // The entry shown on DETAIL; on REVERSAL, the entry being reversed.
    val detailId: String? = null,
    // DETAIL shows a trade line opened from its transaction's cash link: Back returns to the transaction (13a decision 3).
    val detailFromTransaction: Boolean = false,
    // The transaction shown on TRANSACTION, and the screen its Back returns to: HOLDING or TABS (13a decision 3).
    val transactionId: String? = null,
    val transactionFrom: Screen = Screen.TABS,
    // The transactions left through a reversal link, oldest first: Back returns through them, one step at a time.
    val transactionTrail: List<String> = emptyList(),
    // REVERSE_TRANSACTION: the reversal date as digits, and why the reversal was refused (13b).
    val transactionReversalDate: String = "",
    val transactionReversalRefusals: TransactionReversalRefusals = TransactionReversalRefusals(),
    // Reverse and re-enter (14a): the original being replaced, while BUY, SELL or REPLACE_REVIEW shows the pair; the
    // reversal date as digits, the review's inline date field (decision 4), the refusals and the pair K1 made at Review.
    val replacingId: String? = null,
    val replaceReversalDate: String = "",
    val replaceDateEditing: Boolean = false,
    val replaceRefusals: ReplaceRefusals = ReplaceRefusals(),
    val replaceReview: ReentryResult.Created? = null,
    // A replaced sell's first picks as typed, for "(the original's picks)" while they are unchanged (10c marker 9).
    val replaceStartingPicks: Map<String, String> = emptyMap(),
    val reversal: ReversalForm = ReversalForm(),
    val cashFilterForm: CashFilterForm = CashFilterForm(),
    // In the order they were created (spec 3).
    val portfolios: List<Portfolio> = emptyList(),
    // The New portfolio dialog over the selected tab; null while it is closed.
    val portfolioDialog: PortfolioDialog? = null,
    // New portfolio was tapped before the opening balance exists (4.2): the screen asks for it instead.
    val portfolioNeedsOpeningBalance: Boolean = false,
    // The portfolio shown on PORTFOLIO, its ⋮ menu, and the Rename dialog over it (08 decision 1).
    val portfolioId: String? = null,
    val portfolioMenuOpen: Boolean = false,
    val renameDialog: PortfolioDialog? = null,
) {
    val shownPortfolio: Portfolio? get() = portfolioId?.let { id -> portfolios.firstOrNull { it.id == id } }

    /** Net worth rounded to cents, for Overall (4.10: rounded only when shown). */
    val netWorthCents: Long get() = netWorth.rounded(2).movePointRight(2).longValueExact()

    fun holdingsOf(portfolioId: String): List<Holding> = holdings.filter { it.portfolioId == portfolioId }

    /** The holding shown on HOLDING, as of today. */
    val shownHolding: Holding?
        get() = holdings.firstOrNull { it.portfolioId == portfolioId && it.ticker == holdingTicker }

    /** Its transactions, newest first, those dated after today included (view 6). */
    val holdingTransactions: List<Transaction>
        get() {
            val id = portfolioId ?: return emptyList()
            val ticker = holdingTicker ?: return emptyList()
            return Holdings.transactionsOf(transactions, id, ticker)
        }

    /** Its lots with shares as of today, oldest first (view 6, 4.4). */
    val holdingLots: List<Lot>
        get() {
            val id = portfolioId ?: return emptyList()
            val ticker = holdingTicker ?: return emptyList()
            return Holdings.lotsOf(transactions, id, ticker, today)
        }

    /** Each holding at cost while there are no prices (4.8). */
    fun portfolioValue(portfolioId: String): Fraction = holdingsOf(portfolioId).fold(Fraction.ZERO) { sum, holding -> sum + holding.cost }

    /** The derived price per share (4.3), shown as the fields change; null until quantity and total are both valid. */
    val buyPricePerShare: Fraction?
        get() {
            val quantity = Quantity.parse(buyForm.quantityText) as? QuantityResult.Exact ?: return null
            val total = Money.parseCents(buyForm.totalText) as? AmountResult.Cents ?: return null
            return Transactions.pricePerShare(total.value, quantity.value)
        }

    /** The tickers [portfolioId] holds with shares as of today, in ticker order. */
    fun heldTickers(portfolioId: String): List<String> = holdingsOf(portfolioId).filter { it.shares.signum() > 0 }.map { it.ticker }

    /**
     * The tickers the sell's portfolio holds, with shares as of today, in ticker order (11 decision 1). While a sell is
     * replaced, with its reversal applied, so a sold-out holding is offered again (14a).
     */
    val sellTickers: List<String>
        get() {
            val id = sellForm.portfolioId ?: return emptyList()
            if (replacingId == null) return heldTickers(id)
            return Holdings.holdings(sellTransactions, today).filter { it.portfolioId == id && it.shares.signum() > 0 }.map { it.ticker }
        }

    /**
     * The sell's lot list (4.4) for its date, or for today while the date is not a whole date. While a sell is
     * replaced, the lots are as they will be after the reversal (10c marker 9).
     */
    val sellLotChoices: List<LotChoice>
        get() {
            val id = sellForm.portfolioId ?: return emptyList()
            val ticker = sellForm.ticker ?: return emptyList()
            val date = DateDigits.toDate(sellForm.dateText) ?: today
            return Sells.lotChoices(sellTransactions, id, ticker, date, Transactions.nextSeq(entries, sellTransactions))
        }

    // The transactions a sell is entered against: with the replaced sell's reversal drafted at its date (14a).
    private val sellTransactions: List<Transaction>
        get() {
            val original = replacedTransaction ?: return transactions
            val date = DateDigits.toDate(replaceReversalDate) ?: original.date
            return transactions + Reversals.draft(entries, transactions, original, PREVIEW_ID, date)
        }

    /** The original Reverse and re-enter replaces (14a), for the banner and the "Original: …" hints (10c marker 2). */
    val replacedTransaction: Transaction? get() = replacingId?.let { id -> transactions.firstOrNull { it.id == id } }

    /** Reverse and re-enter is offered on a buy or sell not yet reversed (4.6; a split and a cash entry in 14b). */
    val transactionReenterOffered: Boolean get() = shownTransaction?.let { Reentries.offered(transactions, it) } ?: false

    /** What the reversal part does at the typed reversal date (10c marker 4), from a draft; null while it is not a whole date. */
    val replaceReversalEffect: ReversalEffect?
        get() {
            val original = replacedTransaction ?: return null
            val date = DateDigits.toDate(replaceReversalDate) ?: return null
            val draft = Reversals.draft(entries, transactions, original, PREVIEW_ID, date)
            return Reversals.effect(transactions + draft, draft)
        }

    /** The sum of both parts' cash effects (10c marker 7). */
    val replaceCombinedCents: Long?
        get() = replaceReview?.let { it.reversalCashLine.amountCents + it.replacementCashLine.amountCents }

    /** The pieces of the new sell on the combined review, oldest lot first (10c marker 10). */
    val replacePieces: List<SoldPiece>
        get() = replaceReview?.takeIf { it.replacement.type == TransactionType.SELL }?.let {
            Sells.pieces(transactions + it.reversal + it.replacement, it.replacement)
        }.orEmpty()

    /** The sum of the picks that are valid quantities, for "Picked X of Y shares" (wireframe 10a marker 11). */
    val sellPicked: BigDecimal
        get() = sellForm.picks.values
            .mapNotNull { (Quantity.parse(it) as? QuantityResult.Exact)?.value }
            .fold(BigDecimal.ZERO) { sum, quantity -> sum + quantity }

    /** Total over quantity (4.4); null until both are valid. A total of 0 gives $0. */
    val sellPricePerShare: Fraction?
        get() {
            val quantity = Quantity.parse(sellForm.quantityText) as? QuantityResult.Exact ?: return null
            val total = sellTotal(sellForm.totalText) as? AmountResult.Cents ?: return null
            return Transactions.pricePerShare(total.value, quantity.value)
        }

    /** The pieces of the reviewed sell, oldest lot first (wireframe 10a frame E). */
    val sellPieces: List<SoldPiece>
        get() = sellReview?.let { Sells.pieces(transactions + it.transaction, it.transaction) }.orEmpty()

    /** The tickers with a buy in the split's portfolio, in ticker order (section 6, 12 decision 1). */
    val splitTickers: List<String> get() = splitForm.portfolioId?.let { Splits.tickers(transactions, it) }.orEmpty()

    /** The shares at the reviewed split's place, before and after (wireframe 10b frame F). */
    val splitEffect: SplitEffect?
        get() = splitReview?.let { Splits.effect(transactions + it.transaction, it.transaction) }

    /**
     * The fields take digits only (YYYYMMDD): any other character is refused at once, eight digits that are no
     * real date get the date message, fewer digits get nothing (06 decision 1 as changed after phone check 12).
     */
    val filterFromRefusal: DateRefusal? get() = DateDigits.refusal(cashFilterForm.fromText)

    val filterToRefusal: DateRefusal? get() = DateDigits.refusal(cashFilterForm.toText)

    /** Only eight digits of a real date count as a bound; anything else is no bound (06 decision 1). */
    val cashFilter: CashFilter
        get() = CashFilter(
            from = DateDigits.toDate(cashFilterForm.fromText),
            to = DateDigits.toDate(cashFilterForm.toText),
            type = cashFilterForm.type,
        )

    /** A valid date bound or a chip other than All (06 decision 3: the count line and Clear filters show then). */
    val filterActive: Boolean get() = cashFilter.isActive

    /** The rows that match the filters, each with its real balance (view 7). */
    val cashFilteredRows: List<CashRow> get() = CashAccount.rows(entries, cashFilter)

    val holdsData: Boolean get() = CashRules.appHoldsData(entries)

    val detailEntry: CashEntry? get() = entries.firstOrNull { it.id == detailId }

    /** The reversal of the shown entry, if it has one (spec 4.6: at most one). */
    val detailReversedBy: CashEntry? get() = detailId?.let { id -> entries.firstOrNull { it.reversesId == id } }

    /** The original the shown entry reverses, if it is a reversal. */
    val detailReverses: CashEntry? get() = detailEntry?.reversesId?.let { id -> entries.firstOrNull { it.id == id } }

    /** The buy or sell of the shown trade line (wireframe 10b frame H: its detail links to it). */
    val detailTradeTransaction: Transaction?
        get() = detailEntry?.transactionId?.let { id -> transactions.firstOrNull { it.id == id } }

    val shownTransaction: Transaction? get() = transactionId?.let { id -> transactions.firstOrNull { it.id == id } }

    /** The shown transaction's trade line; none for a split (4.5). */
    val transactionCashLine: CashEntry?
        get() = transactionId?.let { id -> entries.firstOrNull { it.type == CashType.TRADE && it.transactionId == id } }

    /** What is left of the shown buy's lot as of today, split-adjusted; null for a buy dated after today (13a decision 2). */
    val transactionLot: Lot?
        get() = shownTransaction?.takeIf { it.type == TransactionType.BUY }?.let { buy ->
            Holdings.lots(transactions, today).firstOrNull { it.buyId == buy.id }
        }

    /** The lots the shown sell used, oldest first, priced on its date (13a decision 1). */
    val transactionPieces: List<SoldPiece>
        get() = shownTransaction?.takeIf { it.type == TransactionType.SELL }?.let { Sells.pieces(transactions, it) }.orEmpty()

    /** The shares at the shown split's place, before and after. */
    val transactionSplitEffect: SplitEffect?
        get() = shownTransaction?.takeIf { it.type == TransactionType.SPLIT }?.let { Splits.effect(transactions, it) }

    /** Reverse is offered on a buy, sell or split not yet reversed; never on a reversal (4.6). */
    val transactionReverseOffered: Boolean get() = shownTransaction?.let { Reversals.offered(transactions, it) } ?: false

    /** The shown transaction's reversal, for "Reversed on" (frame G marker 14). */
    val transactionReversal: Transaction? get() = transactionId?.let { Reversals.reversalOf(transactions, it) }

    /** The original of the shown reversal, for "Reversal of the … of …" (frame G marker 14). */
    val transactionOriginal: Transaction? get() = shownTransaction?.let { Reversals.originalOf(transactions, it) }

    /** What the shown reversal did, worked out from its original (frame G; 13b wording 5). */
    val transactionShownEffect: ReversalEffect?
        get() = shownTransaction?.takeIf { it.type == TransactionType.REVERSAL }?.let { Reversals.effect(transactions, it) }

    /**
     * What the reversal on REVERSE_TRANSACTION would do at the typed date (frame I marker 26), from an unsaved draft;
     * null while the date is not a whole date.
     */
    val transactionReversalEffect: ReversalEffect?
        get() {
            val original = shownTransaction ?: return null
            val date = DateDigits.toDate(transactionReversalDate) ?: return null
            val draft = Reversals.draft(entries, transactions, original, PREVIEW_ID, date)
            return Reversals.effect(transactions + draft, draft)
        }

    /** Reverse is offered on a direct entry that is not a reversal and not yet reversed (spec 4.6). */
    val reverseOffered: Boolean
        get() {
            val entry = detailEntry ?: return false
            val direct = entry.type != CashType.OPENING_BALANCE && entry.type != CashType.TRADE
            return direct && entry.reversesId == null && detailReversedBy == null
        }

    /** The Cash tab balance: on the latest entry date, so future-dated entries count (4.8). */
    val cashBalanceCents: Long get() = CashAccount.balanceCents(entries)

    /** The Cash tab rows, newest first, each with the balance after it (view 7). */
    val cashRows: List<CashRow> get() = CashAccount.rows(entries)

    /** The type the Cash entry button opens: the opening balance until it exists, then a deposit (iteration 03). */
    val cashEntryType: CashType
        get() = if (entries.none { it.type == CashType.OPENING_BALANCE }) CashType.OPENING_BALANCE else CashType.DEPOSIT

    /** Android's back button on More, Cash or Portfolios goes to Overall (02b decision 3). */
    val backGoesToOverall: Boolean get() = screen == Screen.TABS && tab != Tab.OVERALL
}

/**
 * The state and actions of the app: the tabs and the cash entry flow (opening balance from 02a, deposit and
 * withdrawal from 03). It holds the typed form text, so a recreation of the screen keeps it. The store is
 * created on the first [start], after unlock. Refusals are reasons only; their wording is D9 and lives in
 * the screens.
 */
class AppViewModel(
    private val storeProvider: () -> RecordStore,
    clock: Clock,
    private val newId: () -> String,
    private val priceSource: PriceSource,
    private val isOnline: () -> Boolean,
) : ViewModel() {
    private val marketDate = MarketDate(clock)
    private val mutableState = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = mutableState.asStateFlow()
    private var store: RecordStore? = null
    private var started = false

    /** Creates the store and loads the entries, once. Later calls do nothing. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val opened = storeProvider()
                store = opened
                showLoaded(opened.loadCashEntries(), opened.loadPortfolios(), opened.loadTransactions(), opened.loadInstruments())
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE) }
            }
        }
    }

    /**
     * Shows [tab]. From the form or review it leaves them unsaved; the typed text is kept (02b decision 1).
     * Ignored while loading, on the unreadable screen and while a save is running.
     */
    fun selectTab(tab: Tab) {
        val current = mutableState.value
        if (current.screen !in TAB_BAR_SCREENS || current.saving || current.checking) return
        mutableState.update {
            it.copy(
                screen = Screen.TABS,
                tab = tab,
                refusals = Refusals(),
                portfolioNeedsOpeningBalance = false,
                portfolioId = null,
                portfolioMenuOpen = false,
                renameDialog = null,
                buySellMenuOpen = false,
                buyNotice = null,
                buyRefusals = BuyRefusals(),
                holdingTicker = null,
                buyFromHolding = false,
                sellRefusals = SellRefusals(),
                sellPicksDropped = false,
                sellFromHolding = false,
                sellReview = null,
                splitRefusals = SplitRefusals(),
                splitReview = null,
                detailFromTransaction = false,
                transactionId = null,
                transactionTrail = emptyList(),
                transactionReversalDate = "",
                transactionReversalRefusals = TransactionReversalRefusals(),
            ).leftReplace()
        }
    }

    /**
     * After unlock the app shows Overall, unless an entry screen or the New portfolio dialog is open, which stays
     * (02b decision 9). The Buy/Sell menu and its notice close.
     */
    fun onUnlocked() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.portfolioDialog != null) return
        mutableState.update { it.copy(tab = Tab.OVERALL, buySellMenuOpen = false, buyNotice = null) }
    }

    /**
     * Opens the form. The date is filled with today in New York only if it is empty. The type is the opening
     * balance until it exists; after that a kept Deposit or Withdrawal stays, anything else becomes Deposit.
     */
    fun openForm() = mutableState.update { current ->
        val form = current.form
        val dated = if (form.dateText.isEmpty()) form.copy(dateText = marketDate.today().toString()) else form
        val opening = current.cashEntryType == CashType.OPENING_BALANCE
        val type =
            when {
                opening -> CashType.OPENING_BALANCE
                form.type in ENTRY_TYPES -> form.type
                else -> CashType.DEPOSIT
            }
        current.copy(screen = Screen.FORM, form = dated.copy(type = type), refusals = Refusals(), buyNotice = null)
    }

    /** Deposit or Withdrawal only, and only once the opening balance exists (03 decision 2). Other types are ignored. */
    fun onTypeChange(type: CashType) {
        val current = mutableState.value
        if (type !in ENTRY_TYPES || current.form.type !in ENTRY_TYPES) return
        mutableState.update {
            it.copy(form = it.form.copy(type = type), refusals = it.refusals.copy(cash = null, noteRequired = false))
        }
    }

    /** The adjustment's direction (04 decision 2: In by default). Kept while other types are shown. */
    fun onDirectionChange(direction: CashDirection) {
        if (mutableState.value.form.type !in ENTRY_TYPES) return
        mutableState.update { it.copy(form = it.form.copy(direction = direction), refusals = it.refusals.copy(cash = null)) }
    }

    /** Back from the form to the tab it was opened from. The typed text is kept and the refusals are cleared. Ignored outside FORM. */
    fun closeForm() {
        if (mutableState.value.screen != Screen.FORM) return
        mutableState.update { it.copy(screen = Screen.TABS, refusals = Refusals()) }
    }

    fun onDateChange(text: String) = mutableState.update {
        it.copy(form = it.form.copy(dateText = text), refusals = it.refusals.copy(date = null, cash = null))
    }

    fun onAmountChange(text: String) = mutableState.update {
        it.copy(form = it.form.copy(amountText = text), refusals = it.refusals.copy(amount = null, cash = null))
    }

    fun onNoteChange(text: String) = mutableState.update {
        it.copy(
            form = it.form.copy(noteText = text),
            refusals = it.refusals.copy(noteHasLineBreak = false, noteRequired = false),
        )
    }

    /** Checks every field and reports all refusals together. Goes to REVIEW only if all are fine. Saves nothing. */
    fun review() {
        val current = mutableState.value
        if (current.screen != Screen.FORM) return
        val form = current.form
        val amount = Money.parseCents(form.amountText)
        val amountError = (amount as? AmountResult.Refused)?.reason
        val dateText = form.dateText.trim()
        val date = MarketDate.parse(dateText)
        val dateRefusal =
            when {
                dateText.isEmpty() -> DateRefusal.EMPTY
                date == null -> DateRefusal.NOT_A_DATE
                else -> null
            }
        val cents = (amount as? AmountResult.Cents)?.value
        val ruleRefusals =
            if (form.type == CashType.OPENING_BALANCE) {
                openingBalanceRefusals(current, date, cents)
            } else {
                entryRefusals(current, date, cents)
            }
        val refusals = ruleRefusals.copy(amount = amountError, date = dateRefusal)
        val allFine = refusals == Refusals()
        mutableState.update {
            it.copy(screen = if (allFine) Screen.REVIEW else Screen.FORM, refusals = refusals)
        }
    }

    // A stand-in amount and date let K1 judge the note and "already exists" rules in the same pass.
    private fun openingBalanceRefusals(
        current: AppState,
        date: LocalDate?,
        cents: Long?,
    ): Refusals {
        val probe =
            OpeningBalance.create(
                existing = current.entries,
                id = PROBE_ID,
                date = date ?: marketDate.today(),
                amountCents = cents ?: 1L,
                note = current.form.noteText,
            )
        val rule = (probe as? OpeningBalanceResult.Refused)?.reason
        return Refusals(
            noteHasLineBreak = rule == OpeningBalanceError.NOTE_HAS_LINE_BREAK,
            save = if (rule == OpeningBalanceError.ALREADY_EXISTS) SaveRefusal.ALREADY_EXISTS else null,
        )
    }

    /**
     * Two passes of K1 so every refusal shows together: the note with a stand-in date and amount, then the
     * date and balance rules with an empty note, only when the date and amount are valid.
     */
    private fun entryRefusals(
        current: AppState,
        date: LocalDate?,
        cents: Long?,
    ): Refusals {
        val form = current.form
        val noteProbe =
            CashEntries.create(current.entries, PROBE_ID, form.type, marketDate.today(), 1L, form.noteText, form.direction)
        val noteRule = (noteProbe as? CashEntryResult.Refused)?.reason
        // A stand-in note, so the note rules (judged above) cannot hide the date and balance rules here.
        val cashRule =
            if (date != null && cents != null) {
                val probe = CashEntries.create(current.entries, PROBE_ID, form.type, date, cents, PROBE_NOTE, form.direction)
                (probe as? CashEntryResult.Refused)?.reason
            } else {
                null
            }
        return Refusals(
            noteHasLineBreak = noteRule == CashEntryRefusal.NoteHasLineBreak,
            noteRequired = noteRule == CashEntryRefusal.NoteRequired,
            cash = cashRule,
        )
    }

    /** A ninth digit is not accepted; other characters are kept, so their error shows at once. */
    fun onFilterFromChange(text: String) {
        if (tooManyDigits(text)) return
        mutableState.update { it.copy(cashFilterForm = it.cashFilterForm.copy(fromText = text)) }
    }

    fun onFilterToChange(text: String) {
        if (tooManyDigits(text)) return
        mutableState.update { it.copy(cashFilterForm = it.cashFilterForm.copy(toText = text)) }
    }

    private fun tooManyDigits(text: String): Boolean = DateDigits.isDigits(text) && text.length > DateDigits.LENGTH

    /** A digits-only entry date (Buy and Sell, 09 decision 10): empty, not digits, or not a real date. */
    private fun digitsDateRefusal(text: String): DateRefusal? = when {
        text.isEmpty() -> DateRefusal.EMPTY
        !DateDigits.isDigits(text) -> DateRefusal.NOT_DIGITS
        DateDigits.toDate(text) == null -> DateRefusal.NOT_A_DATE
        else -> null
    }

    /** One chip at a time: null for All, or one of the four direct types. Other types are ignored. */
    fun onFilterTypeChange(type: CashType?) {
        if (type != null && type !in ENTRY_TYPES) return
        mutableState.update { it.copy(cashFilterForm = it.cashFilterForm.copy(type = type)) }
    }

    /** Clear filters: both dates empty and All (view 7). */
    fun clearFilters() = mutableState.update { it.copy(cashFilterForm = CashFilterForm()) }

    /**
     * New portfolio or Create a portfolio: opens an empty dialog over the selected tab, or, before the opening
     * balance exists, asks for it instead (4.2, wireframe 10d). Ignored outside the tabs and while a save is running.
     */
    fun openPortfolioDialog() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.saving) return
        if (current.cashEntryType == CashType.OPENING_BALANCE) {
            mutableState.update { it.copy(portfolioNeedsOpeningBalance = true) }
            return
        }
        mutableState.update { it.copy(portfolioDialog = PortfolioDialog(), portfolioNeedsOpeningBalance = false) }
    }

    /** A line break, typed or pasted, becomes a space (07 decision 4). Typing clears the refusal. */
    fun onPortfolioNameChange(text: String) = mutableState.update { state ->
        state.copy(portfolioDialog = state.portfolioDialog?.let { PortfolioDialog(nameText = oneLine(text)) })
    }

    /** Closes the dialog; nothing is saved, and Buy does not open afterwards. Ignored while a save is running. */
    fun cancelPortfolioDialog() {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(portfolioDialog = null, buyAfterPortfolio = false) }
    }

    /**
     * Create (wireframe 10d): checks the name (4.7) and saves the portfolio. On a refusal the dialog stays with the
     * typed text and the reason. On success it closes and the new portfolio is last; the tab stays as it was, or,
     * when the dialog came from the Buy notice, Buy opens on the new portfolio (criterion 24).
     */
    fun createPortfolio() {
        val current = mutableState.value
        val dialog = current.portfolioDialog ?: return
        if (current.saving) return
        val target = store ?: return
        val result = Portfolios.create(current.portfolios, current.entries, newId(), dialog.nameText)
        val portfolio =
            when (result) {
                is PortfolioResult.Refused -> {
                    mutableState.update { it.copy(portfolioDialog = dialog.copy(refusal = result.reason, save = null)) }
                    return
                }

                is PortfolioResult.Created -> result.portfolio
            }
        mutableState.update { it.copy(saving = true, portfolioDialog = dialog.copy(refusal = null, save = null)) }
        viewModelScope.launch {
            try {
                target.savePortfolio(portfolio)
                mutableState.update {
                    it.copy(portfolios = it.portfolios + portfolio, portfolioDialog = null, saving = false)
                }
                if (mutableState.value.buyAfterPortfolio) {
                    mutableState.update { it.copy(buyAfterPortfolio = false) }
                    openBuy(portfolio.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(saving = false, portfolioDialog = dialog.copy(save = SaveRefusal.SAVE_FAILED))
                }
            }
        }
    }

    /** Opens a portfolio's screen from its row (08 decision 3). Unknown ids are ignored. */
    fun openPortfolio(id: String) {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.portfolios.none { it.id == id }) return
        mutableState.update { it.copy(screen = Screen.PORTFOLIO, portfolioId = id, portfolioMenuOpen = false) }
    }

    /** "<" or Android Back: back to the list. Ignored while a save is running. */
    fun closePortfolio() {
        val current = mutableState.value
        if (current.screen != Screen.PORTFOLIO || current.saving) return
        mutableState.update {
            it.copy(screen = Screen.TABS, portfolioId = null, portfolioMenuOpen = false, renameDialog = null)
        }
    }

    fun openPortfolioMenu() {
        if (mutableState.value.screen != Screen.PORTFOLIO) return
        mutableState.update { it.copy(portfolioMenuOpen = true) }
    }

    fun closePortfolioMenu() = mutableState.update { it.copy(portfolioMenuOpen = false) }

    /** Rename in the menu: closes the menu and opens the dialog with the current name (wireframe 10d frame B). */
    fun openRename() {
        val current = mutableState.value
        if (current.screen != Screen.PORTFOLIO || current.saving) return
        val portfolio = current.shownPortfolio ?: return
        mutableState.update { it.copy(portfolioMenuOpen = false, renameDialog = PortfolioDialog(nameText = portfolio.name)) }
    }

    /** As for create: a line break becomes a space and typing clears the refusal. */
    fun onRenameNameChange(text: String) = mutableState.update { state ->
        state.copy(renameDialog = state.renameDialog?.let { PortfolioDialog(nameText = oneLine(text)) })
    }

    /** Closes the dialog; nothing is saved. Ignored while a save is running. */
    fun cancelRename() {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(renameDialog = null) }
    }

    /**
     * Save (wireframe 10d frame B): checks the name against the other portfolios (4.7) and changes only the name.
     * The unchanged name closes the dialog without a write. On a refusal the dialog stays with the typed text.
     */
    fun saveRename() {
        val current = mutableState.value
        val dialog = current.renameDialog ?: return
        val id = current.portfolioId ?: return
        if (current.saving) return
        val target = store ?: return
        val portfolio =
            when (val result = Portfolios.rename(current.portfolios, id, dialog.nameText)) {
                is PortfolioRenameResult.Renamed -> result.portfolio

                is PortfolioRenameResult.Refused -> {
                    mutableState.update { it.copy(renameDialog = dialog.copy(refusal = result.reason, save = null)) }
                    return
                }

                PortfolioRenameResult.Unchanged, PortfolioRenameResult.NotFound -> {
                    mutableState.update { it.copy(renameDialog = null) }
                    return
                }
            }
        mutableState.update { it.copy(saving = true, renameDialog = dialog.copy(refusal = null, save = null)) }
        viewModelScope.launch {
            try {
                target.renamePortfolio(portfolio.id, portfolio.name)
                mutableState.update { state ->
                    state.copy(
                        portfolios = state.portfolios.map { if (it.id == portfolio.id) portfolio else it },
                        renameDialog = null,
                        saving = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(saving = false, renameDialog = dialog.copy(save = SaveRefusal.SAVE_FAILED))
                }
            }
        }
    }

    // A line break, typed or pasted, becomes a space in a portfolio name (07 decision 4).
    private fun oneLine(text: String): String = text.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')

    /** Opens an entry's detail from the Cash list, or follows a link from another detail. Unknown ids are ignored. */
    fun openDetail(id: String) {
        val current = mutableState.value
        if (current.screen != Screen.TABS && current.screen != Screen.DETAIL) return
        if (current.entries.none { it.id == id }) return
        mutableState.update { it.copy(screen = Screen.DETAIL, detailId = id, refusals = Refusals()) }
    }

    /** Back from the detail to the Cash list, or to the transaction whose cash link opened it (13a decision 3). */
    fun closeDetail() {
        val current = mutableState.value
        if (current.screen != Screen.DETAIL) return
        val back = if (current.detailFromTransaction) Screen.TRANSACTION else Screen.TABS
        mutableState.update { it.copy(screen = back, detailId = null, detailFromTransaction = false) }
    }

    /**
     * Opens a buy, sell or split (wireframe 10b frame G): from a Transactions row on Holding detail, or from a trade
     * line on the Cash tab (criterion 29). Back returns there. Unknown ids are ignored.
     */
    fun openTransaction(id: String) {
        val current = mutableState.value
        if (current.screen != Screen.HOLDING && current.screen != Screen.TABS) return
        if (current.transactions.none { it.id == id }) return
        mutableState.update { it.copy(screen = Screen.TRANSACTION, transactionId = id, transactionFrom = current.screen) }
    }

    /**
     * "<" or Back on a transaction: back through the reversal links followed, then to where it was opened from (13a
     * decision 3).
     */
    fun closeTransaction() {
        if (mutableState.value.screen != Screen.TRANSACTION) return
        mutableState.update {
            val previous = it.transactionTrail.lastOrNull()
            if (previous != null) {
                it.copy(transactionId = previous, transactionTrail = it.transactionTrail.dropLast(1))
            } else {
                it.copy(screen = it.transactionFrom, transactionId = null)
            }
        }
    }

    /** "Reversed on …" or "Reversal of the … of …" (frame G marker 14): the other transaction of the pair; Back returns. */
    fun openTransactionLink() {
        val current = mutableState.value
        if (current.screen != Screen.TRANSACTION) return
        val shown = current.transactionId ?: return
        val other = current.transactionReversal ?: current.transactionOriginal ?: return
        mutableState.update { it.copy(transactionId = other.id, transactionTrail = it.transactionTrail + shown) }
    }

    /** Reverse on a buy, sell or split (4.6): the reversal screen, dated the original's date (frame I marker 17). */
    fun openTransactionReversal() {
        val current = mutableState.value
        if (current.screen != Screen.TRANSACTION || !current.transactionReverseOffered) return
        val original = current.shownTransaction ?: return
        mutableState.update {
            it.copy(
                screen = Screen.REVERSE_TRANSACTION,
                transactionReversalDate = original.date.toString().replace("-", ""),
                transactionReversalRefusals = TransactionReversalRefusals(),
            )
        }
    }

    /** Digits only, as the other entry dates (09 decision 10). */
    fun onTransactionReversalDateChange(text: String) {
        if (tooManyDigits(text)) return
        val refusal = if (DateDigits.isDigits(text)) null else DateRefusal.NOT_DIGITS
        mutableState.update {
            it.copy(transactionReversalDate = text, transactionReversalRefusals = TransactionReversalRefusals(date = refusal))
        }
    }

    /** Cancel or Back: the original's detail, nothing saved. */
    fun cancelTransactionReversal() {
        val current = mutableState.value
        if (current.screen != Screen.REVERSE_TRANSACTION || current.saving) return
        mutableState.update { it.copy(screen = Screen.TRANSACTION, transactionReversalRefusals = TransactionReversalRefusals()) }
    }

    /**
     * Confirm and save (frame I): the date field, then K1's reversal rules (4.1, 4.6); a refusal keeps the screen open
     * with the date (frame J). The reversal and its trade line are saved together; the app returns to the original's
     * detail, as 05's cash reversal does (13b decision 2).
     */
    fun confirmTransactionReversal() {
        val current = mutableState.value
        if (current.screen != Screen.REVERSE_TRANSACTION || current.saving) return
        val target = store ?: return
        val original = current.shownTransaction ?: return
        val dateRefusal = digitsDateRefusal(current.transactionReversalDate)
        val date = DateDigits.toDate(current.transactionReversalDate)
        if (dateRefusal != null || date == null) {
            mutableState.update { it.copy(transactionReversalRefusals = TransactionReversalRefusals(date = dateRefusal ?: DateRefusal.NOT_A_DATE)) }
            return
        }
        val created =
            when (val result = Reversals.reverse(current.entries, current.transactions, newId(), newId(), original.id, date)) {
                is ReversalResult.Created -> result

                is ReversalResult.Refused -> {
                    mutableState.update { it.copy(transactionReversalRefusals = TransactionReversalRefusals(rule = result.reason)) }
                    return
                }
            }
        mutableState.update { it.copy(saving = true, transactionReversalRefusals = TransactionReversalRefusals()) }
        viewModelScope.launch {
            try {
                target.saveReversal(created.transaction, created.cashLine)
                mutableState.update {
                    valued(
                        it.copy(
                            screen = Screen.TRANSACTION,
                            transactions = it.transactions + created.transaction,
                            entries = it.entries + listOfNotNull(created.cashLine),
                            transactionReversalDate = "",
                            saving = false,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(saving = false, transactionReversalRefusals = TransactionReversalRefusals(save = SaveRefusal.SAVE_FAILED))
                }
            }
        }
    }

    /**
     * Reverse and re-enter on a buy or sell (4.6, wireframe 10c marker 1): the normal entry screen with the original's
     * values, its date included; the reversal is dated the original's date (marker 5). The new entry goes after the
     * reversal, so its quantity and a sell's picks start with the splits since the original applied, and an unchanged
     * pair leaves the holding as it was (marker 9; 14a fix after phone check 2). A sell's quantity is its picks' sum.
     */
    fun openReenter() {
        val current = mutableState.value
        if (current.screen != Screen.TRANSACTION || !current.transactionReenterOffered) return
        val original = current.shownTransaction ?: return
        val date = original.date.toString().replace("-", "")
        val total = BigDecimal.valueOf(original.totalCents, 2).toPlainString()
        val draft = Reversals.draft(current.entries, current.transactions, original, PREVIEW_ID, original.date)
        val after = current.transactions + draft
        val replacing =
            current.copy(
                replacingId = original.id,
                replaceReversalDate = date,
                replaceDateEditing = false,
                replaceRefusals = ReplaceRefusals(),
                replaceReview = null,
            )
        mutableState.value =
            when (original.type) {
                TransactionType.BUY -> {
                    val quantity = Reentries.startingQuantity(after, original, original.date, draft.enteredSeq + 1)
                    replacing.copy(
                        screen = Screen.BUY,
                        buyForm = BuyForm(original.portfolioId, original.ticker, date, quantity.toPlainString(), total),
                        buyRefusals = BuyRefusals(),
                        buyFromHolding = false,
                    )
                }

                TransactionType.SELL -> {
                    val starting = Reentries.startingPicks(after, original, original.date, draft.enteredSeq + 1)
                    val picks = starting.associate { it.lotId to it.quantity.toPlainString() }
                    val quantity = starting.fold(BigDecimal.ZERO) { sum, pick -> sum + pick.quantity }
                    replacing.copy(
                        screen = Screen.SELL,
                        sellForm =
                        SellForm(
                            portfolioId = original.portfolioId,
                            ticker = original.ticker,
                            dateText = date,
                            quantityText = quantity.toPlainString(),
                            totalText = total,
                            picks = picks,
                        ),
                        replaceStartingPicks = picks,
                        sellRefusals = SellRefusals(),
                        sellPicksDropped = false,
                        sellFromHolding = false,
                        sellReview = null,
                    )
                }

                else -> return
            }
    }

    /** Change on the combined review: the reversal date becomes an inline field (14a decision 4). */
    fun changeReplaceDate() {
        val current = mutableState.value
        if (current.screen != Screen.REPLACE_REVIEW || current.saving) return
        mutableState.update { it.copy(replaceDateEditing = true) }
    }

    /**
     * Digits only, as the other dates (09 decision 10). Once the date is whole, K1 checks the pair at it, and any refusal
     * shows on the review, where only this date can change; part 1's effect follows the date (14a decision 4).
     */
    fun onReplaceDateChange(text: String) {
        val current = mutableState.value
        if (current.screen != Screen.REPLACE_REVIEW || current.saving || tooManyDigits(text)) return
        val refusal = if (DateDigits.isDigits(text)) null else DateRefusal.NOT_DIGITS
        val dated = current.copy(replaceReversalDate = text, replaceRefusals = ReplaceRefusals(date = refusal))
        val date = DateDigits.toDate(text)
        mutableState.value =
            when (val result = date?.let { pairOf(dated, PROBE_IDS, it) }) {
                is ReentryResult.Created -> dated.copy(replaceReview = result)
                is ReentryResult.Refused -> dated.copy(replaceRefusals = ReplaceRefusals(rule = result.reason))
                null -> dated
            }
    }

    /** Back to edit, or Back on the combined review: the entry screen, with the values and the reversal date kept. */
    fun backToReplaceEntry() {
        val current = mutableState.value
        if (current.screen != Screen.REPLACE_REVIEW || current.saving) return
        val entry = if (current.replacedTransaction?.type == TransactionType.SELL) Screen.SELL else Screen.BUY
        mutableState.update {
            it.copy(screen = entry, replaceRefusals = ReplaceRefusals(), replaceReview = null, replaceDateEditing = false)
        }
    }

    /**
     * Confirm and save both (10c marker 8): the date field, then K1's pair rules with real ids; a refusal stays on the
     * review. The pair is saved as one action, and the app returns to the original's detail (14a decision 2).
     */
    fun confirmReplace() {
        val current = mutableState.value
        if (current.screen != Screen.REPLACE_REVIEW || current.saving) return
        val target = store ?: return
        val original = current.replacedTransaction ?: return
        val dateRefusal = digitsDateRefusal(current.replaceReversalDate)
        val date = DateDigits.toDate(current.replaceReversalDate)
        if (dateRefusal != null || date == null) {
            // The field shows the reason, so it is open whenever the date is refused (14a Deploy finding 1).
            mutableState.update {
                it.copy(replaceRefusals = ReplaceRefusals(date = dateRefusal ?: DateRefusal.NOT_A_DATE), replaceDateEditing = true)
            }
            return
        }
        val created =
            when (val result = pairOf(current, ReentryIds(newId(), newId(), newId(), newId()), date)) {
                is ReentryResult.Created -> result

                is ReentryResult.Refused -> {
                    mutableState.update { it.copy(replaceRefusals = ReplaceRefusals(rule = result.reason)) }
                    return
                }

                null -> {
                    backToReplaceEntry()
                    return
                }
            }
        mutableState.update { it.copy(saving = true, replaceRefusals = ReplaceRefusals()) }
        viewModelScope.launch {
            try {
                target.savePair(created.reversal, created.reversalCashLine, created.replacement, created.replacementCashLine)
                mutableState.update {
                    valued(
                        it.copy(
                            screen = Screen.TRANSACTION,
                            transactionId = original.id,
                            transactions = it.transactions + created.reversal + created.replacement,
                            entries = it.entries + created.reversalCashLine + created.replacementCashLine,
                            saving = false,
                        ).leftReplace(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, replaceRefusals = ReplaceRefusals(save = SaveRefusal.SAVE_FAILED)) }
            }
        }
    }

    /**
     * K1's pair rules on the typed entry with the reversal dated [reversalDate]; null when the entry's date or total
     * does not parse (the field rules decide then). [standIns] as for a plain buy or sell.
     */
    private fun pairOf(
        current: AppState,
        ids: ReentryIds,
        reversalDate: LocalDate,
        standIns: Boolean = false,
    ): ReentryResult? {
        val original = current.replacedTransaction ?: return null
        return when (original.type) {
            TransactionType.BUY -> {
                val form = current.buyForm
                val date = DateDigits.toDate(form.dateText) ?: return null
                val total = Money.parseCents(form.totalText) as? AmountResult.Cents ?: return null
                val typed = (Quantity.parse(form.quantityText) as? QuantityResult.Exact)?.value
                Reentries.reenterBuy(
                    cashEntries = current.entries,
                    transactions = current.transactions,
                    portfolios = current.portfolios,
                    ids = ids,
                    originalId = original.id,
                    reversalDate = reversalDate,
                    portfolioId = form.portfolioId.orEmpty(),
                    ticker = if (standIns && form.tickerText.isBlank()) PROBE_ID else form.tickerText,
                    date = date,
                    quantity = typed ?: if (standIns) BigDecimal.ONE else return null,
                    totalCents = total.value,
                )
            }

            TransactionType.SELL -> {
                val form = current.sellForm
                val date = DateDigits.toDate(form.dateText) ?: return null
                val total = sellTotal(form.totalText) as? AmountResult.Cents ?: return null
                val picks = form.picks.mapNotNull { (lotId, text) -> (Quantity.parse(text) as? QuantityResult.Exact)?.let { Pick(lotId, it.value) } }
                if (!standIns && picks.size != form.picks.size) return null
                val picked = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + pick.quantity }
                val typed = (Quantity.parse(form.quantityText) as? QuantityResult.Exact)?.value
                Reentries.reenterSell(
                    cashEntries = current.entries,
                    transactions = current.transactions,
                    portfolios = current.portfolios,
                    ids = ids,
                    originalId = original.id,
                    reversalDate = reversalDate,
                    portfolioId = form.portfolioId.orEmpty(),
                    ticker = form.ticker ?: if (standIns) PROBE_ID else return null,
                    date = date,
                    quantity = typed ?: if (standIns) picked.takeIf { it.signum() > 0 } ?: BigDecimal.ONE else return null,
                    totalCents = total.value,
                    picks = picks,
                )
            }

            else -> null
        }
    }

    /**
     * The pair at Review (14a decision 3): a refusal about the reversal date opens the combined review, but only when
     * the pair passes at the earliest date that works, so it never hides a second rule (14a risk 2); every other
     * refusal stays on the entry screen.
     */
    private fun pairAtReview(current: AppState): PairReview {
        val original = current.replacedTransaction ?: return PairReview.OnEntry(null)
        val date = DateDigits.toDate(current.replaceReversalDate) ?: original.date
        return when (val result = pairOf(current, PROBE_IDS, date, standIns = true)) {
            null -> PairReview.OnEntry(null)

            is ReentryResult.Created -> PairReview.Passes(result)

            is ReentryResult.Refused -> {
                val reason = (result.reason as? ReentryRefusal.OfReversal)?.reason
                val earliest =
                    when {
                        !result.reason.fixedOnReview -> null
                        reason is ReversalRefusal.TooEarly -> reason.earliest
                        else -> original.date
                    }
                when (val retried = earliest?.let { pairOf(current, PROBE_IDS, it, standIns = true) }) {
                    is ReentryResult.Created -> PairReview.OnReview(retried, result.reason)
                    is ReentryResult.Refused -> PairReview.OnEntry(retried.reason)
                    null -> PairReview.OnEntry(result.reason)
                }
            }
        }
    }

    /** The cash link of a buy or sell: its trade line's detail, which returns here. A split has none. */
    fun openCashLine() {
        val current = mutableState.value
        if (current.screen != Screen.TRANSACTION) return
        val line = current.transactionCashLine ?: return
        mutableState.update { it.copy(screen = Screen.DETAIL, detailId = line.id, detailFromTransaction = true, refusals = Refusals()) }
    }

    /**
     * The trade line's link to its buy or sell (wireframe 10b frame H). Opened from that transaction, it goes back to it
     * rather than opening it again, so there is no loop (13a decision 3).
     */
    fun openLinkedTransaction() {
        val current = mutableState.value
        if (current.screen != Screen.DETAIL) return
        val transaction = current.detailTradeTransaction ?: return
        mutableState.update {
            if (it.detailFromTransaction && it.transactionId == transaction.id) {
                it.copy(screen = Screen.TRANSACTION, detailId = null, detailFromTransaction = false)
            } else {
                it.copy(
                    screen = Screen.TRANSACTION,
                    transactionId = transaction.id,
                    transactionFrom = Screen.TABS,
                    detailId = null,
                    detailFromTransaction = false,
                )
            }
        }
    }

    /**
     * Opens the reversal for the shown entry, dated the original's date, with [defaultNote] pre-filled. The
     * screen passes the note because its wording is D9 and lives in the strings. Ignored unless Reverse is offered.
     */
    fun openReversal(defaultNote: String) {
        val current = mutableState.value
        if (current.screen != Screen.DETAIL || !current.reverseOffered) return
        val original = current.detailEntry ?: return
        mutableState.update {
            it.copy(
                screen = Screen.REVERSAL,
                reversal = ReversalForm(dateText = original.date.toString(), noteText = defaultNote),
                refusals = Refusals(),
            )
        }
    }

    fun onReversalDateChange(text: String) = mutableState.update {
        it.copy(reversal = it.reversal.copy(dateText = text), refusals = it.refusals.copy(date = null, cash = null))
    }

    fun onReversalNoteChange(text: String) = mutableState.update {
        it.copy(
            reversal = it.reversal.copy(noteText = text),
            refusals = it.refusals.copy(noteHasLineBreak = false, noteRequired = false),
        )
    }

    /** Back from the reversal to the detail. Nothing is saved. Ignored while a save is running. */
    fun cancelReversal() {
        val current = mutableState.value
        if (current.screen != Screen.REVERSAL || current.saving) return
        mutableState.update { it.copy(screen = Screen.DETAIL, reversal = ReversalForm(), refusals = Refusals()) }
    }

    /**
     * Checks and saves the reversal, one row (spec 4.6: one screen, no separate review). On a refusal the screen
     * stays with the typed text. On success it returns to the original's detail (05 decision 4).
     */
    fun confirmReversal() {
        val current = mutableState.value
        if (current.screen != Screen.REVERSAL || current.saving) return
        val original = current.detailEntry ?: return
        val target = store ?: return
        val dateText = current.reversal.dateText.trim()
        val date = MarketDate.parse(dateText)
        val dateRefusal =
            when {
                dateText.isEmpty() -> DateRefusal.EMPTY
                date == null -> DateRefusal.NOT_A_DATE
                else -> null
            }
        // Two passes of K1 so every refusal shows together, as in the form's review: the note with the original's
        // date as a stand-in, then (with a valid date) the date and balance rules with a stand-in note. One pass
        // hid the date refusal behind "note required" (defect found in 05 phone check 5).
        val noteProbe = CashEntries.reverse(current.entries, PROBE_ID, original.id, original.date, current.reversal.noteText)
        val noteRule = (noteProbe as? CashEntryResult.Refused)?.reason
        val cashRule =
            date?.let {
                val probe = CashEntries.reverse(current.entries, PROBE_ID, original.id, it, PROBE_NOTE)
                (probe as? CashEntryResult.Refused)?.reason
            }
        val refusals =
            Refusals(
                date = dateRefusal,
                noteHasLineBreak = noteRule == CashEntryRefusal.NoteHasLineBreak,
                noteRequired = noteRule == CashEntryRefusal.NoteRequired,
                cash = cashRule,
            )
        if (refusals != Refusals()) {
            mutableState.update { it.copy(refusals = refusals) }
            return
        }
        // The guard before saving: the same rules again, with the real id and the typed note.
        val result = CashEntries.reverse(current.entries, newId(), original.id, date ?: return, current.reversal.noteText)
        val entry = (result as? CashEntryResult.Created)?.entry
        if (entry == null) {
            mutableState.update { it.copy(refusals = refusals) }
            return
        }
        mutableState.update { it.copy(saving = true, refusals = Refusals()) }
        viewModelScope.launch {
            try {
                target.saveCashEntries(listOf(entry))
                mutableState.update {
                    valued(
                        it.copy(
                            screen = Screen.DETAIL,
                            entries = it.entries + entry,
                            reversal = ReversalForm(),
                            refusals = Refusals(),
                            saving = false,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(saving = false, refusals = Refusals(save = SaveRefusal.SAVE_FAILED))
                }
            }
        }
    }

    /** Back from the review to the form. The typed text is kept. */
    fun backToEdit() {
        val current = mutableState.value
        if (current.screen != Screen.REVIEW || current.saving) return
        mutableState.update { it.copy(screen = Screen.FORM, refusals = Refusals()) }
    }

    /** Saves the entry, one row. Ignored outside REVIEW and while a save is running. */
    fun confirm() {
        val current = mutableState.value
        if (current.screen != Screen.REVIEW || current.saving) return
        val target = store ?: return
        val entry = newEntry(current)
        if (entry == null) {
            mutableState.update { it.copy(screen = Screen.FORM) }
            return
        }
        mutableState.update { it.copy(saving = true, refusals = Refusals()) }
        viewModelScope.launch {
            try {
                target.saveCashEntries(listOf(entry))
                mutableState.update {
                    valued(
                        it.copy(
                            screen = Screen.TABS,
                            entries = it.entries + entry,
                            form = FormState(),
                            refusals = Refusals(),
                            saving = false,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(saving = false, refusals = Refusals(save = SaveRefusal.SAVE_FAILED))
                }
            }
        }
    }

    /** The guard before saving: the same rules again, with a real id. Null if any rule fails. */
    private fun newEntry(current: AppState): CashEntry? {
        val form = current.form
        val amount = Money.parseCents(form.amountText) as? AmountResult.Cents ?: return null
        val date = MarketDate.parse(form.dateText) ?: return null
        if (form.type == CashType.OPENING_BALANCE) {
            val result = OpeningBalance.create(current.entries, newId(), date, amount.value, form.noteText)
            return (result as? OpeningBalanceResult.Created)?.entry
        }
        val result = CashEntries.create(current.entries, newId(), form.type, date, amount.value, form.noteText, form.direction)
        return (result as? CashEntryResult.Created)?.entry
    }

    private fun showLoaded(
        entries: List<CashEntry>,
        portfolios: List<Portfolio>,
        transactions: List<Transaction>,
        instruments: List<Instrument>,
    ) {
        mutableState.update {
            valued(
                it.copy(
                    screen = Screen.TABS,
                    entries = entries,
                    portfolios = Portfolios.inCreationOrder(portfolios),
                    transactions = transactions,
                    instruments = instruments,
                ),
            )
        }
    }

    /** The holdings and net worth for [state]'s records, as of today in New York (4.8). */
    private fun valued(state: AppState): AppState {
        val today = marketDate.today()
        return state.copy(
            holdings = Holdings.holdings(state.transactions, today),
            netWorth = Valuation.netWorth(state.entries, state.transactions, today),
            today = today,
        )
    }

    /** Opens a holding from its row on the portfolio's screen (view 6). Unknown tickers are ignored. */
    fun openHolding(ticker: String) {
        val current = mutableState.value
        val id = current.portfolioId ?: return
        if (current.screen != Screen.PORTFOLIO || current.holdingsOf(id).none { it.ticker == ticker }) return
        mutableState.update {
            it.copy(screen = Screen.HOLDING, holdingTicker = ticker, holdingTab = HoldingTab.TRANSACTIONS, portfolioMenuOpen = false)
        }
    }

    /** "<" or Android Back: back to the portfolio. */
    fun closeHolding() {
        if (mutableState.value.screen != Screen.HOLDING) return
        mutableState.update { it.copy(screen = Screen.PORTFOLIO, holdingTicker = null, buyNotice = null) }
    }

    /** Not saved: the next opening starts on Transactions (view 6). */
    fun selectHoldingTab(tab: HoldingTab) {
        if (mutableState.value.screen != Screen.HOLDING) return
        mutableState.update { it.copy(holdingTab = tab) }
    }

    /** Buy in the holding's header: the ticker and portfolio filled in; Back and a save return here (10 decision 2). */
    fun buyFromHolding() {
        val current = mutableState.value
        if (current.screen != Screen.HOLDING || current.saving) return
        val ticker = current.holdingTicker ?: return
        openBuy(portfolioId = current.portfolioId, ticker = ticker)
    }

    /** Buy/Sell (navigation outline): opens the short menu over Overall, Portfolios or Cash. */
    fun openBuySellMenu() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.saving) return
        mutableState.update { it.copy(buySellMenuOpen = true, buyNotice = null) }
    }

    fun closeBuySellMenu() = mutableState.update { it.copy(buySellMenuOpen = false) }

    /**
     * Buy in the menu: before the opening balance exists it asks for it, with no portfolio it asks for one (4.2,
     * criterion 24), otherwise the buy entry opens. Ignored off the tabs.
     */
    fun chooseBuy() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.saving) return
        val notice =
            when {
                current.cashEntryType == CashType.OPENING_BALANCE -> BuyNotice.NEEDS_OPENING_BALANCE
                current.portfolios.isEmpty() -> BuyNotice.NEEDS_PORTFOLIO
                else -> null
            }
        if (notice != null) {
            mutableState.update { it.copy(buySellMenuOpen = false, buyNotice = notice) }
            return
        }
        openBuy(portfolioId = null)
    }

    /** Sell in the menu: with no holding that has shares it says so (criterion 24), otherwise the sell entry opens. */
    fun chooseSell() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.saving) return
        if (current.holdings.none { it.shares.signum() > 0 }) {
            mutableState.update { it.copy(buySellMenuOpen = false, buyNotice = BuyNotice.NOTHING_TO_SELL) }
            return
        }
        openSell(portfolioId = null)
    }

    /**
     * Sell in the holding's header: the portfolio and ticker chosen; Back and a save return here. At 0 shares it
     * says so instead (11 decision 2).
     */
    fun sellFromHolding() {
        val current = mutableState.value
        if (current.screen != Screen.HOLDING || current.saving) return
        val holding = current.shownHolding ?: return
        if (holding.shares.signum() == 0) {
            mutableState.update { it.copy(buyNotice = BuyNotice.NOTHING_TO_SELL_IN_HOLDING) }
            return
        }
        openSell(portfolioId = holding.portfolioId, ticker = holding.ticker)
    }

    /**
     * Opens the sell entry. The typed text is kept until a sell is saved; the date is filled with today in New York
     * only if it is empty. The portfolio is [portfolioId], else the kept one, else the first; the ticker is [ticker],
     * else the kept one if still held there, else the first held (11 decision 1). Picks are kept only for the same
     * portfolio and ticker.
     */
    private fun openSell(
        portfolioId: String?,
        ticker: String? = null,
    ) = mutableState.update { current ->
        val form = current.sellForm
        val kept = form.portfolioId?.takeIf { id -> current.portfolios.any { it.id == id } }
        val chosen = portfolioId ?: kept ?: current.portfolios.firstOrNull()?.id
        val held = chosen?.let { current.heldTickers(it) }.orEmpty()
        val keptTicker = form.ticker?.takeIf { chosen == form.portfolioId && it in held }
        val chosenTicker = ticker ?: keptTicker ?: held.firstOrNull()
        val samePlace = chosen == form.portfolioId && chosenTicker == form.ticker
        val date = form.dateText.ifEmpty { marketDate.today().toString().replace("-", "") }
        current.copy(
            screen = Screen.SELL,
            buySellMenuOpen = false,
            buyNotice = null,
            sellForm =
            form.copy(
                portfolioId = chosen,
                ticker = chosenTicker,
                dateText = date,
                picks = if (samePlace) form.picks else emptyMap(),
            ),
            sellRefusals = SellRefusals(),
            sellPicksDropped = false,
            sellFromHolding = ticker != null,
            sellReview = null,
        )
    }

    /** One of the portfolios only. The ticker becomes its first held one, and the picks are cleared. */
    fun onSellPortfolioChange(id: String) {
        val current = mutableState.value
        if (current.portfolios.none { it.id == id }) return
        mutableState.update {
            it.copy(
                sellForm = it.sellForm.copy(portfolioId = id, ticker = it.heldTickers(id).firstOrNull(), picks = emptyMap()),
                sellRefusals = it.sellRefusals.copy(tickerMissing = false, picks = emptyMap(), rule = null),
                sellPicksDropped = false,
            )
        }
    }

    /** One of the tickers held in the chosen portfolio only (11 decision 1). The picks are cleared. */
    fun onSellTickerChange(ticker: String) {
        if (ticker !in mutableState.value.sellTickers) return
        mutableState.update {
            it.copy(
                sellForm = it.sellForm.copy(ticker = ticker, picks = emptyMap()),
                sellRefusals = it.sellRefusals.copy(tickerMissing = false, picks = emptyMap(), rule = null),
                sellPicksDropped = false,
            )
        }
    }

    /**
     * Digits only, as Buy's date (09 decision 10). Once the date is whole, the lot list follows it and a pick that
     * no longer passes the lot rules is dropped, with a line (wireframe 10a marker 11).
     */
    fun onSellDateChange(text: String) {
        if (tooManyDigits(text)) return
        val refusal = if (DateDigits.isDigits(text)) null else DateRefusal.NOT_DIGITS
        mutableState.update { state ->
            val dated =
                state.copy(
                    sellForm = state.sellForm.copy(dateText = text),
                    sellRefusals = state.sellRefusals.copy(date = refusal, rule = null),
                )
            if (DateDigits.toDate(text) == null) return@update dated
            val passing = dated.sellLotChoices.filter { it.unavailable == null }.map { it.buyId }.toSet()
            val kept = dated.sellForm.picks.filterKeys { it in passing }
            if (kept.size == dated.sellForm.picks.size) {
                dated
            } else {
                dated.copy(sellForm = dated.sellForm.copy(picks = kept), sellPicksDropped = true)
            }
        }
    }

    fun onSellQuantityChange(text: String) = mutableState.update {
        it.copy(sellForm = it.sellForm.copy(quantityText = text), sellRefusals = it.sellRefusals.copy(quantity = null, rule = null))
    }

    fun onSellTotalChange(text: String) = mutableState.update {
        it.copy(sellForm = it.sellForm.copy(totalText = text), sellRefusals = it.sellRefusals.copy(total = null, rule = null))
    }

    /**
     * A tap on a lot picks it with its full remainder, as the picker shows it; a second tap unpicks it. A lot that
     * fails the lot rules, or is not listed, cannot be picked (wireframe 10a marker 10).
     */
    fun toggleLot(lotId: String) = mutableState.update { state ->
        val picks = state.sellForm.picks
        val next =
            if (lotId in picks) {
                picks - lotId
            } else {
                val choice = state.sellLotChoices.firstOrNull { it.buyId == lotId && it.unavailable == null } ?: return@update state
                picks + (lotId to Sells.shown(choice.available).toPlainString())
            }
        state.copy(
            sellForm = state.sellForm.copy(picks = next),
            sellRefusals = state.sellRefusals.copy(picks = state.sellRefusals.picks - lotId, rule = null),
            sellPicksDropped = false,
        )
    }

    /** A picked lot's quantity, typed for a partial lot. Ignored for a lot that is not picked. */
    fun onPickChange(
        lotId: String,
        text: String,
    ) {
        if (lotId !in mutableState.value.sellForm.picks) return
        mutableState.update {
            it.copy(
                sellForm = it.sellForm.copy(picks = it.sellForm.picks + (lotId to text)),
                sellRefusals = it.sellRefusals.copy(picks = it.sellRefusals.picks - lotId, rule = null),
                sellPicksDropped = false,
            )
        }
    }

    /** Sell all (4.4): picks every lot that passes, each at its remainder to 8 places, and fills the quantity with their sum. */
    fun sellAll() = mutableState.update { state ->
        val picks = Sells.sellAll(state.sellLotChoices)
        if (picks.isEmpty()) return@update state
        val quantity = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + pick.quantity }
        state.copy(
            sellForm =
            state.sellForm.copy(
                picks = picks.associate { it.lotId to it.quantity.toPlainString() },
                quantityText = quantity.toPlainString(),
            ),
            sellRefusals = state.sellRefusals.copy(quantity = null, picks = emptyMap(), rule = null),
            sellPicksDropped = false,
        )
    }

    /**
     * Back from the sell entry to the tab, or to the holding it came from; the typed text is kept. While replacing, back
     * to the original's detail, nothing kept (10c marker 1).
     */
    fun closeSell() {
        val current = mutableState.value
        if (current.screen != Screen.SELL) return
        if (current.replacingId != null) {
            mutableState.update { it.copy(screen = Screen.TRANSACTION, sellRefusals = SellRefusals(), sellPicksDropped = false).leftReplace() }
            return
        }
        mutableState.update {
            it.copy(screen = afterSell(it), sellRefusals = SellRefusals(), sellPicksDropped = false, sellFromHolding = false)
        }
    }

    // Where the sell entry returns to: the holding it was opened from (11 decision 2), else the tab.
    private fun afterSell(state: AppState): Screen = if (state.sellFromHolding) Screen.HOLDING else Screen.TABS

    /**
     * Review (wireframe 10a marker 6): the field rules and, with stand-ins, K1's rules (4.1, 4.2, 4.4) together. No
     * ticker check: a sell never needs one (4.6). Shows the review only if the save would pass. While replacing, K1
     * checks the pair and the combined review opens (14a decision 3).
     */
    fun reviewSell() {
        val current = mutableState.value
        if (current.screen != Screen.SELL || current.saving) return
        if (current.replacingId != null) {
            reviewReplacedSell(current)
            return
        }
        val probe = sellOf(current, PROBE_ID, PROBE_ID, standIns = true)
        val refusals = sellFieldRefusals(current).copy(rule = shownSellRule(current, (probe as? SellResult.Refused)?.reason))
        if (refusals != SellRefusals() || probe !is SellResult.Created) {
            mutableState.update { it.copy(sellRefusals = refusals) }
            return
        }
        mutableState.update { it.copy(screen = Screen.SELL_REVIEW, sellReview = probe, sellRefusals = SellRefusals()) }
    }

    // Review of a replacing sell: the new sell's own rules show on the entry, the reversal's in replaceRefusals.
    private fun reviewReplacedSell(current: AppState) {
        val review = pairAtReview(current)
        val entryRule = (review as? PairReview.OnEntry)?.reason
        val refusals = sellFieldRefusals(current).copy(rule = shownSellRule(current, (entryRule as? ReentryRefusal.OfSell)?.reason))
        val reversalRule = entryRule as? ReentryRefusal.OfReversal
        if (refusals != SellRefusals() || review is PairReview.OnEntry) {
            mutableState.update { it.copy(sellRefusals = refusals, replaceRefusals = ReplaceRefusals(rule = reversalRule)) }
            return
        }
        mutableState.update {
            it.copy(
                screen = Screen.REPLACE_REVIEW,
                sellRefusals = SellRefusals(),
                replaceReview = review.created,
                replaceRefusals = ReplaceRefusals(rule = (review as? PairReview.OnReview)?.refusal),
                replaceDateEditing = DateDigits.toDate(it.replaceReversalDate) == null,
            )
        }
    }

    // The sell's field rules, shown together (wireframe 10a).
    private fun sellFieldRefusals(current: AppState): SellRefusals {
        val form = current.sellForm
        return SellRefusals(
            tickerMissing = form.ticker == null,
            date = digitsDateRefusal(form.dateText),
            quantity = (Quantity.parse(form.quantityText) as? QuantityResult.Refused)?.reason,
            total = (sellTotal(form.totalText) as? AmountResult.Refused)?.reason,
            picks = form.picks.mapNotNull { (lotId, text) -> (Quantity.parse(text) as? QuantityResult.Refused)?.let { lotId to it.reason } }.toMap(),
        )
    }

    // A stand-in quantity or a missing pick makes the holding and add-up rules meaningless; the field refusals say why.
    private fun shownSellRule(
        current: AppState,
        reason: SellRefusal?,
    ): SellRefusal? {
        val form = current.sellForm
        val standIn = Quantity.parse(form.quantityText) !is QuantityResult.Exact || form.ticker == null
        val pickErrors = form.picks.values.any { Quantity.parse(it) is QuantityResult.Refused }
        return when {
            reason is SellRefusal.MoreThanHeld && standIn -> null
            reason is SellRefusal.PicksDoNotAddUp && (standIn || pickErrors) -> null
            else -> reason
        }
    }

    /** Back to edit from the sell review. The typed text is kept. */
    fun backToSell() {
        val current = mutableState.value
        if (current.screen != Screen.SELL_REVIEW || current.saving) return
        mutableState.update { it.copy(screen = Screen.SELL, sellRefusals = SellRefusals(), sellReview = null) }
    }

    /**
     * Confirm and save (criterion 30): K1's rules again with real ids, then the sell, its cash line and its
     * allocations are saved together (open item 14). A rule that fails now returns to the entry with the reason.
     */
    fun confirmSell() {
        val current = mutableState.value
        if (current.screen != Screen.SELL_REVIEW || current.saving) return
        val target = store ?: return
        val created =
            when (val result = sellOf(current, newId(), newId())) {
                is SellResult.Created -> result

                is SellResult.Refused -> {
                    mutableState.update { it.copy(screen = Screen.SELL, sellRefusals = SellRefusals(rule = result.reason), sellReview = null) }
                    return
                }

                null -> {
                    mutableState.update { it.copy(screen = Screen.SELL, sellReview = null) }
                    return
                }
            }
        mutableState.update { it.copy(saving = true, sellRefusals = SellRefusals()) }
        viewModelScope.launch {
            try {
                target.saveSell(created.transaction, created.cashLine)
                mutableState.update {
                    valued(
                        it.copy(
                            screen = afterSell(it),
                            entries = it.entries + created.cashLine,
                            transactions = it.transactions + created.transaction,
                            sellForm = SellForm(),
                            sellRefusals = SellRefusals(),
                            saving = false,
                            sellFromHolding = false,
                            sellReview = null,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, sellRefusals = SellRefusals(save = SaveRefusal.SAVE_FAILED)) }
            }
        }
    }

    /**
     * K1's sell rules on the typed values; null when the date or total does not parse (the field rules decide then).
     * Without [standIns] it is also null when the quantity, the ticker or a pick does not parse. With [standIns], a bad
     * quantity counts as the sum of the valid picks (or 1), a missing ticker as a placeholder, and bad picks are left out.
     */
    private fun sellOf(
        current: AppState,
        transactionId: String,
        cashEntryId: String,
        standIns: Boolean = false,
    ): SellResult? {
        val form = current.sellForm
        val date = DateDigits.toDate(form.dateText) ?: return null
        val total = sellTotal(form.totalText) as? AmountResult.Cents ?: return null
        val picks = form.picks.mapNotNull { (lotId, text) -> (Quantity.parse(text) as? QuantityResult.Exact)?.let { Pick(lotId, it.value) } }
        if (!standIns && picks.size != form.picks.size) return null
        val picked = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + pick.quantity }
        val typed = (Quantity.parse(form.quantityText) as? QuantityResult.Exact)?.value
        val quantity = typed ?: if (standIns) picked.takeIf { it.signum() > 0 } ?: BigDecimal.ONE else return null
        val ticker = form.ticker ?: if (standIns) PROBE_ID else return null
        return Sells.sell(
            cashEntries = current.entries,
            transactions = current.transactions,
            portfolios = current.portfolios,
            transactionId = transactionId,
            cashEntryId = cashEntryId,
            portfolioId = form.portfolioId.orEmpty(),
            ticker = ticker,
            date = date,
            quantity = quantity,
            totalCents = total.value,
            picks = picks,
        )
    }

    /**
     * Split in the holding's header (navigation outline): the holding's portfolio and ticker chosen (12 decision 1),
     * also at 0 shares (12 decision 2). The typed text is kept until a split is saved; the date is filled with today
     * in New York only if it is empty.
     */
    fun splitFromHolding() {
        val current = mutableState.value
        if (current.screen != Screen.HOLDING || current.saving) return
        val portfolioId = current.portfolioId ?: return
        val ticker = current.holdingTicker ?: return
        mutableState.update {
            val form = it.splitForm
            it.copy(
                screen = Screen.SPLIT,
                buyNotice = null,
                splitForm =
                form.copy(
                    portfolioId = portfolioId,
                    ticker = ticker,
                    dateText = form.dateText.ifEmpty { marketDate.today().toString().replace("-", "") },
                ),
                splitRefusals = SplitRefusals(),
                splitReview = null,
            )
        }
    }

    /** One of the portfolios only. The ticker becomes its first with a buy, or none. */
    fun onSplitPortfolioChange(id: String) {
        val current = mutableState.value
        if (current.portfolios.none { it.id == id }) return
        mutableState.update {
            it.copy(
                splitForm = it.splitForm.copy(portfolioId = id, ticker = Splits.tickers(it.transactions, id).firstOrNull()),
                splitRefusals = it.splitRefusals.copy(tickerMissing = false, rule = null),
            )
        }
    }

    /** One of the tickers with a buy in the chosen portfolio only. */
    fun onSplitTickerChange(ticker: String) {
        if (ticker !in mutableState.value.splitTickers) return
        mutableState.update {
            it.copy(splitForm = it.splitForm.copy(ticker = ticker), splitRefusals = it.splitRefusals.copy(tickerMissing = false, rule = null))
        }
    }

    /** Digits only, as Buy's and Sell's date (09 decision 10). */
    fun onSplitDateChange(text: String) {
        if (tooManyDigits(text)) return
        val refusal = if (DateDigits.isDigits(text)) null else DateRefusal.NOT_DIGITS
        mutableState.update {
            it.copy(splitForm = it.splitForm.copy(dateText = text), splitRefusals = it.splitRefusals.copy(date = refusal, rule = null))
        }
    }

    fun onSplitToChange(text: String) = mutableState.update {
        it.copy(splitForm = it.splitForm.copy(toText = text), splitRefusals = it.splitRefusals.copy(to = null, rule = null))
    }

    fun onSplitFromChange(text: String) = mutableState.update {
        it.copy(splitForm = it.splitForm.copy(fromText = text), splitRefusals = it.splitRefusals.copy(from = null, rule = null))
    }

    /** Back from the split entry to the holding it came from. The typed text is kept. */
    fun closeSplit() {
        if (mutableState.value.screen != Screen.SPLIT) return
        mutableState.update { it.copy(screen = Screen.HOLDING, splitRefusals = SplitRefusals()) }
    }

    /**
     * Review (wireframe 10b marker 10): the field rules and, with stand-ins, K1's rules (4.1, 4.5) together. A stand-in
     * ratio makes the ratio and replay rules meaningless, and a missing ticker the ticker rule; the field refusals say
     * why. No ticker check: a split never needs one (4.6). Shows the review only if the save would pass.
     */
    fun reviewSplit() {
        val current = mutableState.value
        if (current.screen != Screen.SPLIT || current.saving) return
        val form = current.splitForm
        val fieldRefusals =
            SplitRefusals(
                tickerMissing = form.ticker == null,
                date = digitsDateRefusal(form.dateText),
                to = ratioError(form.toText),
                from = ratioError(form.fromText),
            )
        val probe = splitOf(current, PROBE_ID, standIns = true)
        val reason = (probe as? SplitResult.Refused)?.reason
        val ratioStandIn = fieldRefusals.to != null || fieldRefusals.from != null
        val rule =
            when {
                ratioStandIn && (reason is SplitRefusal.SellShort || reason == SplitRefusal.RatioEqual) -> null
                form.ticker == null && reason == SplitRefusal.TickerNotBought -> null
                else -> reason
            }
        val refusals = fieldRefusals.copy(rule = rule)
        if (refusals != SplitRefusals() || probe !is SplitResult.Created) {
            mutableState.update { it.copy(splitRefusals = refusals) }
            return
        }
        mutableState.update { it.copy(screen = Screen.SPLIT_REVIEW, splitReview = probe, splitRefusals = SplitRefusals()) }
    }

    /** Back to edit from the split review. The typed text is kept. */
    fun backToSplit() {
        val current = mutableState.value
        if (current.screen != Screen.SPLIT_REVIEW || current.saving) return
        mutableState.update { it.copy(screen = Screen.SPLIT, splitRefusals = SplitRefusals(), splitReview = null) }
    }

    /**
     * Confirm and save: K1's rules again with a real id, then the split is saved; it has no cash line (4.5). A rule
     * that fails now returns to the entry with the reason. A save returns to the holding the split was opened from.
     */
    fun confirmSplit() {
        val current = mutableState.value
        if (current.screen != Screen.SPLIT_REVIEW || current.saving) return
        val target = store ?: return
        val created =
            when (val result = splitOf(current, newId())) {
                is SplitResult.Created -> result

                is SplitResult.Refused -> {
                    mutableState.update { it.copy(screen = Screen.SPLIT, splitRefusals = SplitRefusals(rule = result.reason), splitReview = null) }
                    return
                }

                null -> {
                    mutableState.update { it.copy(screen = Screen.SPLIT, splitReview = null) }
                    return
                }
            }
        mutableState.update { it.copy(saving = true, splitRefusals = SplitRefusals()) }
        viewModelScope.launch {
            try {
                target.saveSplit(created.transaction)
                mutableState.update {
                    valued(
                        it.copy(
                            screen = Screen.HOLDING,
                            transactions = it.transactions + created.transaction,
                            splitForm = SplitForm(),
                            splitRefusals = SplitRefusals(),
                            saving = false,
                            splitReview = null,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, splitRefusals = SplitRefusals(save = SaveRefusal.SAVE_FAILED)) }
            }
        }
    }

    /**
     * K1's split rules on the typed values; null when the date does not parse (the field rules decide then). Without
     * [standIns] it is also null when a number or the ticker is missing. With [standIns], a bad number makes the ratio
     * 2 for 1 and a missing ticker a placeholder.
     */
    private fun splitOf(
        current: AppState,
        transactionId: String,
        standIns: Boolean = false,
    ): SplitResult? {
        val form = current.splitForm
        val date = DateDigits.toDate(form.dateText) ?: return null
        val to = ratioNumber(form.toText)
        val from = ratioNumber(form.fromText)
        val ratio =
            if (to != null && from != null) {
                SplitRatio(to, from)
            } else if (standIns) {
                SplitRatio(2, 1)
            } else {
                return null
            }
        val ticker = form.ticker ?: if (standIns) PROBE_ID else return null
        return Splits.split(
            cashEntries = current.entries,
            transactions = current.transactions,
            portfolios = current.portfolios,
            transactionId = transactionId,
            portfolioId = form.portfolioId.orEmpty(),
            ticker = ticker,
            date = date,
            ratio = ratio,
        )
    }

    fun dismissBuyNotice() = mutableState.update { it.copy(buyNotice = null) }

    /** Create a portfolio from the Buy notice: the New portfolio dialog, then Buy once it is created (criterion 24). */
    fun createPortfolioForBuy() {
        mutableState.update { it.copy(buyNotice = null, buyAfterPortfolio = true) }
        openPortfolioDialog()
    }

    /**
     * Opens the buy entry. The typed text is kept until a buy is saved; the date is filled with today in New York
     * only if it is empty. The portfolio is [portfolioId], else the kept one if it still exists, else the first.
     * A [ticker] is given only from a holding's header, which Back and a save then return to (10 decision 2).
     */
    private fun openBuy(
        portfolioId: String?,
        ticker: String? = null,
    ) = mutableState.update { current ->
        val form = current.buyForm
        val kept = form.portfolioId?.takeIf { id -> current.portfolios.any { it.id == id } }
        val chosen = portfolioId ?: kept ?: current.portfolios.firstOrNull()?.id
        // Digits only, as the Cash filters (09 decision 10): today as YYYYMMDD, shown with its dashes.
        val date = form.dateText.ifEmpty { marketDate.today().toString().replace("-", "") }
        current.copy(
            screen = Screen.BUY,
            buySellMenuOpen = false,
            buyNotice = null,
            buyForm = form.copy(portfolioId = chosen, dateText = date, tickerText = ticker ?: form.tickerText),
            buyRefusals = BuyRefusals(),
            buyFromHolding = ticker != null,
        )
    }

    /** One of the portfolios only; other ids are ignored. */
    fun onBuyPortfolioChange(id: String) {
        if (mutableState.value.portfolios.none { it.id == id }) return
        mutableState.update { it.copy(buyForm = it.buyForm.copy(portfolioId = id), buyRefusals = it.buyRefusals.copy(rule = null)) }
    }

    /** The ticker is shown in capitals as it is typed (wireframe 10a marker 2). */
    fun onBuyTickerChange(text: String) = mutableState.update {
        it.copy(
            buyForm = it.buyForm.copy(tickerText = text.uppercase(Locale.ROOT)),
            buyRefusals = it.buyRefusals.copy(tickerEmpty = false, check = null, rule = null),
        )
    }

    /**
     * Digits only, as the Cash filters (09 decision 10): a ninth digit is not accepted, and any other character is kept
     * so its refusal shows at once.
     */
    fun onBuyDateChange(text: String) {
        if (tooManyDigits(text)) return
        val refusal = if (DateDigits.isDigits(text)) null else DateRefusal.NOT_DIGITS
        mutableState.update {
            it.copy(buyForm = it.buyForm.copy(dateText = text), buyRefusals = it.buyRefusals.copy(date = refusal, rule = null))
        }
    }

    fun onBuyQuantityChange(text: String) = mutableState.update {
        it.copy(buyForm = it.buyForm.copy(quantityText = text), buyRefusals = it.buyRefusals.copy(quantity = null, rule = null))
    }

    fun onBuyTotalChange(text: String) = mutableState.update {
        it.copy(buyForm = it.buyForm.copy(totalText = text), buyRefusals = it.buyRefusals.copy(total = null, rule = null))
    }

    /**
     * Back from the buy entry to the tab, or to the holding it came from; the typed text is kept. While replacing, back
     * to the original's detail, nothing kept (10c marker 1). Ignored while the check runs.
     */
    fun closeBuy() {
        val current = mutableState.value
        if (current.screen != Screen.BUY || current.checking) return
        if (current.replacingId != null) {
            mutableState.update { it.copy(screen = Screen.TRANSACTION, buyRefusals = BuyRefusals()).leftReplace() }
            return
        }
        mutableState.update {
            it.copy(screen = afterBuy(it), buyRefusals = BuyRefusals(), buyFromHolding = false)
        }
    }

    // Where the buy entry returns to: the holding it was opened from (10 decision 2), else the tab.
    private fun afterBuy(state: AppState): Screen = if (state.buyFromHolding) Screen.HOLDING else Screen.TABS

    /**
     * Review (wireframe 10a marker 6): the field rules, then K1's rules, then the ticker check of 4.3 through K4.
     * Each later stage runs only when the earlier ones pass. Shows the review only if the save would pass. While
     * replacing, K1 checks the pair, the new buy still gets the ticker check (4.6), and the combined review opens
     * (14a decision 3).
     */
    fun reviewBuy() {
        val current = mutableState.value
        if (current.screen != Screen.BUY || current.checking || current.saving) return
        val target = store ?: return
        val form = current.buyForm
        val ticker = TickerCheck.normalize(form.tickerText)
        val quantity = Quantity.parse(form.quantityText)
        val total = Money.parseCents(form.totalText)
        val fieldRefusals =
            BuyRefusals(
                tickerEmpty = ticker == null,
                date = digitsDateRefusal(form.dateText),
                quantity = (quantity as? QuantityResult.Refused)?.reason,
                total = (total as? AmountResult.Refused)?.reason,
            )
        // K1's rules with stand-ins for a bad ticker or quantity, so a field error does not hide the cash or date rule
        // (defect found in phone check 7; CLAUDE.md pitfall from 04 and 05). Null when the date or total is bad.
        val replacing = current.replacingId != null
        val pair = if (replacing) pairAtReview(current) else null
        val entryRule = (pair as? PairReview.OnEntry)?.reason
        val rule =
            if (replacing) {
                (entryRule as? ReentryRefusal.OfBuy)?.reason
            } else {
                (buyOf(current, PROBE_ID, PROBE_ID, standIns = true) as? BuyResult.Refused)?.reason
            }
        val refusals = fieldRefusals.copy(rule = rule)
        val reversalRule = entryRule as? ReentryRefusal.OfReversal
        if (refusals != BuyRefusals() || pair is PairReview.OnEntry) {
            mutableState.update { it.copy(buyRefusals = refusals, replaceRefusals = ReplaceRefusals(rule = reversalRule)) }
            return
        }
        val checked = ticker ?: return
        mutableState.update { it.copy(checking = true, buyRefusals = BuyRefusals(), replaceRefusals = ReplaceRefusals()) }
        viewModelScope.launch {
            try {
                when (val result = TickerVerifier.verify(checked, current.instruments, isOnline(), priceSource, target)) {
                    is Verification.Verified -> mutableState.update { state ->
                        val instruments = state.instruments.filter { it.ticker != result.instrument.ticker } + result.instrument
                        state.copy(
                            screen = if (replacing) Screen.REPLACE_REVIEW else Screen.BUY_REVIEW,
                            instruments = instruments,
                            checking = false,
                            replaceReview = pair?.created,
                            replaceRefusals = ReplaceRefusals(rule = (pair as? PairReview.OnReview)?.refusal),
                            // An unfinished reversal date kept through Back to edit opens in its field (Deploy finding 1).
                            replaceDateEditing = replacing && DateDigits.toDate(state.replaceReversalDate) == null,
                        )
                    }

                    is Verification.Refused -> mutableState.update {
                        it.copy(checking = false, buyRefusals = BuyRefusals(check = result.reason))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, checking = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(checking = false, buyRefusals = BuyRefusals(save = SaveRefusal.SAVE_FAILED)) }
            }
        }
    }

    /** Back to edit from the buy review. The typed text is kept. */
    fun backToBuy() {
        val current = mutableState.value
        if (current.screen != Screen.BUY_REVIEW || current.saving) return
        mutableState.update { it.copy(screen = Screen.BUY, buyRefusals = BuyRefusals()) }
    }

    /**
     * Confirm and save (criterion 30): K1's rules again with real ids, then the buy and its cash line are saved
     * together (open item 14). A rule that fails now returns to the entry with the reason.
     */
    fun confirmBuy() {
        val current = mutableState.value
        if (current.screen != Screen.BUY_REVIEW || current.saving) return
        val target = store ?: return
        val result = buyOf(current, newId(), newId())
        val created =
            when (result) {
                is BuyResult.Created -> result

                is BuyResult.Refused -> {
                    mutableState.update { it.copy(screen = Screen.BUY, buyRefusals = BuyRefusals(rule = result.reason)) }
                    return
                }

                null -> {
                    mutableState.update { it.copy(screen = Screen.BUY) }
                    return
                }
            }
        mutableState.update { it.copy(saving = true, buyRefusals = BuyRefusals()) }
        viewModelScope.launch {
            try {
                target.saveBuy(created.transaction, created.cashLine)
                mutableState.update {
                    valued(
                        it.copy(
                            screen = afterBuy(it),
                            entries = it.entries + created.cashLine,
                            transactions = it.transactions + created.transaction,
                            buyForm = BuyForm(),
                            buyRefusals = BuyRefusals(),
                            saving = false,
                            buyFromHolding = false,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, buyRefusals = BuyRefusals(save = SaveRefusal.SAVE_FAILED)) }
            }
        }
    }

    /**
     * K1's buy rules on the typed values; null when the date or total does not parse (the field rules decide then).
     * With [standIns], a bad quantity counts as 1 and an empty ticker as a placeholder, so the cash and date rules,
     * which need neither, are still judged. A missing portfolio is refused by K1 as an unknown one.
     */
    private fun buyOf(
        current: AppState,
        transactionId: String,
        cashEntryId: String,
        standIns: Boolean = false,
    ): BuyResult? {
        val form = current.buyForm
        val date = DateDigits.toDate(form.dateText) ?: return null
        val total = Money.parseCents(form.totalText) as? AmountResult.Cents ?: return null
        val typed = (Quantity.parse(form.quantityText) as? QuantityResult.Exact)?.value
        val quantity = typed ?: if (standIns) BigDecimal.ONE else return null
        val ticker = if (standIns && form.tickerText.isBlank()) PROBE_ID else form.tickerText
        return Transactions.buy(
            cashEntries = current.entries,
            transactions = current.transactions,
            portfolios = current.portfolios,
            transactionId = transactionId,
            cashEntryId = cashEntryId,
            portfolioId = form.portfolioId.orEmpty(),
            ticker = ticker,
            date = date,
            quantity = quantity,
            totalCents = total.value,
        )
    }

    private companion object {
        const val PROBE_ID = "check"

        // The types the form offers once the opening balance exists (03 decision 2).
        val ENTRY_TYPES = setOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT)

        // Not blank and one line, so the review's second K1 pass judges only the date and balance rules.
        const val PROBE_NOTE = "check"

        // The screens that show the bottom bar (spec 6: always visible; 02b decision 1).
        val TAB_BAR_SCREENS =
            setOf(
                Screen.TABS,
                Screen.FORM,
                Screen.REVIEW,
                Screen.DETAIL,
                Screen.REVERSAL,
                Screen.PORTFOLIO,
                Screen.BUY,
                Screen.BUY_REVIEW,
                Screen.HOLDING,
                Screen.SELL,
                Screen.SELL_REVIEW,
                Screen.SPLIT,
                Screen.SPLIT_REVIEW,
                Screen.TRANSACTION,
                Screen.REVERSE_TRANSACTION,
                Screen.REPLACE_REVIEW,
            )
    }
}
