package app.ghghorg.atsassets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.BuyRefusal
import app.ghghorg.atsassets.core.BuyResult
import app.ghghorg.atsassets.core.CashAccount
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntries
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashEntryResult
import app.ghghorg.atsassets.core.CashFilter
import app.ghghorg.atsassets.core.CashPairResult
import app.ghghorg.atsassets.core.CashRow
import app.ghghorg.atsassets.core.CashRules
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.Classification
import app.ghghorg.atsassets.core.DeletionCheck
import app.ghghorg.atsassets.core.DeletionSummary
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Hiding
import app.ghghorg.atsassets.core.HistoryChip
import app.ghghorg.atsassets.core.HistoryFilter
import app.ghghorg.atsassets.core.HistoryRow
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.ListResult
import app.ghghorg.atsassets.core.ListUsers
import app.ghghorg.atsassets.core.Lot
import app.ghghorg.atsassets.core.LotChoice
import app.ghghorg.atsassets.core.MarketDate
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.OpeningBalance
import app.ghghorg.atsassets.core.OpeningBalanceError
import app.ghghorg.atsassets.core.OpeningBalanceResult
import app.ghghorg.atsassets.core.Pick
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PortfolioDeletion
import app.ghghorg.atsassets.core.PortfolioRefusal
import app.ghghorg.atsassets.core.PortfolioRemoval
import app.ghghorg.atsassets.core.PortfolioRenameResult
import app.ghghorg.atsassets.core.PortfolioResult
import app.ghghorg.atsassets.core.Portfolios
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.PriceSource
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.QuantityError
import app.ghghorg.atsassets.core.QuantityResult
import app.ghghorg.atsassets.core.RealizedReport
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
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.SplitEffect
import app.ghghorg.atsassets.core.SplitPairResult
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.SplitRefusal
import app.ghghorg.atsassets.core.SplitResult
import app.ghghorg.atsassets.core.Splits
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
import app.ghghorg.atsassets.core.TickerCheck
import app.ghghorg.atsassets.core.TradeSide
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionHistory
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.Transactions
import app.ghghorg.atsassets.core.UnreadableDataException
import app.ghghorg.atsassets.core.Valuation
import app.ghghorg.atsassets.prices.CheckRefusal
import app.ghghorg.atsassets.prices.PriceRefresh
import app.ghghorg.atsassets.prices.TickerVerifier
import app.ghghorg.atsassets.prices.Verification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
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

    // A portfolio's hidden holdings (view 9, wireframe 09), opened from the portfolio's screen (iteration 15).
    HIDDEN_HOLDINGS,

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

    // Net Worth (Total) (view 2, wireframe 02, iteration 17), opened from Overall's net worth or from More.
    NET_WORTH,

    // Allocation, one portfolio (view 4, wireframe 04, iteration 19), opened from a portfolio's screen or from More.
    ALLOCATION,

    // Settings (section 6, wireframe 10f frame B, iteration 20), opened from More's gear.
    SETTINGS,

    // The tab order (wireframe 10f frame E, iteration 22), opened from Settings' Navigation section.
    TAB_ORDER,

    // The Edit picker of a holding's sector or geography (wireframe 10f frame H, iteration 24), opened from its Classification tab.
    CLASS_PICKER,

    // A sector or geography list (wireframe 10f frame F, iteration 25), opened from Settings' Lists section.
    CLASS_LIST,

    // Realized gains (view 5, wireframe 05, iteration 26), opened from More.
    REALIZED_GAINS,

    // A portfolio's Transaction history (view 8, wireframe 08, iteration 27), opened from the portfolio's screen or from a
    // sale on Realized gains (27 decision 1).
    TRANSACTION_HISTORY,
}

/**
 * The Name layer of a list screen (wireframe 10f frame G, marker 14; 25): [renaming] is the value being renamed, null
 * for Add value; [text] is what is typed, kept when a save is refused or fails.
 */
data class ClassValueEdit(
    val renaming: String?,
    val text: String,
)

// The id of the unsaved draft the reversal screen shows the effect of; never saved.
internal const val PREVIEW_ID = "preview"

// The ids of a pair K1 checks at Review or while the reversal date is typed; never saved. Distinct, because the cash
// replay leaves the reversal's line unjudged by its id on a shared date (14a decision 5).
private val PROBE_IDS = ReentryIds("check-r", "check-rc", "check-n", "check-nc")

/** A pair K1 passed: a buy or sell with their cash lines (14a), a split, or a cash entry (14b). */
private sealed interface ReplacePair {
    data class Trade(
        val created: ReentryResult.Created,
    ) : ReplacePair

    data class Split(
        val created: SplitPairResult.Created,
    ) : ReplacePair

    data class Cash(
        val created: CashPairResult.Created,
    ) : ReplacePair
}

/** K1's answer for a pair of any kind. */
private sealed interface PairCheck {
    data class Passed(
        val pair: ReplacePair,
    ) : PairCheck

    data class Refused(
        val reason: ReentryRefusal,
    ) : PairCheck
}

/** The pair at Review (14a decision 3): it passes, opens the review with a reversal-date refusal, or stays on the entry. */
private sealed interface PairReview {
    val created: ReplacePair?

    data class Passes(
        override val created: ReplacePair,
    ) : PairReview

    data class OnReview(
        override val created: ReplacePair,
        val refusal: ReentryRefusal,
    ) : PairReview

    /** [reason] is null when a field does not parse; the field refusals say why. */
    data class OnEntry(
        val reason: ReentryRefusal?,
    ) : PairReview {
        override val created: ReplacePair? get() = null
    }
}

// The pair shown on the combined review, in the field of its kind; the others cleared.
private fun AppState.withPair(pair: ReplacePair?): AppState = copy(
    replaceReview = (pair as? ReplacePair.Trade)?.created,
    replaceSplitReview = (pair as? ReplacePair.Split)?.created,
    replaceCashReview = (pair as? ReplacePair.Cash)?.created,
)

// Leaves Reverse and re-enter: the pair's state is cleared and the entry text it filled in is not kept (14a, 14b). Only
// the form of the replaced entry's kind is cleared.
private fun AppState.leftReplace(): AppState {
    if (replacingId == null && replacingCashId == null) return this
    val kind = replacedTransaction?.type
    return copy(
        replacingId = null,
        replacingCashId = null,
        replaceReversalDate = "",
        replaceReversalNote = "",
        replaceDateEditing = false,
        replaceRefusals = ReplaceRefusals(),
        replaceStartingPicks = emptyMap(),
        replaceFilledQuantity = "",
        buyForm = if (kind == TransactionType.BUY) BuyForm() else buyForm,
        sellForm = if (kind == TransactionType.SELL) SellForm() else sellForm,
        splitForm = if (kind == TransactionType.SPLIT) SplitForm() else splitForm,
        form = if (replacingCashId != null) FormState() else form,
    ).withPair(null)
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

/** One row of Hidden holdings (view 9): the ticker and the date of its last transaction, a reversal included. */
data class HiddenRow(
    val ticker: String,
    val lastTransaction: LocalDate?,
)

/** Holding detail's tabs (view 6). Classification joins in 23 (10 decision 1). Never saved. */
enum class HoldingTab {
    TRANSACTIONS,
    LOTS,

    // Sector, geography and the split history (view 6, wireframe 06 frame C; 23a).
    CLASSIFICATION,
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

/** The default order, left to right, for screens drawn without the app's state; the app's bar follows [AppState.tabOrder] (22). */
val DEFAULT_TAB_ORDER = TabOrder.DEFAULT.tabs

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
 * The Cash list filters as typed (view 7): From and To text, one chip (a direct [type], or Buy or Sell as [trade]
 * (28); both null for All) and the ticker picked (28; null for All tickers). Held in memory only, so they last while
 * the app runs and are never stored (06 decision 4).
 */
data class CashFilterForm(
    val fromText: String = "",
    val toText: String = "",
    val type: CashType? = null,
    val ticker: String? = null,
    val trade: TradeSide? = null,
)

/**
 * The Delete dialog (wireframe 10d frames D and E; 29): K1's summary when it opened, the name as typed (a line break
 * becomes a space, as for names), and why the deletion was not saved, if it was not.
 */
data class DeleteDialog(
    val summary: DeletionSummary,
    val typedName: String = "",
    val save: SaveRefusal? = null,
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
    val transactions: List<Transaction> = emptyList(),
    val instruments: List<Instrument> = emptyList(),
    val buySellMenuOpen: Boolean = false,
    // Overall's "Allocation by" (view 1): not saved, Asset type at each load and unlock (18); its choice layer.
    val allocationBy: AllocationBy = AllocationBy.ASSET_TYPE,
    val allocationMenuOpen: Boolean = false,
    // Allocation (view 4): the portfolio its chooser shows, not saved, and the chooser's layer (19).
    val allocationPortfolioId: String? = null,
    val portfolioChoiceOpen: Boolean = false,
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
    // 15: the hidden holdings as stored; HOLDING was opened from Hidden holdings, so Back returns there (decision 3);
    // why a Hide or Unhide was not saved.
    val hidden: List<HiddenHolding> = emptyList(),
    val holdingFromHidden: Boolean = false,
    val hideRefusal: SaveRefusal? = null,
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
    // DETAIL shows a trade line opened from Transaction history: Back, and its link's transaction, return there (27).
    val detailFromHistory: Boolean = false,
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
    // 14c: a replaced buy's first quantity as filled in; the same text sends the original's exact shares (decision 2).
    val replaceFilledQuantity: String = "",
    // 14b: the cash entry being replaced (FORM), the reversal's fixed note (decision 1), and the pair K1 made at Review
    // for a cash entry or a split.
    val replacingCashId: String? = null,
    val replaceReversalNote: String = "",
    val replaceCashReview: CashPairResult.Created? = null,
    val replaceSplitReview: SplitPairResult.Created? = null,
    val reversal: ReversalForm = ReversalForm(),
    val cashFilterForm: CashFilterForm = CashFilterForm(),
    // 28: the Cash tab's Ticker menu, drawn in the window; not an input of the kept Cash list.
    val cashTickerMenuOpen: Boolean = false,
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
    // 29: the Delete dialog over the portfolio's screen (wireframe 10d frames D and E), and the blocked message in its
    // place (frame F; DeletionCheck.Blocked or CannotDelete), each drawn in the window.
    val deleteDialog: DeleteDialog? = null,
    val deleteBlocked: DeletionCheck? = null,
    // 16a: a price refresh runs (the "updating" indicator), and the last one failed (the quiet refresh-failed line).
    val refreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    // 20: the auto-lock choice as stored, null until it is loaded (the lock then keeps "every time you leave"); why a
    // choice was not saved.
    val autoLock: AutoLock? = null,
    val autoLockRefusal: SaveRefusal? = null,
    // Settings' Auto-lock choice layer (wireframe 10f frame C), drawn in the window.
    val autoLockChoiceOpen: Boolean = false,
    // 21: the screenshot window's milliseconds left, as MainActivity last passed them; null while it is off. Its layer
    // on Settings (wireframe 10f frame C, marker 8), drawn in the window.
    val screenshotWindowLeftMs: Long? = null,
    val screenshotLayerOpen: Boolean = false,
    // 22: the bottom bar's order as stored, the default until it is loaded; why a move was not saved.
    val tabOrder: TabOrder = TabOrder.DEFAULT,
    val tabOrderRefusal: SaveRefusal? = null,
    // 23a: the sector and geography lists as stored, loaded with the records and extended by each refresh's profiles.
    val classLists: ClassLists = ClassLists(),
    // 24: the holdings' overrides as stored; on CLASS_PICKER the list being edited and the value picked; why an override
    // was not saved or cleared (on the picker or the Classification tab).
    val overrides: List<ClassOverride> = emptyList(),
    val classPicker: ClassList? = null,
    val classPickerChoice: String? = null,
    val overrideRefusal: SaveRefusal? = null,
    // 25: Settings, Lists: the list shown on CLASS_LIST; its Name layer (frame G) for Add value or Rename; K10's outcome
    // of the last add, rename or delete there (a refusal, or the line confirming it); a save or delete the store failed.
    val classListShown: ClassList? = null,
    val classValueEdit: ClassValueEdit? = null,
    val listOutcome: ListResult? = null,
    val listSaveRefusal: SaveRefusal? = null,
    // 26: Realized gains (view 5): From and To as typed (digits, as the Cash list's; decision 3) and the holdings expanded,
    // as (portfolio id, ticker); kept while on Realized gains and the screens it opens (decision 2).
    val gainsFromText: String = "",
    val gainsToText: String = "",
    val gainsExpanded: Set<Pair<String, String>> = emptySet(),
    // 27: Transaction history (view 8): the portfolio shown, where "<" returns (the portfolio, or Realized gains for a
    // sale, decision 1), From and To as typed, the ticker (null: all tickers), the chip and the Ticker menu; kept while
    // on the history and the screens it opens (decision 2).
    val historyPortfolioId: String? = null,
    val historyFrom: Screen = Screen.PORTFOLIO,
    val historyFromText: String = "",
    val historyToText: String = "",
    val historyTicker: String? = null,
    val historyChip: HistoryChip = HistoryChip.ALL,
    val historyTickerMenuOpen: Boolean = false,
    // 15c: the kept fields, worked out in KeptFields.kt from the fields above, so reading one costs nothing. Every write
    // through the view model refreshes them; a state built directly gets them from these defaults.
    // As of today (4.8), by portfolio, then ticker.
    val holdings: List<Holding> = KeptFields.holdings(transactions, today),
    // Cash as of today plus every holding at its latest price, or at cost without one, as of today (4.8; 16a).
    val netWorth: Fraction = KeptFields.netWorth(entries, holdings, instruments, today),
    // The shown holding's lots with shares as of today, oldest first (view 6, 4.4).
    val holdingLots: List<Lot> = KeptFields.holdingLots(transactions, portfolioId, holdingTicker, today),
    // Hide on the shown holding (15 decision 1).
    val hideOffered: Boolean = KeptFields.hideOffered(transactions, hidden, portfolioId, holdingTicker),
    // The tickers the sell's portfolio holds (11 decision 1; 14a while a sell is replaced).
    val sellTickers: List<String> =
        KeptFields.sellTickers(entries, transactions, holdings, hidden, sellForm.portfolioId, replacingId, replaceReversalDate, today),
    // The sell's lot list (4.4) for its date, or for today while the date is not a whole date (10c marker 9).
    val sellLotChoices: List<LotChoice> =
        KeptFields.sellLotChoices(
            entries,
            transactions,
            sellForm.portfolioId,
            sellForm.ticker,
            sellForm.dateText,
            replacingId,
            replaceReversalDate,
            today,
        ),
    // What is left of the shown buy's lot as of today (13a decision 2).
    val transactionLot: Lot? = KeptFields.transactionLot(transactions, transactionId, today),
    // The Cash list as the screen draws it (view 7; 15d).
    val cashList: CashListModel = KeptFields.cashList(entries, transactions, portfolios, cashFilterForm),
    // 25 (Deploy finding 1): every holding as (portfolio id, ticker) as of the latest transaction date or today, whichever
    // is later, hidden and sold-out ones included, for K10's counts on the list screen.
    val listHoldings: List<Pair<String, String>> = KeptFields.listHoldings(transactions, today),
    // 26: K3's realized gains for the From and To typed, each end empty or not a date counting as open (decision 3).
    val realizedGains: RealizedReport = KeptFields.realizedGains(transactions, portfolios, gainsFromText, gainsToText),
    // The inputs the kept fields above were worked out from.
    val keptFrom: KeptInputs =
        KeptInputs(
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
            portfolios = portfolios,
            cashFilterForm = cashFilterForm,
            instruments = instruments,
            gainsFromText = gainsFromText,
            gainsToText = gainsToText,
        ),
) {
    val shownPortfolio: Portfolio? get() = portfolioId?.let { id -> portfolios.firstOrNull { it.id == id } }

    /** Net worth rounded to cents, for Overall (4.10: rounded only when shown). */
    val netWorthCents: Long get() = netWorth.rounded(2).movePointRight(2).longValueExact()

    /** A portfolio's holdings list (view 3): a hidden holding is left out of it and of its count (4.4). */
    fun holdingsOf(portfolioId: String): List<Holding> = holdings.filter {
        it.portfolioId == portfolioId && HiddenHolding(portfolioId, it.ticker) !in hidden
    }

    /** The shown holding is hidden: Holding detail then offers neither Hide nor Unhide (view 9, marker 2). */
    val shownHoldingHidden: Boolean
        get() {
            val id = portfolioId ?: return false
            val ticker = holdingTicker ?: return false
            return HiddenHolding(id, ticker) in hidden
        }

    /** A portfolio's Hidden holdings rows, in ticker order (view 9, marker 2). */
    fun hiddenRows(portfolioId: String): List<HiddenRow> = hidden
        .filter { it.portfolioId == portfolioId }
        .sortedBy { it.ticker }
        .map { HiddenRow(it.ticker, Hiding.lastTransactionDate(transactions, portfolioId, it.ticker)) }

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

    /** Its Transactions tab's rows, as the screen draws them (view 6; CR2, 15a decision 1). */
    val holdingRows: List<HoldingRowModel>
        get() {
            val id = portfolioId ?: return emptyList()
            val ticker = holdingTicker ?: return emptyList()
            return HoldingRows.of(transactions, id, ticker)
        }

    /** The shown holding's latest price, or null when it never had one (4.8; 16a). */
    val shownPrice: Price?
        get() = instruments.firstOrNull { it.ticker == holdingTicker }?.price

    /** Holding detail's header as the screen draws it (view 6; CR2, 16a step 6); null when no holding is shown. */
    val holdingHeader: HoldingHeaderModel?
        get() {
            val holding = shownHolding ?: return null
            return HoldingDetail.header(holding, shownPortfolio?.name.orEmpty(), shownPrice, refreshing, refreshFailed)
        }

    /** Its Lots tab's rows, with each lot's gain at the latest price (view 6; CR2, 16a step 6). */
    val holdingLotRows: List<LotRowModel>
        get() = HoldingDetail.lots(holdingLots, shownPrice)

    /** Its Classification tab (view 6, frame C; CR2, 23a; the overrides from 24); null when no holding is shown. */
    val holdingClassification: ClassificationModel?
        get() {
            val id = portfolioId ?: return null
            val ticker = holdingTicker ?: return null
            return HoldingDetail.classification(holdingClasses, transactions, id, ticker, notSaved = overrideRefusal != null)
        }

    /** The Edit picker as its screen draws it (wireframe 10f frame H; CR2, 24); null off CLASS_PICKER. */
    val classPickerModel: ClassPickerModel?
        get() {
            val list = classPicker ?: return null
            val ticker = holdingTicker ?: return null
            return ClassPicker.of(list, ticker, classLists[list], classPickerChoice, classValueShown(list), saving, overrideRefusal)
        }

    /** The value the open holding's [list] row shows, as the list spells it; none when Unclassified or not on the list. */
    internal fun classValueShown(list: ClassList): String? {
        val portfolio = portfolioId ?: return null
        val ticker = holdingTicker ?: return null
        val shown = holdingClasses.valueFor(list, portfolio, ticker) ?: return null
        return Classification.overrideValue(classLists[list], shown)
    }

    /** A portfolio's value, cost and gain/loss as the Portfolios list and its screen draw them (view 3; CR2, 16a). */
    fun portfolioFigures(portfolioId: String): PortfolioFiguresModel = PortfolioFigures.of(holdingsOf(portfolioId), KeptFields.prices(instruments))

    /** The price refresh as the Portfolios list and a portfolio's screen draw it (16b), Overall and the sell screen (16c). */
    val priceRefresh: PriceRefreshModel
        get() = PriceRefreshModel(refreshing, refreshFailed)

    /** Overall as the screen draws it (view 1; CR2, 16c). */
    val overall: OverallModel
        get() = Overall.of(netWorth, holdings, portfolios, ::holdingsOf, KeptFields.prices(instruments), priceRefresh)

    /** Net Worth (Total) as the screen draws it (view 2; CR2, 17): cash as of today and each portfolio's value (4.8). */
    val netWorthTotal: NetWorthModel
        get() =
            NetWorthTotal.of(CashRules.balanceOn(entries, today), holdings, portfolios, ::holdingsOf, KeptFields.prices(instruments), priceRefresh)

    /**
     * A breakdown of net worth (view 1, view 2; CR2, 18): cash as of today and each holding at its price or at cost, null
     * while net worth shows as $0.00 (4.8; 17 decision 4). A ticker's asset type is its instrument's.
     */
    fun allocation(by: AllocationBy): AllocationModel? = Allocations.of(
        by,
        CashRules.balanceOn(entries, today),
        holdings,
        portfolios,
        KeptFields.prices(instruments),
        instruments.associate { it.ticker to it.assetType },
        holdingClasses,
    )

    /** Each holding's effective sector and geography from its instrument's profile through the lists (4.9, K10; 23b). */
    internal val holdingClasses: HoldingClasses
        get() = HoldingClasses(classLists, profiles, overrides)

    /**
     * What K10 needs to count a value's holdings (4.9; 25): every holding as of the latest transaction date or today,
     * whichever is later (a buy dated after today is a holding too), hidden ones included, whatever its shares, kept in
     * [listHoldings] (Deploy finding 1); each instrument's profile; every override.
     */
    internal val listUsers: ListUsers
        get() = ListUsers(listHoldings, profiles, overrides)

    // Each instrument's source profile by ticker (23a), for the effective values and the counts.
    private val profiles: Map<String, SourceProfile>
        get() = instruments.mapNotNull { instrument -> instrument.profile?.let { instrument.ticker to it } }.toMap()

    /** Allocation as the screen draws it (view 4; CR2, 19): the chooser's portfolio, its value and its breakdowns. */
    val portfolioAllocation: PortfolioAllocationModel
        get() =
            PortfolioAllocation.of(
                portfolios,
                allocationPortfolioId,
                ::holdingsOf,
                KeptFields.prices(instruments),
                instruments.associate { it.ticker to it.assetType },
                priceRefresh,
                holdingClasses,
            )

    /** More as the screen draws it (section 6; CR2, 17). */
    val more: MoreModel
        get() = More.of()

    /** Settings as the screen draws it (section 6; CR2, 20). */
    val settings: SettingsModel
        get() = Settings.of(autoLock, autoLockRefusal, screenshotWindowLeftMs, tabOrder, classLists)

    /** The list screen as it draws it (wireframe 10f frames F and G; CR2, 25); null off CLASS_LIST. */
    val classListScreen: ClassListModel?
        get() {
            val list = classListShown ?: return null
            return ClassListScreens.of(list, classLists, listUsers, classValueEdit, listOutcome, listSaveRefusal, saving)
        }

    /** 27: the history's portfolio's tickers, hidden holdings included (view 8). */
    val historyTickers: List<String>
        get() = historyPortfolioId?.let { TransactionHistory.tickers(transactions, it) }.orEmpty()

    /** 27: the history's rows for the filters as typed; an end that is not a whole date counts as open (26 decision 3). */
    val historyRows: List<HistoryRow>
        get() =
            historyPortfolioId
                ?.let { id ->
                    val filter = HistoryFilter(historyTicker, DateDigits.toDate(historyFromText), DateDigits.toDate(historyToText), historyChip)
                    TransactionHistory.rows(transactions, entries, id, filter)
                }.orEmpty()

    /** Transaction history as the screen draws it (view 8, wireframe 08; CR2, 27). */
    val transactionHistoryScreen: TransactionHistoryModel
        get() =
            TransactionHistories.of(
                rows = historyRows,
                portfolioName = portfolios.firstOrNull { it.id == historyPortfolioId }?.name.orEmpty(),
                fromText = historyFromText,
                toText = historyToText,
                ticker = historyTicker,
                tickers = historyTickers,
                chip = historyChip,
                menuOpen = historyTickerMenuOpen,
            )

    /** Realized gains as the screen draws it (view 5, wireframe 05; CR2, 26), from the kept report. */
    val realizedGainsScreen: RealizedGainsModel
        get() = RealizedGainsScreens.of(realizedGains, portfolios, gainsFromText, gainsToText, gainsExpanded)

    // 22: the Tab order screen (wireframe 10f frame E), from the order in force.
    val tabOrderScreen: TabOrderModel
        get() = TabOrders.of(tabOrder, tabOrderRefusal)

    /** The sell screen's lot rows, each lot's gain at the latest price in today's shares (16c decisions 3 and 4). */
    val sellLotRows: List<SellLotRowModel>
        get() {
            val toToday =
                KeptFields.sellUnitsToToday(
                    entries,
                    transactions,
                    sellForm.portfolioId,
                    sellForm.ticker,
                    sellForm.dateText,
                    replacingId,
                    replaceReversalDate,
                    today,
                )
            return SellLots.rows(sellLotChoices, toToday, instruments.firstOrNull { it.ticker == sellForm.ticker }?.price)
        }

    /** A portfolio's holding rows as its screen draws them (view 3; CR2, 16a). */
    fun portfolioHoldingRows(portfolioId: String): List<PortfolioHoldingRowModel> = PortfolioFigures.rows(holdingsOf(portfolioId), KeptFields.prices(instruments))

    /** The derived price per share (4.3), shown as the fields change; null until quantity and total are both valid. */
    val buyPricePerShare: Fraction?
        get() {
            val quantity = Quantity.parse(buyForm.quantityText) as? QuantityResult.Exact ?: return null
            val total = Money.parseCents(buyForm.totalText) as? AmountResult.Cents ?: return null
            return Transactions.pricePerShare(total.value, quantity.value)
        }

    /** The tickers [portfolioId] holds with shares as of today, in ticker order. */
    fun heldTickers(portfolioId: String): List<String> = KeptFields.heldTickers(holdings, hidden, portfolioId)

    /** The cash entry Reverse and re-enter replaces (14b), for the banner and the "Original: …" hints. */
    val replacedCashEntry: CashEntry? get() = replacingCashId?.let { id -> entries.firstOrNull { it.id == id } }

    /** Reverse and re-enter on a direct cash entry not yet reversed, not a reversal (4.6; 14b). */
    val detailReenterOffered: Boolean get() = detailEntry?.let { Reentries.cashOffered(entries, it) } ?: false

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

    /** The new split's shares at its place, before and after, on the combined review (14b; 10c marker 11). */
    val replaceSplitEffect: SplitEffect?
        get() = replaceSplitReview?.let { Splits.effect(transactions + it.reversal + it.replacement, it.replacement) }

    /** The sum of both parts' cash effects (10c marker 7); none for a split. */
    val replaceCombinedCents: Long?
        get() = replaceReview?.let { it.reversalCashLine.amountCents + it.replacementCashLine.amountCents }
            ?: replaceCashReview?.let { it.reversal.amountCents + it.replacement.amountCents }

    /** The combined review as the screen draws it (4.6, wireframe 10c; CR2, 15a decision 1); null with nothing replaced. */
    val replaceReviewModel: ReplaceReviewModel?
        get() = ReplaceReview.of(
            original = replacedTransaction,
            originalCash = replacedCashEntry,
            reversalNote = replaceReversalNote,
            reversalEffect = replaceReversalEffect,
            trade = replaceReview,
            pieces = replacePieces,
            split = replaceSplitReview,
            splitEffect = replaceSplitEffect,
            cash = replaceCashReview,
            combinedCents = replaceCombinedCents,
            portfolios = portfolios,
        )

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
            ticker = cashFilterForm.ticker,
            trade = cashFilterForm.trade,
        )

    /** The Cash tab's Ticker menu (28): each ticker with a trade line, from every portfolio, alphabetically (view 7). */
    val cashTickers: List<String> get() = CashAccount.tickers(entries, transactions)

    /** A valid date bound, a chip other than All or a ticker (06 decision 3: the count line and Clear filters show then). */
    val filterActive: Boolean get() = cashFilter.isActive

    /** The rows that match the filters, each with its real balance (view 7). */
    val cashFilteredRows: List<CashRow> get() = CashAccount.rows(entries, transactions, cashFilter)

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
    private val mutableState = KeptStateFlow(AppState())
    val state: StateFlow<AppState> = mutableState.readOnly
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
                val entries = opened.loadCashEntries()
                val portfolios = opened.loadPortfolios()
                val transactions = opened.loadTransactions()
                val instruments = opened.loadInstruments()
                // The hidden holdings are checked against the transactions just read, not a second read (15c).
                val hidden = opened.loadHidden(transactions)
                // 20: the auto-lock choice comes with the records; until then the lock keeps its strictest rule.
                val autoLock = opened.loadAutoLock()
                // 22: the tab order too; until then the bar is not shown (LOADING).
                val tabOrder = opened.loadTabOrder()
                // 23a: the sector and geography lists, for the Classification tab; 24: the holdings' overrides.
                val classLists = opened.loadClassLists()
                val overrides = opened.loadOverrides()
                showLoaded(entries, portfolios, transactions, instruments, hidden, autoLock, tabOrder, classLists, overrides)
                // The first unlock's refresh: onUnlocked came while the records were still loading (16a decision 2).
                refreshPrices()
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
                // 29: the Delete dialog and the blocked message end with the portfolio's screen.
                deleteDialog = null,
                deleteBlocked = null,
                buySellMenuOpen = false,
                allocationMenuOpen = false,
                portfolioChoiceOpen = false,
                autoLockChoiceOpen = false,
                screenshotLayerOpen = false,
                // 28: the Cash tab's Ticker menu closes; its filters stay (06 decision 4).
                cashTickerMenuOpen = false,
                autoLockRefusal = null,
                tabOrderRefusal = null,
                buyNotice = null,
                buyRefusals = BuyRefusals(),
                holdingTicker = null,
                holdingFromHidden = false,
                hideRefusal = null,
                // 24's Deploy review: the picker and a failed save or clear end with the holding.
                classPicker = null,
                classPickerChoice = null,
                overrideRefusal = null,
                // 25: the list screen, its Name layer and its lines end with it.
                classListShown = null,
                classValueEdit = null,
                listOutcome = null,
                listSaveRefusal = null,
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
            ).leftReplace().leftGains().leftHistory()
        }
    }

    /** Pull-to-refresh (spec 7; 16a decision 2). */
    fun onPullToRefresh() = refreshPrices()

    /** The phone has a network again: a refresh that failed is run again (16a decision 2); otherwise nothing happens. */
    fun onOnline() {
        if (mutableState.value.refreshFailed) refreshPrices()
    }

    /**
     * Refreshes the prices (K4; spec 7, 4.8): asks for every ticker held except hidden ones (16a decision 3), saves the
     * fresh prices and marks the rest stale, and every figure follows through the kept fields. Nothing happens before
     * the records are loaded, on the unreadable-data screen, or while a refresh runs.
     */
    private fun refreshPrices() {
        val current = mutableState.value
        val opened = store ?: return
        if (current.screen == Screen.LOADING || current.screen == Screen.UNREADABLE || current.refreshing) return
        val tickers = PriceRefresh.tickers(current.transactions, current.hidden)
        // 23a: the profile is asked once per instrument (decision 3); an instrument not stored yet is not asked.
        val unasked = current.instruments.filter { it.profile == null }.map { it.ticker }.toSet()
        // 25 decision 4: every held ticker, hidden ones included, so the store adds their texts that map to no value.
        val held = current.listUsers.holdings.map { it.second }.distinct()
        mutableState.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            try {
                val outcome = PriceRefresh.refresh(tickers, isOnline(), priceSource, opened, unasked, held)
                mutableState.update {
                    // 23a: the lists gain what the store added: the texts of the profiles of stored instruments; 25: then
                    // the stored profiles of the held tickers not among them, as the store's profilesToAdd.
                    val saved = outcome.profiles.filterKeys { ticker -> it.instruments.any { i -> i.ticker == ticker } }
                    val again =
                        held.filter { ticker -> ticker !in saved }.mapNotNull { ticker ->
                            it.instruments.firstOrNull { i -> i.ticker == ticker }?.profile
                        }
                    it.copy(
                        instruments = PriceRefresh.applied(it.instruments, outcome),
                        classLists = Classification.extended(it.classLists, saved.values + again),
                        refreshing = false,
                        refreshFailed = outcome.refreshFailed,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, refreshing = false) }
            } catch (e: Exception) {
                // The prices could not be saved: they stay as they were, and the refresh counts as failed.
                mutableState.update { it.copy(refreshing = false, refreshFailed = true) }
            }
        }
    }

    /**
     * After unlock the app shows Overall, unless an entry screen or the New portfolio dialog is open, which stays
     * (02b decision 9). The Buy/Sell menu and its notice close. Prices refresh on every unlock (16a decision 2).
     */
    fun onUnlocked() {
        refreshPrices()
        // View 1: "Allocation by" is not saved; each unlock starts on Asset type (18).
        // The choice layers close, as after 18; Settings' layers too (20, 21).
        mutableState.update {
            it.copy(
                allocationBy = AllocationBy.ASSET_TYPE,
                allocationMenuOpen = false,
                portfolioChoiceOpen = false,
                autoLockChoiceOpen = false,
                screenshotLayerOpen = false,
            )
        }
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.portfolioDialog != null) return
        // 28: the Cash tab's Ticker menu closes with the tab; its filters stay.
        mutableState.update { it.copy(tab = Tab.OVERALL, buySellMenuOpen = false, buyNotice = null, cashTickerMenuOpen = false) }
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

    /**
     * Back from the form to the tab it was opened from; the typed text is kept and the refusals are cleared. While a cash
     * entry is replaced, back to its detail, nothing kept (14b). Ignored outside FORM.
     */
    fun closeForm() {
        val current = mutableState.value
        if (current.screen != Screen.FORM) return
        if (current.replacingCashId != null) {
            mutableState.update { it.copy(screen = Screen.DETAIL, refusals = Refusals()).leftReplace() }
            return
        }
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
        if (current.replacingCashId != null) {
            reviewReplacedCash(current, Refusals(amount = amountError, date = dateRefusal))
            return
        }
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

    /**
     * Review of a replacing cash entry (14b): the field rules and the note rules (judged on their own, with stand-ins),
     * then K1's pair rules; the new entry's cash rules show as on the plain form, the reversal's in replaceRefusals, and
     * only a reversal-date refusal opens the combined review (14a decision 3).
     */
    private fun reviewReplacedCash(
        current: AppState,
        fieldRefusals: Refusals,
    ) {
        val noteRule = cashNoteRule(current)
        val review = pairAtReview(current)
        val entryRule = (review as? PairReview.OnEntry)?.reason
        val refusals =
            fieldRefusals.copy(
                noteHasLineBreak = noteRule == CashEntryRefusal.NoteHasLineBreak,
                noteRequired = noteRule == CashEntryRefusal.NoteRequired,
                cash = (entryRule as? ReentryRefusal.OfCash)?.reason,
            )
        if (refusals != Refusals() || review is PairReview.OnEntry) {
            val reversalRule = entryRule as? ReentryRefusal.OfCashReversal
            mutableState.update { it.copy(refusals = refusals, replaceRefusals = ReplaceRefusals(rule = reversalRule)) }
            return
        }
        mutableState.update {
            it.copy(
                screen = Screen.REPLACE_REVIEW,
                refusals = Refusals(),
                replaceRefusals = ReplaceRefusals(rule = (review as? PairReview.OnReview)?.refusal),
                replaceDateEditing = DateDigits.toDate(it.replaceReversalDate) == null,
            ).withPair(review.created)
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

    /**
     * One chip at a time: null for All, or one of the four direct types; Buy or Sell is cleared (28). Other types are
     * ignored, and so is a direct type while a ticker is picked: those chips are disabled then (view 7).
     */
    fun onFilterTypeChange(type: CashType?) {
        if (type != null && (type !in ENTRY_TYPES || mutableState.value.cashFilterForm.ticker != null)) return
        mutableState.update { it.copy(cashFilterForm = it.cashFilterForm.copy(type = type, trade = null)) }
    }

    /** Clear filters: both dates empty and All (view 7). */
    fun clearFilters() = mutableState.update { it.copy(cashFilterForm = CashFilterForm()) }

    /** Buy or Sell (28): one chip at a time, so the direct type is cleared; enabled with or without a ticker. */
    fun onFilterTradeChange(side: TradeSide) {
        mutableState.update { it.copy(cashFilterForm = it.cashFilterForm.copy(type = null, trade = side)) }
    }

    /** The Cash tab's Ticker field (28; 27 decision 3): its menu, drawn in the window, once some entry exists. */
    fun openCashTickerMenu() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.tab != Tab.CASH || !current.cashList.hasEntries) return
        mutableState.update { it.copy(cashTickerMenuOpen = true) }
    }

    /** Back or a tap outside the menu: closes it without a change. */
    fun closeCashTickerMenu() = mutableState.update { it.copy(cashTickerMenuOpen = false) }

    /**
     * A ticker in the menu, or null for "All tickers"; closes the menu. With a ticker a direct chip becomes All, since
     * those entries have no ticker (view 7). A ticker not offered, or a pick with the menu closed, is ignored.
     */
    fun pickCashTicker(ticker: String?) {
        val current = mutableState.value
        if (!current.cashTickerMenuOpen) return
        if (ticker != null && ticker !in current.cashTickers) return
        mutableState.update {
            val form = it.cashFilterForm
            val type = if (ticker != null) null else form.type
            it.copy(cashFilterForm = form.copy(ticker = ticker, type = type), cashTickerMenuOpen = false)
        }
    }

    /**
     * New portfolio or Create a portfolio: opens an empty dialog over the selected tab, or, before the opening
     * balance exists, asks for it instead (4.2, wireframe 10d). Also over Allocation with no portfolio (wireframe 04
     * marker 4; 19). Ignored elsewhere and while a save is running.
     */
    fun openPortfolioDialog() {
        val current = mutableState.value
        if (current.screen !in setOf(Screen.TABS, Screen.ALLOCATION) || current.saving) return
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
                    it.copy(
                        portfolios = it.portfolios + portfolio,
                        portfolioDialog = null,
                        saving = false,
                        // From Allocation (19): the screen shows the new portfolio (10d: back to where it was opened).
                        allocationPortfolioId = if (it.screen == Screen.ALLOCATION) portfolio.id else it.allocationPortfolioId,
                    )
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

    /**
     * Net Worth (Total) (view 2): from Overall's net worth (navigation outline) or More's entry (17 decision 1). The tab
     * stays, so Back returns to it. Ignored elsewhere and while a layer is open.
     */
    fun openNetWorth() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.tab !in setOf(Tab.OVERALL, Tab.MORE)) return
        if (current.buySellMenuOpen || current.buyNotice != null || current.portfolioDialog != null || current.allocationMenuOpen) return
        mutableState.update { it.copy(screen = Screen.NET_WORTH) }
    }

    /** "Allocation by" (view 1): opens its choice layer, drawn in the window; on Overall only, not over another layer. */
    fun openAllocationMenu() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.tab != Tab.OVERALL) return
        if (current.buySellMenuOpen || current.buyNotice != null || current.portfolioDialog != null) return
        mutableState.update { it.copy(allocationMenuOpen = true) }
    }

    /** Back or a tap outside the layer: closes it and keeps the choice. */
    fun closeAllocationMenu() = mutableState.update { it.copy(allocationMenuOpen = false) }

    /** One of the options: shown until the next unlock, not saved (view 1); closes the layer. */
    fun chooseAllocation(by: AllocationBy) = mutableState.update { it.copy(allocationBy = by, allocationMenuOpen = false) }

    /**
     * Allocation (view 4): from a portfolio's screen ("Allocation >") on that portfolio, keeping it so Back returns there;
     * from More (19 decision 1) on the first portfolio. Ignored elsewhere and while a layer is open.
     */
    fun openAllocation() {
        val current = mutableState.value
        if (current.saving || current.portfolioMenuOpen) return
        when {
            current.screen == Screen.PORTFOLIO ->
                mutableState.update { it.copy(screen = Screen.ALLOCATION, allocationPortfolioId = it.portfolioId, portfolioChoiceOpen = false) }

            current.screen == Screen.TABS && current.tab == Tab.MORE ->
                mutableState.update {
                    it.copy(screen = Screen.ALLOCATION, allocationPortfolioId = it.portfolios.firstOrNull()?.id, portfolioChoiceOpen = false)
                }
        }
    }

    /** "<" or Android Back: back to the portfolio it was opened from, or to More. Ignored while a save is running. */
    fun closeAllocation() {
        val current = mutableState.value
        if (current.screen != Screen.ALLOCATION || current.saving) return
        mutableState.update {
            it.copy(
                screen = if (it.portfolioId != null) Screen.PORTFOLIO else Screen.TABS,
                portfolioChoiceOpen = false,
                portfolioNeedsOpeningBalance = false,
            )
        }
    }

    /** The chooser (view 4, marker 1; decision 2): opens its layer, drawn in the window; on Allocation only, with portfolios. */
    fun openPortfolioChoice() {
        val current = mutableState.value
        if (current.screen != Screen.ALLOCATION || current.portfolios.isEmpty() || current.portfolioDialog != null) return
        mutableState.update { it.copy(portfolioChoiceOpen = true) }
    }

    /** Back or a tap outside the layer: closes it and keeps the shown portfolio. */
    fun closePortfolioChoice() = mutableState.update { it.copy(portfolioChoiceOpen = false) }

    /** A portfolio in the chooser: shown until Allocation is left, not saved (view 4); closes the layer. Unknown ids are ignored. */
    fun choosePortfolio(id: String) {
        if (mutableState.value.portfolios.none { it.id == id }) return
        mutableState.update { it.copy(allocationPortfolioId = id, portfolioChoiceOpen = false) }
    }

    /** More's Realized gains (view 5; 26): opens it on all dates, nothing expanded. On More only, not over a layer. */
    fun openRealizedGains() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.tab != Tab.MORE || current.saving) return
        mutableState.update { it.copy(screen = Screen.REALIZED_GAINS).leftGains() }
    }

    /** "<" or Android Back: back to More; the dates and the expanded holdings end with the screen (decision 2). */
    fun closeRealizedGains() {
        if (mutableState.value.screen != Screen.REALIZED_GAINS) return
        mutableState.update { it.copy(screen = Screen.TABS).leftGains() }
    }

    /** From, as the Cash list's: a ninth digit is not accepted; other characters are kept, so their line shows at once. */
    fun onGainsFromChange(text: String) {
        if (mutableState.value.screen != Screen.REALIZED_GAINS || tooManyDigits(text)) return
        mutableState.update { it.copy(gainsFromText = text) }
    }

    fun onGainsToChange(text: String) {
        if (mutableState.value.screen != Screen.REALIZED_GAINS || tooManyDigits(text)) return
        mutableState.update { it.copy(gainsToText = text) }
    }

    /** Clear dates (view 5): both empty, so all dates; the expanded holdings stay. */
    fun clearGainsDates() {
        if (mutableState.value.screen != Screen.REALIZED_GAINS) return
        mutableState.update { it.copy(gainsFromText = "", gainsToText = "") }
    }

    /** A holding row: expands or collapses its sales (view 5). Ignored for a holding not listed. */
    fun toggleGainsHolding(
        portfolioId: String,
        ticker: String,
    ) {
        val current = mutableState.value
        if (current.screen != Screen.REALIZED_GAINS) return
        if (current.realizedGains.holdings.none { it.portfolioId == portfolioId && it.ticker == ticker }) return
        val key = portfolioId to ticker
        mutableState.update { it.copy(gainsExpanded = if (key in it.gainsExpanded) it.gainsExpanded - key else it.gainsExpanded + key) }
    }

    // Realized gains' dates and expanded holdings dropped (decision 2).
    private fun AppState.leftGains(): AppState = copy(gainsFromText = "", gainsToText = "", gainsExpanded = emptySet())

    /** A portfolio's Transaction history (view 8; 27), from its screen: all dates, all tickers, All. Not over a layer. */
    fun openTransactionHistory() {
        val current = mutableState.value
        if (current.screen != Screen.PORTFOLIO || current.saving || current.portfolioMenuOpen || current.renameDialog != null) return
        val id = current.portfolioId ?: return
        mutableState.update { it.leftHistory().copy(screen = Screen.TRANSACTION_HISTORY, historyPortfolioId = id, historyFrom = Screen.PORTFOLIO) }
    }

    /** 27 decision 1: a sale on Realized gains opens its portfolio's history with its ticker and the Sell chip. */
    fun openGainsSale(sellId: String) {
        val current = mutableState.value
        if (current.screen != Screen.REALIZED_GAINS || current.saving) return
        val sell = current.transactions.firstOrNull { it.id == sellId && it.type == TransactionType.SELL } ?: return
        if (current.portfolios.none { it.id == sell.portfolioId }) return
        mutableState.update {
            it.leftHistory().copy(
                screen = Screen.TRANSACTION_HISTORY,
                historyPortfolioId = sell.portfolioId,
                historyFrom = Screen.REALIZED_GAINS,
                historyTicker = sell.ticker,
                historyChip = HistoryChip.SELL,
            )
        }
    }

    /** "<" or Android Back: the Ticker menu closes first; then back where it was opened from, the filters ended (decision 2). */
    fun closeTransactionHistory() {
        val current = mutableState.value
        if (current.screen != Screen.TRANSACTION_HISTORY) return
        if (current.historyTickerMenuOpen) {
            mutableState.update { it.copy(historyTickerMenuOpen = false) }
            return
        }
        mutableState.update { it.copy(screen = it.historyFrom).leftHistory() }
    }

    /** From and To, as the Cash list's: a ninth digit is not accepted; other characters are kept, so their line shows. */
    fun onHistoryFromChange(text: String) {
        if (mutableState.value.screen != Screen.TRANSACTION_HISTORY || tooManyDigits(text)) return
        mutableState.update { it.copy(historyFromText = text) }
    }

    fun onHistoryToChange(text: String) {
        if (mutableState.value.screen != Screen.TRANSACTION_HISTORY || tooManyDigits(text)) return
        mutableState.update { it.copy(historyToText = text) }
    }

    /** One chip at a time (view 8); none is disabled. */
    fun selectHistoryChip(chip: HistoryChip) {
        if (mutableState.value.screen != Screen.TRANSACTION_HISTORY) return
        mutableState.update { it.copy(historyChip = chip, historyTickerMenuOpen = false) }
    }

    /** The Ticker field (decision 3): its menu, drawn in the window. */
    fun openHistoryTickerMenu() {
        if (mutableState.value.screen != Screen.TRANSACTION_HISTORY) return
        mutableState.update { it.copy(historyTickerMenuOpen = true) }
    }

    fun closeHistoryTickerMenu() = mutableState.update { it.copy(historyTickerMenuOpen = false) }

    /** A ticker in the menu, or null for "All tickers"; closes the menu. A ticker not offered is ignored. */
    fun pickHistoryTicker(ticker: String?) {
        val current = mutableState.value
        if (current.screen != Screen.TRANSACTION_HISTORY || !current.historyTickerMenuOpen) return
        if (ticker != null && ticker !in current.historyTickers) return
        mutableState.update { it.copy(historyTicker = ticker, historyTickerMenuOpen = false) }
    }

    /** A cash-effect line on the history (view 8): that trade's line (frame H), whose Back returns to the history. */
    fun openHistoryCashLine(transactionId: String) {
        val current = mutableState.value
        if (current.screen != Screen.TRANSACTION_HISTORY || current.historyTickerMenuOpen) return
        val transaction = current.transactions.firstOrNull { it.id == transactionId } ?: return
        if (transaction.portfolioId != current.historyPortfolioId) return
        val line = current.entries.firstOrNull { it.type == CashType.TRADE && it.transactionId == transactionId } ?: return
        mutableState.update { it.copy(screen = Screen.DETAIL, detailId = line.id, detailFromHistory = true, refusals = Refusals()) }
    }

    // Transaction history's portfolio, filters and menu dropped (27 decision 2).
    private fun AppState.leftHistory(): AppState = copy(
        historyPortfolioId = null,
        historyFrom = Screen.PORTFOLIO,
        historyFromText = "",
        historyToText = "",
        historyTicker = null,
        historyChip = HistoryChip.ALL,
        historyTickerMenuOpen = false,
        detailFromHistory = false,
    )

    /** "<" or Android Back: back to the tab it was opened from. */
    fun closeNetWorth() {
        if (mutableState.value.screen != Screen.NET_WORTH) return
        mutableState.update { it.copy(screen = Screen.TABS) }
    }

    /** "<" or Android Back: back to the list. Ignored while a save is running. */
    fun closePortfolio() {
        val current = mutableState.value
        if (current.screen != Screen.PORTFOLIO || current.saving) return
        mutableState.update {
            it.copy(
                screen = Screen.TABS,
                portfolioId = null,
                portfolioMenuOpen = false,
                renameDialog = null,
                deleteDialog = null,
                deleteBlocked = null,
            )
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

    // K1's deletion check of the shown portfolio on the records as they are now (4.7).
    private fun AppState.toDelete(id: String) = PortfolioDeletion.check(portfolios, transactions, entries, hidden, overrides, id)

    /**
     * Delete in the menu (wireframe 10d, marker 9): closes the menu and runs K1's check. Allowed opens the dialog with
     * its summary; Blocked or CannotDelete shows the message instead (frame F). Ignored while a save is running.
     */
    fun openDelete() {
        val current = mutableState.value
        if (current.screen != Screen.PORTFOLIO || current.saving) return
        val id = current.portfolioId ?: return
        when (val check = current.toDelete(id)) {
            is DeletionCheck.Allowed ->
                mutableState.update { it.copy(portfolioMenuOpen = false, deleteDialog = DeleteDialog(check.summary)) }

            is DeletionCheck.Blocked, is DeletionCheck.CannotDelete ->
                mutableState.update { it.copy(portfolioMenuOpen = false, deleteBlocked = check) }

            // Neither is reachable from a shown portfolio's records; the menu just closes.
            DeletionCheck.NotFound, DeletionCheck.TooLarge -> mutableState.update { it.copy(portfolioMenuOpen = false) }
        }
    }

    /** Close on the blocked message (frame F): nothing changes. */
    fun closeDeleteBlocked() = mutableState.update { it.copy(deleteBlocked = null) }

    /** The typed name (marker 11): a line break becomes a space, as for names; typing clears a failed save. */
    fun onDeleteNameChange(text: String) = mutableState.update { state ->
        state.copy(deleteDialog = state.deleteDialog?.copy(typedName = oneLine(text), save = null))
    }

    /** Cancel or Back: closes the dialog; nothing is removed. Ignored while a save is running. */
    fun cancelDelete() {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(deleteDialog = null) }
    }

    /**
     * Delete portfolio (frame E): only once the typed name matches (case and spaces ignored). K1 checks again; if the
     * records changed so that it is now blocked, the message replaces the dialog and nothing is removed. Otherwise K2
     * removes the portfolio and what is its own as one action, the state drops the same records, and the Portfolios tab
     * shows. A failed removal keeps the dialog with its line and changes nothing.
     */
    fun confirmDelete() {
        val current = mutableState.value
        val dialog = current.deleteDialog ?: return
        val id = current.portfolioId ?: return
        if (current.saving || !PortfolioDeletion.nameMatches(dialog.summary.portfolio, dialog.typedName)) return
        val target = store ?: return
        val removal =
            when (val check = current.toDelete(id)) {
                is DeletionCheck.Allowed -> check.removal

                is DeletionCheck.Blocked, is DeletionCheck.CannotDelete -> {
                    mutableState.update { it.copy(deleteDialog = null, deleteBlocked = check) }
                    return
                }

                DeletionCheck.NotFound, DeletionCheck.TooLarge -> {
                    mutableState.update { it.copy(deleteDialog = null) }
                    return
                }
            }
        mutableState.update { it.copy(saving = true, deleteDialog = dialog.copy(save = null)) }
        viewModelScope.launch {
            try {
                target.deletePortfolio(removal)
                mutableState.update { it.withoutPortfolio(removal).copy(saving = false, deleteDialog = null) }
                selectTab(Tab.PORTFOLIOS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, deleteDialog = dialog.copy(save = SaveRefusal.SAVE_FAILED)) }
            }
        }
    }

    // The records [removal] names dropped from the state, as K2 removed them; a form that had the portfolio picked
    // loses it, and a Cash ticker with no trade line left goes back to All tickers (decision 2).
    private fun AppState.withoutPortfolio(removal: PortfolioRemoval): AppState {
        val id = removal.portfolioId
        val leftEntries = entries.filter { it.id !in removal.cashEntryIds }
        val leftTransactions = transactions.filter { it.id !in removal.transactionIds }
        val form = cashFilterForm
        val ticker = form.ticker?.takeIf { it in CashAccount.tickers(leftEntries, leftTransactions) }
        return copy(
            portfolios = portfolios.filter { it.id != id },
            transactions = leftTransactions,
            entries = leftEntries,
            hidden = hidden - removal.hidden.toSet(),
            overrides = overrides - removal.overrides.toSet(),
            cashFilterForm = if (ticker == form.ticker) form else form.copy(ticker = ticker),
            buyForm = if (buyForm.portfolioId == id) buyForm.copy(portfolioId = null) else buyForm,
            sellForm = if (sellForm.portfolioId == id) SellForm() else sellForm,
            splitForm = if (splitForm.portfolioId == id) splitForm.copy(portfolioId = null) else splitForm,
        )
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

    /**
     * Back from the detail to the Cash list, to the transaction whose cash link opened it (13a decision 3), or to the
     * Transaction history whose cash-effect line opened it (27).
     */
    fun closeDetail() {
        val current = mutableState.value
        if (current.screen != Screen.DETAIL) return
        val back =
            when {
                current.detailFromTransaction -> Screen.TRANSACTION
                current.detailFromHistory -> Screen.TRANSACTION_HISTORY
                else -> Screen.TABS
            }
        mutableState.update { it.copy(screen = back, detailId = null, detailFromTransaction = false, detailFromHistory = false) }
    }

    /**
     * Opens a buy, sell or split (wireframe 10b frame G): from a Transactions row on Holding detail, or from a trade
     * line on the Cash tab (criterion 29). Back returns there. Unknown ids are ignored.
     */
    fun openTransaction(id: String) {
        val current = mutableState.value
        // 27: also a row of Transaction history, not while its Ticker menu is open. A sale on Realized gains opens the
        // history first (27 decision 1).
        if (current.screen != Screen.HOLDING && current.screen != Screen.TABS && current.screen != Screen.TRANSACTION_HISTORY) return
        if (current.historyTickerMenuOpen) return
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
        // A sell's reversal that gives a hidden holding shares unhides it in the same save (4.4; 15 decision 2).
        val unhide = if (created.cashLine == null) emptyList() else Hiding.unhiddenBy(current.transactions + created.transaction, current.hidden)
        mutableState.update { it.copy(saving = true, transactionReversalRefusals = TransactionReversalRefusals()) }
        viewModelScope.launch {
            try {
                target.saveReversal(created.transaction, created.cashLine, unhide)
                mutableState.update {
                    valued(
                        it.copy(
                            screen = Screen.TRANSACTION,
                            transactions = it.transactions + created.transaction,
                            entries = it.entries + listOfNotNull(created.cashLine),
                            hidden = it.hidden - unhide.toSet(),
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
            ).withPair(null)
        mutableState.value =
            when (original.type) {
                // A split starts with its own ratio (14b; 10c marker 11).
                TransactionType.SPLIT -> {
                    val ratio = original.split ?: return
                    replacing.copy(
                        screen = Screen.SPLIT,
                        splitForm =
                        SplitForm(
                            portfolioId = original.portfolioId,
                            ticker = original.ticker,
                            dateText = date,
                            toText = ratio.to.toString(),
                            fromText = ratio.from.toString(),
                        ),
                        splitRefusals = SplitRefusals(),
                        splitReview = null,
                    )
                }

                TransactionType.BUY -> {
                    val quantity = Reentries.startingQuantity(after, original, original.date, draft.enteredSeq + 1)
                    replacing.copy(
                        screen = Screen.BUY,
                        buyForm = BuyForm(original.portfolioId, original.ticker, date, quantity.toPlainString(), total),
                        replaceFilledQuantity = quantity.toPlainString(),
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

    /**
     * Reverse and re-enter on a direct cash entry (14b; 10c frame E): the cash form with the original's type, date,
     * amount (as a positive number with its direction) and note. The reversal's note is [defaultNote], fixed (decision
     * 1); the screen passes it because its wording is D9 and lives in the strings, as for Reverse (05).
     */
    fun openReenterCash(defaultNote: String) {
        val current = mutableState.value
        if (current.screen != Screen.DETAIL || !current.detailReenterOffered) return
        val original = current.detailEntry ?: return
        val outgoing = original.amountCents < 0L
        mutableState.value =
            current.copy(
                screen = Screen.FORM,
                form =
                FormState(
                    dateText = original.date.toString(),
                    amountText = BigDecimal.valueOf(Math.absExact(original.amountCents), 2).toPlainString(),
                    noteText = original.note,
                    type = original.type,
                    direction = if (original.type == CashType.ADJUSTMENT && outgoing) CashDirection.OUT else CashDirection.IN,
                ),
                refusals = Refusals(),
                replacingCashId = original.id,
                replaceReversalNote = defaultNote,
                replaceReversalDate = original.date.toString().replace("-", ""),
                replaceDateEditing = false,
                replaceRefusals = ReplaceRefusals(),
            ).withPair(null)
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
            when (val result = date?.let { pairCheck(dated, PROBE_IDS, it) }) {
                is PairCheck.Passed -> dated.withPair(result.pair)
                is PairCheck.Refused -> dated.copy(replaceRefusals = ReplaceRefusals(rule = result.reason))
                null -> dated
            }
    }

    /** Back to edit, or Back on the combined review: the entry screen, with the values and the reversal date kept. */
    fun backToReplaceEntry() {
        val current = mutableState.value
        if (current.screen != Screen.REPLACE_REVIEW || current.saving) return
        val entry =
            when {
                current.replacingCashId != null -> Screen.FORM
                current.replacedTransaction?.type == TransactionType.SELL -> Screen.SELL
                current.replacedTransaction?.type == TransactionType.SPLIT -> Screen.SPLIT
                else -> Screen.BUY
            }
        mutableState.update {
            it.copy(screen = entry, replaceRefusals = ReplaceRefusals(), replaceDateEditing = false).withPair(null)
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
        if (current.replacedTransaction == null && current.replacedCashEntry == null) return
        val dateRefusal = digitsDateRefusal(current.replaceReversalDate)
        val date = DateDigits.toDate(current.replaceReversalDate)
        if (dateRefusal != null || date == null) {
            // The field shows the reason, so it is open whenever the date is refused (14a Deploy finding 1).
            mutableState.update {
                it.copy(replaceRefusals = ReplaceRefusals(date = dateRefusal ?: DateRefusal.NOT_A_DATE), replaceDateEditing = true)
            }
            return
        }
        val pair =
            when (val result = pairCheck(current, ReentryIds(newId(), newId(), newId(), newId()), date)) {
                is PairCheck.Passed -> result.pair

                is PairCheck.Refused -> {
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
                // Each kind is saved as one action (K2): a trade pair with its cash lines, a split pair, or the two cash
                // entries as one batch. The app returns to the original's detail (14a decision 2; 05).
                // A trade pair that leaves a hidden holding with shares unhides it in the same save (4.4; 15 decision 2).
                val unhide =
                    (pair as? ReplacePair.Trade)?.created?.let {
                        Hiding.unhiddenBy(current.transactions + it.reversal + it.replacement, current.hidden)
                    } ?: emptyList()
                when (pair) {
                    is ReplacePair.Trade -> pair.created.let { target.savePair(it.reversal, it.reversalCashLine, it.replacement, it.replacementCashLine, unhide) }
                    is ReplacePair.Split -> target.saveSplitPair(pair.created.reversal, pair.created.replacement)
                    is ReplacePair.Cash -> target.saveCashEntries(listOf(pair.created.reversal, pair.created.replacement))
                }
                mutableState.update { valued(saved(it, pair).copy(hidden = it.hidden - unhide.toSet(), saving = false).leftReplace()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, replaceRefusals = ReplaceRefusals(save = SaveRefusal.SAVE_FAILED)) }
            }
        }
    }

    // The records of a saved pair added, on the original's detail: a transaction's, or a cash entry's.
    private fun saved(
        state: AppState,
        pair: ReplacePair,
    ): AppState = when (pair) {
        is ReplacePair.Trade ->
            state.copy(
                screen = Screen.TRANSACTION,
                transactions = state.transactions + pair.created.reversal + pair.created.replacement,
                entries = state.entries + pair.created.reversalCashLine + pair.created.replacementCashLine,
            )

        is ReplacePair.Split ->
            state.copy(screen = Screen.TRANSACTION, transactions = state.transactions + pair.created.reversal + pair.created.replacement)

        is ReplacePair.Cash ->
            state.copy(screen = Screen.DETAIL, entries = state.entries + pair.created.reversal + pair.created.replacement)
    }

    /** K1's pair rules for the replaced entry of any kind; null when an entry field does not parse. */
    private fun pairCheck(
        current: AppState,
        ids: ReentryIds,
        reversalDate: LocalDate,
        standIns: Boolean = false,
    ): PairCheck? {
        current.replacedCashEntry?.let { return cashPairOf(current, it, ids, reversalDate, standIns) }
        val original = current.replacedTransaction ?: return null
        if (original.type == TransactionType.SPLIT) return splitPairOf(current, original, ids, reversalDate, standIns)
        return when (val result = pairOf(current, ids, reversalDate, standIns)) {
            is ReentryResult.Created -> PairCheck.Passed(ReplacePair.Trade(result))
            is ReentryResult.Refused -> PairCheck.Refused(result.reason)
            null -> null
        }
    }

    /** A split's pair on the typed split (14b); with [standIns], a bad number makes the ratio 2 for 1, as reviewSplit does. */
    private fun splitPairOf(
        current: AppState,
        original: Transaction,
        ids: ReentryIds,
        reversalDate: LocalDate,
        standIns: Boolean,
    ): PairCheck? {
        val form = current.splitForm
        val date = DateDigits.toDate(form.dateText) ?: return null
        val to = ratioNumber(form.toText)
        val from = ratioNumber(form.fromText)
        val ratio =
            when {
                to != null && from != null -> SplitRatio(to, from)
                standIns -> SplitRatio(2, 1)
                else -> return null
            }
        val result =
            Reentries.reenterSplit(
                cashEntries = current.entries,
                transactions = current.transactions,
                portfolios = current.portfolios,
                reversalId = ids.reversalId,
                replacementId = ids.replacementId,
                originalId = original.id,
                reversalDate = reversalDate,
                portfolioId = form.portfolioId.orEmpty(),
                ticker = form.ticker ?: if (standIns) PROBE_ID else return null,
                date = date,
                ratio = ratio,
            )
        return when (result) {
            is SplitPairResult.Created -> PairCheck.Passed(ReplacePair.Split(result))
            is SplitPairResult.Refused -> PairCheck.Refused(result.reason)
        }
    }

    /** The form's note judged on its own, with a stand-in date and amount; null when the note passes. */
    private fun cashNoteRule(current: AppState): CashEntryRefusal? {
        val form = current.form
        val probe = CashEntries.create(current.entries, PROBE_ID, form.type, marketDate.today(), 1L, form.noteText, form.direction)
        return (probe as? CashEntryResult.Refused)?.reason
    }

    /**
     * A cash entry's pair on the typed form (14b), the reversal with its fixed note (decision 1); null when the date or
     * amount does not parse. With [standIns], a refused note is replaced by a stand-in, so the cash rule shows beside the
     * note's; a note that passes is kept, as the review shows it (14b phone check 3).
     */
    private fun cashPairOf(
        current: AppState,
        original: CashEntry,
        ids: ReentryIds,
        reversalDate: LocalDate,
        standIns: Boolean,
    ): PairCheck? {
        val form = current.form
        val date = MarketDate.parse(form.dateText.trim()) ?: return null
        val cents = (Money.parseCents(form.amountText) as? AmountResult.Cents)?.value ?: return null
        val result =
            Reentries.reenterCash(
                existing = current.entries,
                reversalId = ids.reversalId,
                originalId = original.id,
                reversalDate = reversalDate,
                reversalNote = current.replaceReversalNote,
                replacementId = ids.replacementId,
                type = form.type,
                date = date,
                amountCents = cents,
                note = if (standIns && cashNoteRule(current) != null) PROBE_NOTE else form.noteText,
                direction = form.direction,
            )
        return when (result) {
            is CashPairResult.Created -> PairCheck.Passed(ReplacePair.Cash(result))
            is CashPairResult.Refused -> PairCheck.Refused(result.reason)
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
                    asFilledIn = typed != null && form.quantityText == current.replaceFilledQuantity,
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
        val originalDate = current.replacedCashEntry?.date ?: current.replacedTransaction?.date ?: return PairReview.OnEntry(null)
        val date = DateDigits.toDate(current.replaceReversalDate) ?: originalDate
        return when (val result = pairCheck(current, PROBE_IDS, date, standIns = true)) {
            null -> PairReview.OnEntry(null)

            is PairCheck.Passed -> PairReview.Passes(result.pair)

            is PairCheck.Refused -> {
                val reason = (result.reason as? ReentryRefusal.OfReversal)?.reason
                val earliest =
                    when {
                        !result.reason.fixedOnReview -> null
                        reason is ReversalRefusal.TooEarly -> reason.earliest
                        else -> originalDate
                    }
                when (val retried = earliest?.let { pairCheck(current, PROBE_IDS, it, standIns = true) }) {
                    is PairCheck.Passed -> PairReview.OnReview(retried.pair, result.reason)
                    is PairCheck.Refused -> PairReview.OnEntry(retried.reason)
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
                // 27: opened from Transaction history, the transaction's Back returns there.
                it.copy(
                    screen = Screen.TRANSACTION,
                    transactionId = transaction.id,
                    transactionFrom = if (it.detailFromHistory) Screen.TRANSACTION_HISTORY else Screen.TABS,
                    detailId = null,
                    detailFromTransaction = false,
                    detailFromHistory = false,
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
        hidden: List<HiddenHolding>,
        autoLock: AutoLock,
        tabOrder: TabOrder,
        classLists: ClassLists,
        overrides: List<ClassOverride>,
    ) {
        mutableState.update {
            valued(
                it.copy(
                    screen = Screen.TABS,
                    entries = entries,
                    portfolios = Portfolios.inCreationOrder(portfolios),
                    transactions = transactions,
                    instruments = instruments,
                    hidden = hidden,
                    autoLock = autoLock,
                    tabOrder = tabOrder,
                    classLists = classLists,
                    overrides = overrides,
                ),
            )
        }
    }

    /** [state] as of today in New York, with its holdings and net worth for its records (4.8). */
    private fun valued(state: AppState): AppState = state.copy(today = marketDate.today()).refreshed()

    /** Opens a holding from its row on the portfolio's screen (view 6). Unknown tickers are ignored. */
    fun openHolding(ticker: String) {
        val current = mutableState.value
        val id = current.portfolioId ?: return
        if (current.screen != Screen.PORTFOLIO || current.holdingsOf(id).none { it.ticker == ticker }) return
        mutableState.update {
            it.copy(
                screen = Screen.HOLDING,
                holdingTicker = ticker,
                holdingTab = HoldingTab.TRANSACTIONS,
                portfolioMenuOpen = false,
                overrideRefusal = null,
            )
        }
    }

    /** "<" or Android Back: back to the portfolio, or to Hidden holdings if it was opened there (15 decision 3). */
    fun closeHolding() {
        if (mutableState.value.screen != Screen.HOLDING) return
        mutableState.update {
            it.copy(
                screen = if (it.holdingFromHidden) Screen.HIDDEN_HOLDINGS else Screen.PORTFOLIO,
                holdingTicker = null,
                holdingFromHidden = false,
                buyNotice = null,
                hideRefusal = null,
                // 24's Deploy review: a failed clear's "Not saved" belongs to this visit.
                overrideRefusal = null,
            )
        }
    }

    /** Hide on Holding detail (4.4): no confirmation; the holding stays shown, now hidden. Not a transaction. */
    fun hide() {
        val current = mutableState.value
        if (current.screen != Screen.HOLDING || current.saving || !current.hideOffered) return
        val holding = HiddenHolding(current.portfolioId ?: return, current.holdingTicker ?: return)
        saveHidden { target ->
            target.hide(holding)
            mutableState.update { it.copy(hidden = it.hidden + holding) }
        }
    }

    /** Hidden holdings, from the portfolio's screen, where it is always listed (view 9, marker 1). */
    fun openHiddenHoldings() {
        val current = mutableState.value
        if (current.screen != Screen.PORTFOLIO || current.saving) return
        mutableState.update { it.copy(screen = Screen.HIDDEN_HOLDINGS, portfolioMenuOpen = false, hideRefusal = null) }
    }

    /** "<" or Android Back: back to the portfolio. */
    fun closeHiddenHoldings() {
        val current = mutableState.value
        if (current.screen != Screen.HIDDEN_HOLDINGS || current.saving) return
        mutableState.update { it.copy(screen = Screen.PORTFOLIO, hideRefusal = null) }
    }

    /** Unhide on a row (view 9, marker 3): no confirmation; the row goes and the holding is back in the list at 0 shares. */
    fun unhide(ticker: String) {
        val current = mutableState.value
        if (current.screen != Screen.HIDDEN_HOLDINGS || current.saving) return
        val holding = HiddenHolding(current.portfolioId ?: return, ticker)
        if (holding !in current.hidden) return
        saveHidden { target ->
            target.unhide(holding)
            mutableState.update { it.copy(hidden = it.hidden - holding) }
        }
    }

    /** A row of Hidden holdings opens its Holding detail; Back returns to Hidden holdings (15 decision 3). */
    fun openHiddenHolding(ticker: String) {
        val current = mutableState.value
        val id = current.portfolioId ?: return
        if (current.screen != Screen.HIDDEN_HOLDINGS || HiddenHolding(id, ticker) !in current.hidden) return
        mutableState.update {
            it.copy(screen = Screen.HOLDING, holdingTicker = ticker, holdingTab = HoldingTab.TRANSACTIONS, holdingFromHidden = true, hideRefusal = null)
        }
    }

    // A Hide or an Unhide: one write; a failure changes nothing and says so (D9 wording in the screens).
    private fun saveHidden(write: suspend (RecordStore) -> Unit) {
        val target = store ?: return
        mutableState.update { it.copy(saving = true, hideRefusal = null) }
        viewModelScope.launch {
            try {
                write(target)
                mutableState.update { it.copy(saving = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, hideRefusal = SaveRefusal.SAVE_FAILED) }
            }
        }
    }

    /** The screenshot window's milliseconds left, null when off, from MainActivity at each change and tick (21). Shown only. */
    fun onScreenshotWindow(leftMs: Long?) = mutableState.update { it.copy(screenshotWindowLeftMs = leftMs) }

    /** The Screenshot window row (marker 8): opens its layer, drawn in the window; on Settings only, not over another layer. */
    fun openScreenshotLayer() {
        val current = mutableState.value
        if (current.screen != Screen.SETTINGS || current.saving || current.autoLockChoiceOpen) return
        mutableState.update { it.copy(screenshotLayerOpen = true) }
    }

    /** Back or a tap outside the layer: closes it; the window goes on or stays off as it is. */
    fun closeScreenshotLayer() = mutableState.update { it.copy(screenshotLayerOpen = false) }

    /** More's gear (section 6; wireframe 10f marker 2): opens Settings; the tab stays More, so Back returns there. */
    fun openSettings() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.tab != Tab.MORE || current.saving) return
        mutableState.update { it.copy(screen = Screen.SETTINGS, autoLockChoiceOpen = false, autoLockRefusal = null) }
    }

    /** "<" or Android Back: back to More. Ignored while a save is running. */
    fun closeSettings() {
        val current = mutableState.value
        if (current.screen != Screen.SETTINGS || current.saving) return
        mutableState.update { it.copy(screen = Screen.TABS, autoLockChoiceOpen = false, screenshotLayerOpen = false, autoLockRefusal = null) }
    }

    /** The Auto-lock row (marker 3): opens the choice layer (frame C), drawn in the window; on Settings only. */
    fun openAutoLockChoice() {
        val current = mutableState.value
        if (current.screen != Screen.SETTINGS || current.saving || current.screenshotLayerOpen) return
        mutableState.update { it.copy(autoLockChoiceOpen = true, autoLockRefusal = null) }
    }

    /** Back or a tap outside the layer: closes it and keeps the choice. */
    fun closeAutoLockChoice() = mutableState.update { it.copy(autoLockChoiceOpen = false) }

    /**
     * One of the four choices in the layer: closes it, then saves [choice] and applies it once saved (20): the lock
     * follows [AppState.autoLock], so a choice that could not be saved never applies, and a failure says so (D9 wording
     * in the screens). Ignored without the layer open, while a save is running, and for the choice already made.
     */
    fun chooseAutoLock(choice: AutoLock) {
        val current = mutableState.value
        val target = store ?: return
        if (current.screen != Screen.SETTINGS || !current.autoLockChoiceOpen || current.saving) return
        mutableState.update { it.copy(autoLockChoiceOpen = false) }
        if (current.autoLock == null || choice == current.autoLock) return
        mutableState.update { it.copy(saving = true, autoLockRefusal = null) }
        viewModelScope.launch {
            try {
                target.saveAutoLock(choice)
                mutableState.update { it.copy(saving = false, autoLock = choice) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, autoLockRefusal = SaveRefusal.SAVE_FAILED) }
            }
        }
    }

    /** Settings' Tab order row (wireframe 10f marker 5): opens the Tab order screen (frame E); on Settings only. */
    fun openTabOrder() {
        val current = mutableState.value
        if (current.screen != Screen.SETTINGS || current.saving || current.autoLockChoiceOpen || current.screenshotLayerOpen) return
        mutableState.update { it.copy(screen = Screen.TAB_ORDER, tabOrderRefusal = null) }
    }

    /** "<" or Android Back: back to Settings. Ignored while a save is running. */
    fun closeTabOrder() {
        val current = mutableState.value
        if (current.screen != Screen.TAB_ORDER || current.saving) return
        mutableState.update { it.copy(screen = Screen.SETTINGS, tabOrderRefusal = null) }
    }

    /** Up on [tab]'s row: one place left (frame E, marker 11). */
    fun moveTabUp(tab: Tab) = moveTab(mutableState.value.tabOrder.moveUp(tab))

    /** Down on [tab]'s row: one place right. */
    fun moveTabDown(tab: Tab) = moveTab(mutableState.value.tabOrder.moveDown(tab))

    /**
     * Saves [moved] and applies it once saved, as the auto-lock choice (20): the bar follows [AppState.tabOrder], so an
     * order that could not be saved never shows, and a failure says so. Ignored off the Tab order screen, while a save is
     * running, and when nothing moved (the first's Up, the last's Down; decision 1).
     */
    private fun moveTab(moved: TabOrder) {
        val current = mutableState.value
        val target = store ?: return
        if (current.screen != Screen.TAB_ORDER || current.saving || moved == current.tabOrder) return
        mutableState.update { it.copy(saving = true, tabOrderRefusal = null) }
        viewModelScope.launch {
            try {
                target.saveTabOrder(moved)
                mutableState.update { it.copy(saving = false, tabOrder = moved) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, tabOrderRefusal = SaveRefusal.SAVE_FAILED) }
            }
        }
    }

    /**
     * Edit on the Classification tab's [list] row (decision 2): opens the picker (frame H) on the value shown, when it is
     * a value of the list; on Holding detail only.
     */
    fun openClassPicker(list: ClassList) {
        val current = mutableState.value
        if (current.portfolioId == null || current.holdingTicker == null) return
        if (current.screen != Screen.HOLDING || current.saving) return
        val choice = current.classValueShown(list)
        mutableState.update { it.copy(screen = Screen.CLASS_PICKER, classPicker = list, classPickerChoice = choice, overrideRefusal = null) }
    }

    /** A row of the picker: picks [value] when it is on the list (4.9: picked, not typed). */
    fun chooseClassValue(value: String) {
        val current = mutableState.value
        val list = current.classPicker ?: return
        if (current.screen != Screen.CLASS_PICKER || current.saving) return
        val picked = Classification.overrideValue(current.classLists[list], value) ?: return
        mutableState.update { it.copy(classPickerChoice = picked, overrideRefusal = null) }
    }

    /** Cancel, "<" or Android Back: back to the Classification tab, nothing changed. Ignored while a save is running. */
    fun cancelClassPicker() {
        val current = mutableState.value
        if (current.screen != Screen.CLASS_PICKER || current.saving) return
        mutableState.update { it.copy(screen = Screen.HOLDING, classPicker = null, classPickerChoice = null, overrideRefusal = null) }
    }

    /**
     * Save (frame H): stores the pick as the holding's override and applies it once stored, as the tab order (22); back on
     * the Classification tab, the breakdowns following. A failure keeps the picker and the pick and says so.
     */
    fun saveClassOverride() {
        val current = mutableState.value
        val target = store ?: return
        val list = current.classPicker ?: return
        val value = current.classPickerChoice ?: return
        val portfolioId = current.portfolioId ?: return
        val ticker = current.holdingTicker ?: return
        if (current.screen != Screen.CLASS_PICKER || current.saving) return
        // Step 7 fix (your A): nothing to save while the value shown is still picked.
        if (current.classPickerModel?.canSave != true) return
        val override = ClassOverride(portfolioId, ticker, list, value)
        mutableState.update { it.copy(saving = true, overrideRefusal = null) }
        viewModelScope.launch {
            try {
                target.saveOverride(override)
                mutableState.update {
                    it.copy(
                        saving = false,
                        screen = Screen.HOLDING,
                        overrides = it.overrides.filterNot { kept -> kept.isFor(portfolioId, ticker, list) } + override,
                        classPicker = null,
                        classPickerChoice = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, overrideRefusal = SaveRefusal.SAVE_FAILED) }
            }
        }
    }

    /**
     * Clear override on the Classification tab's [list] row (4.9): back to the source's value or "Unclassified" once
     * cleared in the store. Ignored when the holding has no override on [list]; a failure keeps it and says so.
     */
    fun clearClassOverride(list: ClassList) {
        val current = mutableState.value
        val target = store ?: return
        val portfolioId = current.portfolioId ?: return
        val ticker = current.holdingTicker ?: return
        if (current.screen != Screen.HOLDING || current.saving || current.overrides.none { it.isFor(portfolioId, ticker, list) }) return
        mutableState.update { it.copy(saving = true, overrideRefusal = null) }
        viewModelScope.launch {
            try {
                target.clearOverride(portfolioId, ticker, list)
                mutableState.update { it.copy(saving = false, overrides = it.overrides.filterNot { kept -> kept.isFor(portfolioId, ticker, list) }) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, overrideRefusal = SaveRefusal.SAVE_FAILED) }
            }
        }
    }

    private fun ClassOverride.isFor(
        portfolioId: String,
        ticker: String,
        list: ClassList,
    ): Boolean = this.portfolioId == portfolioId && this.ticker == ticker && this.list == list

    /** Settings' Sectors or Geographies row (wireframe 10f marker 6): opens that list's screen (frame F); on Settings only. */
    fun openClassList(list: ClassList) {
        val current = mutableState.value
        if (current.screen != Screen.SETTINGS || current.saving || current.autoLockChoiceOpen || current.screenshotLayerOpen) return
        mutableState.update { it.copy(screen = Screen.CLASS_LIST, classListShown = list).leftListLines() }
    }

    /** "<" or Android Back: back to Settings. Ignored while a save is running. */
    fun closeClassList() {
        val current = mutableState.value
        if (current.screen != Screen.CLASS_LIST || current.saving) return
        mutableState.update { it.copy(screen = Screen.SETTINGS, classListShown = null).leftListLines() }
    }

    /** Add value (frame F, marker 13): opens the Name layer, empty. */
    fun openAddClassValue() = openClassValueEdit(ClassValueEdit(renaming = null, text = ""))

    /** Rename on [value]'s row: opens the Name layer on its name; ignored for a value not on the list shown. */
    fun openRenameClassValue(value: String) {
        val list = mutableState.value.classListShown ?: return
        if (value !in mutableState.value.classLists[list]) return
        openClassValueEdit(ClassValueEdit(renaming = value, text = value))
    }

    private fun openClassValueEdit(edit: ClassValueEdit) {
        val current = mutableState.value
        if (current.screen != Screen.CLASS_LIST || current.saving || current.classValueEdit != null) return
        mutableState.update { it.copy(classValueEdit = edit).leftListLines(keepEdit = true) }
    }

    /** The Name field: keeps [text] as typed; a refusal or failure shown is cleared. */
    fun typeClassValueName(text: String) {
        val current = mutableState.value
        val edit = current.classValueEdit ?: return
        if (current.screen != Screen.CLASS_LIST || current.saving) return
        mutableState.update { it.copy(classValueEdit = edit.copy(text = text)).leftListLines(keepEdit = true) }
    }

    /** Cancel or Back on the Name layer: closes it, nothing changed. Ignored while a save is running. */
    fun cancelClassValueName() {
        val current = mutableState.value
        if (current.screen != Screen.CLASS_LIST || current.saving || current.classValueEdit == null) return
        mutableState.update { it.leftListLines() }
    }

    /**
     * Save on the Name layer (frame G): adds or renames by K10's rule (4.9; decisions 1 and 2), judged on the lists in the
     * state first; a refusal keeps the layer and the typed text and says why. Once stored, the lists (and with a rename
     * the overrides, by their row) follow, so the tab, the picker and the breakdowns change at once; the layer closes
     * and a line confirms it. A rename to the same name stores nothing. A failure keeps the layer and says so.
     */
    fun saveClassValueName() {
        val current = mutableState.value
        val target = store ?: return
        val list = current.classListShown ?: return
        val edit = current.classValueEdit ?: return
        if (current.screen != Screen.CLASS_LIST || current.saving) return
        val renaming = edit.renaming
        val result =
            if (renaming == null) {
                Classification.add(current.classLists, list, edit.text)
            } else {
                Classification.rename(current.classLists, list, renaming, edit.text, current.instruments.mapNotNull { it.profile })
            }
        when (result) {
            is ListResult.Refused -> {
                mutableState.update { it.copy(listOutcome = result, listSaveRefusal = null) }
                return
            }

            is ListResult.Renamed ->
                if (result.from == result.to) {
                    mutableState.update { it.leftListLines() }
                    return
                }

            else -> Unit
        }
        mutableState.update { it.copy(saving = true, listOutcome = null, listSaveRefusal = null) }
        viewModelScope.launch {
            try {
                val lists =
                    when (result) {
                        is ListResult.Added -> result.lists.also { target.addListValue(list, result.value) }
                        is ListResult.Renamed -> result.lists.also { target.renameListValue(list, result.from, result.to) }
                        else -> return@launch
                    }
                mutableState.update {
                    val overrides =
                        if (result is ListResult.Renamed) {
                            it.overrides.map { o -> if (o.list == list && o.value == result.from) o.copy(value = result.to) else o }
                        } else {
                            it.overrides
                        }
                    it.copy(saving = false, classLists = lists, overrides = overrides, classValueEdit = null, listOutcome = result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, listSaveRefusal = SaveRefusal.SAVE_FAILED) }
            }
        }
    }

    /**
     * Delete on [value]'s row (frame F): refused while K10 counts a holding that uses it, hidden ones included (4.9);
     * otherwise deleted with its remembered texts and the overrides of holdings no longer there (decision 3), and a line
     * confirms it. A failure keeps the value and says so. Ignored while the Name layer is open or a save is running.
     */
    fun deleteClassValue(value: String) {
        val current = mutableState.value
        val target = store ?: return
        val list = current.classListShown ?: return
        if (current.screen != Screen.CLASS_LIST || current.saving || current.classValueEdit != null) return
        if (value !in current.classLists[list]) return
        val deleted =
            when (val result = Classification.delete(current.classLists, list, value, current.listUsers)) {
                is ListResult.Deleted -> result

                else -> {
                    mutableState.update { it.copy(listOutcome = result, listSaveRefusal = null) }
                    return
                }
            }
        mutableState.update { it.copy(saving = true, listOutcome = null, listSaveRefusal = null) }
        viewModelScope.launch {
            try {
                target.deleteListValue(list, deleted.value, deleted.overridesRemoved)
                mutableState.update {
                    it.copy(
                        saving = false,
                        classLists = deleted.lists,
                        overrides = it.overrides - deleted.overridesRemoved.toSet(),
                        listOutcome = deleted,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                mutableState.update { it.copy(screen = Screen.UNREADABLE, saving = false) }
            } catch (e: Exception) {
                mutableState.update { it.copy(saving = false, listSaveRefusal = SaveRefusal.SAVE_FAILED) }
            }
        }
    }

    /** The list screen's layer and lines dropped; with [keepEdit] the Name layer stays. */
    private fun AppState.leftListLines(keepEdit: Boolean = false): AppState {
        val edit = if (keepEdit) classValueEdit else null
        return copy(classValueEdit = edit, listOutcome = null, listSaveRefusal = null)
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
            // Refreshed here, so the lot list read below is the new date's.
            val dated =
                state
                    .copy(
                        sellForm = state.sellForm.copy(dateText = text),
                        sellRefusals = state.sellRefusals.copy(date = refusal, rule = null),
                    ).refreshed()
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
                replaceRefusals = ReplaceRefusals(rule = (review as? PairReview.OnReview)?.refusal),
                replaceDateEditing = DateDigits.toDate(it.replaceReversalDate) == null,
            ).withPair(review.created)
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

    /**
     * Back from the split entry to the holding it came from; the typed text is kept. While a split is replaced, back to
     * its detail, nothing kept (14b).
     */
    fun closeSplit() {
        val current = mutableState.value
        if (current.screen != Screen.SPLIT) return
        if (current.replacingId != null) {
            mutableState.update { it.copy(screen = Screen.TRANSACTION, splitRefusals = SplitRefusals()).leftReplace() }
            return
        }
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
        if (current.replacingId != null) {
            reviewReplacedSplit(current, fieldRefusals)
            return
        }
        val probe = splitOf(current, PROBE_ID, standIns = true)
        val refusals = fieldRefusals.copy(rule = shownSplitRule(current, fieldRefusals, (probe as? SplitResult.Refused)?.reason))
        if (refusals != SplitRefusals() || probe !is SplitResult.Created) {
            mutableState.update { it.copy(splitRefusals = refusals) }
            return
        }
        mutableState.update { it.copy(screen = Screen.SPLIT_REVIEW, splitReview = probe, splitRefusals = SplitRefusals()) }
    }

    // Review of a replacing split (14b): the new split's rules show as on the plain screen, the reversal's in
    // replaceRefusals; only a reversal-date refusal opens the combined review (14a decision 3).
    private fun reviewReplacedSplit(
        current: AppState,
        fieldRefusals: SplitRefusals,
    ) {
        val review = pairAtReview(current)
        val entryRule = (review as? PairReview.OnEntry)?.reason
        val refusals = fieldRefusals.copy(rule = shownSplitRule(current, fieldRefusals, (entryRule as? ReentryRefusal.OfSplit)?.reason))
        if (refusals != SplitRefusals() || review is PairReview.OnEntry) {
            val reversalRule = entryRule as? ReentryRefusal.OfReversal
            mutableState.update { it.copy(splitRefusals = refusals, replaceRefusals = ReplaceRefusals(rule = reversalRule)) }
            return
        }
        mutableState.update {
            it.copy(
                screen = Screen.REPLACE_REVIEW,
                splitRefusals = SplitRefusals(),
                replaceRefusals = ReplaceRefusals(rule = (review as? PairReview.OnReview)?.refusal),
                replaceDateEditing = DateDigits.toDate(it.replaceReversalDate) == null,
            ).withPair(review.created)
        }
    }

    // A stand-in ratio makes the ratio and replay rules meaningless, and a missing ticker the ticker rule; the field
    // refusals say why (12).
    private fun shownSplitRule(
        current: AppState,
        fieldRefusals: SplitRefusals,
        reason: SplitRefusal?,
    ): SplitRefusal? {
        val ratioStandIn = fieldRefusals.to != null || fieldRefusals.from != null
        return when {
            ratioStandIn && (reason is SplitRefusal.SellShort || reason == SplitRefusal.RatioEqual) -> null
            current.splitForm.ticker == null && reason == SplitRefusal.TickerNotBought -> null
            else -> reason
        }
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
        // The pair's own rules: the reversal's, or a ratio too large to keep the original's exact shares (14c decision 1).
        val reversalRule = entryRule?.takeIf { it is ReentryRefusal.OfReversal || it is ReentryRefusal.QuantityRatioTooLarge }
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
                            replaceRefusals = ReplaceRefusals(rule = (pair as? PairReview.OnReview)?.refusal),
                            // An unfinished reversal date kept through Back to edit opens in its field (Deploy finding 1).
                            replaceDateEditing = replacing && DateDigits.toDate(state.replaceReversalDate) == null,
                        ).withPair(pair?.created)
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
        // A new buy of a hidden ticker unhides it in the same save (4.4; 15 decision 2).
        val unhide = Hiding.unhiddenBy(current.transactions + created.transaction, current.hidden)
        mutableState.update { it.copy(saving = true, buyRefusals = BuyRefusals()) }
        viewModelScope.launch {
            try {
                target.saveBuy(created.transaction, created.cashLine, unhide)
                mutableState.update {
                    valued(
                        it.copy(
                            screen = afterBuy(it),
                            entries = it.entries + created.cashLine,
                            transactions = it.transactions + created.transaction,
                            hidden = it.hidden - unhide.toSet(),
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
                Screen.HIDDEN_HOLDINGS,
                Screen.SELL,
                Screen.SELL_REVIEW,
                Screen.SPLIT,
                Screen.SPLIT_REVIEW,
                Screen.TRANSACTION,
                Screen.REVERSE_TRANSACTION,
                Screen.REPLACE_REVIEW,
                Screen.NET_WORTH,
                Screen.ALLOCATION,
                Screen.SETTINGS,
                Screen.TAB_ORDER,
                Screen.CLASS_PICKER,
                Screen.CLASS_LIST,
                Screen.REALIZED_GAINS,
                Screen.TRANSACTION_HISTORY,
            )
    }
}
