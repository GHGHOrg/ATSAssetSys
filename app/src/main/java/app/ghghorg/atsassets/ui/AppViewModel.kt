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
import app.ghghorg.atsassets.core.MarketDate
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.OpeningBalance
import app.ghghorg.atsassets.core.OpeningBalanceError
import app.ghghorg.atsassets.core.OpeningBalanceResult
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
import app.ghghorg.atsassets.core.TickerCheck
import app.ghghorg.atsassets.core.Transaction
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
}

/**
 * A line the Buy/Sell menu shows instead of an entry screen (wireframe 10a frame G; 09 decision 2). Reasons only;
 * the wording is D9 and lives in the screens.
 */
enum class BuyNotice {
    NEEDS_OPENING_BALANCE,
    NEEDS_PORTFOLIO,
    NOTHING_TO_SELL,

    // Something is held, but the sell screen arrives in iteration 11.
    SELL_LATER,
}

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
    // The entry shown on DETAIL; on REVERSAL, the entry being reversed.
    val detailId: String? = null,
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

    /** Each holding at cost while there are no prices (4.8). */
    fun portfolioValue(portfolioId: String): Fraction = holdingsOf(portfolioId).fold(Fraction.ZERO) { sum, holding -> sum + holding.cost }

    /** The derived price per share (4.3), shown as the fields change; null until quantity and total are both valid. */
    val buyPricePerShare: Fraction?
        get() {
            val quantity = Quantity.parse(buyForm.quantityText) as? QuantityResult.Exact ?: return null
            val total = Money.parseCents(buyForm.totalText) as? AmountResult.Cents ?: return null
            return Transactions.pricePerShare(total.value, quantity.value)
        }

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
            )
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

    /** Back from the detail to the Cash list. */
    fun closeDetail() {
        if (mutableState.value.screen != Screen.DETAIL) return
        mutableState.update { it.copy(screen = Screen.TABS, detailId = null) }
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
        )
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

    /** Sell in the menu: nothing held says so; something held points to a later build until 11 (09 decision 2). */
    fun chooseSell() {
        val current = mutableState.value
        if (current.screen != Screen.TABS || current.saving) return
        val held = current.holdings.any { it.shares.signum() > 0 }
        mutableState.update {
            it.copy(buySellMenuOpen = false, buyNotice = if (held) BuyNotice.SELL_LATER else BuyNotice.NOTHING_TO_SELL)
        }
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
     */
    private fun openBuy(portfolioId: String?) = mutableState.update { current ->
        val form = current.buyForm
        val kept = form.portfolioId?.takeIf { id -> current.portfolios.any { it.id == id } }
        val chosen = portfolioId ?: kept ?: current.portfolios.firstOrNull()?.id
        // Digits only, as the Cash filters (09 decision 10): today as YYYYMMDD, shown with its dashes.
        val date = form.dateText.ifEmpty { marketDate.today().toString().replace("-", "") }
        current.copy(
            screen = Screen.BUY,
            buySellMenuOpen = false,
            buyNotice = null,
            buyForm = form.copy(portfolioId = chosen, dateText = date),
            buyRefusals = BuyRefusals(),
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

    /** Back from the buy entry to the tab. The typed text is kept. Ignored while the check runs. */
    fun closeBuy() {
        val current = mutableState.value
        if (current.screen != Screen.BUY || current.checking) return
        mutableState.update { it.copy(screen = Screen.TABS, buyRefusals = BuyRefusals()) }
    }

    /**
     * Review (wireframe 10a marker 6): the field rules, then K1's rules, then the ticker check of 4.3 through K4.
     * Each later stage runs only when the earlier ones pass. Shows the review only if the save would pass.
     */
    fun reviewBuy() {
        val current = mutableState.value
        if (current.screen != Screen.BUY || current.checking || current.saving) return
        val target = store ?: return
        val form = current.buyForm
        val ticker = TickerCheck.normalize(form.tickerText)
        val quantity = Quantity.parse(form.quantityText)
        val total = Money.parseCents(form.totalText)
        val dateRefusal =
            when {
                form.dateText.isEmpty() -> DateRefusal.EMPTY
                !DateDigits.isDigits(form.dateText) -> DateRefusal.NOT_DIGITS
                DateDigits.toDate(form.dateText) == null -> DateRefusal.NOT_A_DATE
                else -> null
            }
        val fieldRefusals =
            BuyRefusals(
                tickerEmpty = ticker == null,
                date = dateRefusal,
                quantity = (quantity as? QuantityResult.Refused)?.reason,
                total = (total as? AmountResult.Refused)?.reason,
            )
        if (fieldRefusals != BuyRefusals()) {
            mutableState.update { it.copy(buyRefusals = fieldRefusals) }
            return
        }
        val checked = ticker ?: return
        val probe = buyOf(current, PROBE_ID, PROBE_ID)
        if (probe !is BuyResult.Created) {
            // Null only without a chosen portfolio, which K1 refuses the same way as an unknown one.
            val rule = (probe as? BuyResult.Refused)?.reason ?: BuyRefusal.PortfolioNotFound
            mutableState.update { it.copy(buyRefusals = BuyRefusals(rule = rule)) }
            return
        }
        mutableState.update { it.copy(checking = true, buyRefusals = BuyRefusals()) }
        viewModelScope.launch {
            try {
                when (val result = TickerVerifier.verify(checked, current.instruments, isOnline(), priceSource, target)) {
                    is Verification.Verified -> mutableState.update { state ->
                        val instruments = state.instruments.filter { it.ticker != result.instrument.ticker } + result.instrument
                        state.copy(screen = Screen.BUY_REVIEW, instruments = instruments, checking = false)
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
                            screen = Screen.TABS,
                            entries = it.entries + created.cashLine,
                            transactions = it.transactions + created.transaction,
                            buyForm = BuyForm(),
                            buyRefusals = BuyRefusals(),
                            saving = false,
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

    /** K1's buy rules on the typed values; null when a field does not parse (the field rules decide then). */
    private fun buyOf(
        current: AppState,
        transactionId: String,
        cashEntryId: String,
    ): BuyResult? {
        val form = current.buyForm
        val date = DateDigits.toDate(form.dateText) ?: return null
        val quantity = Quantity.parse(form.quantityText) as? QuantityResult.Exact ?: return null
        val total = Money.parseCents(form.totalText) as? AmountResult.Cents ?: return null
        return Transactions.buy(
            cashEntries = current.entries,
            transactions = current.transactions,
            portfolios = current.portfolios,
            transactionId = transactionId,
            cashEntryId = cashEntryId,
            portfolioId = form.portfolioId ?: return null,
            ticker = form.tickerText,
            date = date,
            quantity = quantity.value,
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
            setOf(Screen.TABS, Screen.FORM, Screen.REVIEW, Screen.DETAIL, Screen.REVERSAL, Screen.PORTFOLIO, Screen.BUY, Screen.BUY_REVIEW)
    }
}
