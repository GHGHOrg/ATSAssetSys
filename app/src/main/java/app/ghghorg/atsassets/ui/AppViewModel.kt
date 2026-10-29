package app.ghghorg.atsassets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.CashAccount
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntries
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashEntryResult
import app.ghghorg.atsassets.core.CashRow
import app.ghghorg.atsassets.core.CashRules
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.MarketDate
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.OpeningBalance
import app.ghghorg.atsassets.core.OpeningBalanceError
import app.ghghorg.atsassets.core.OpeningBalanceResult
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.UnreadableDataException
import app.ghghorg.atsassets.core.Valuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** The one screen shown at a time. TABS shows the selected tab under the bottom bar. */
enum class Screen {
    LOADING,
    UNREADABLE,
    TABS,
    FORM,
    REVIEW,
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
    val netWorthCents: Long = 0L,
) {
    val holdsData: Boolean get() = CashRules.appHoldsData(entries)

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
                showLoaded(opened.loadCashEntries())
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
        if (current.screen !in TAB_BAR_SCREENS || current.saving) return
        mutableState.update { it.copy(screen = Screen.TABS, tab = tab, refusals = Refusals()) }
    }

    /** After unlock the app shows Overall, unless the form or review is open, which stays (02b decision 9). */
    fun onUnlocked() {
        if (mutableState.value.screen != Screen.TABS) return
        mutableState.update { it.copy(tab = Tab.OVERALL) }
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
        current.copy(screen = Screen.FORM, form = dated.copy(type = type), refusals = Refusals())
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
                    val entries = it.entries + entry
                    it.copy(
                        screen = Screen.TABS,
                        entries = entries,
                        form = FormState(),
                        refusals = Refusals(),
                        saving = false,
                        netWorthCents = netWorthOf(entries),
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

    private fun showLoaded(entries: List<CashEntry>) {
        mutableState.update {
            it.copy(screen = Screen.TABS, entries = entries, netWorthCents = netWorthOf(entries))
        }
    }

    private fun netWorthOf(entries: List<CashEntry>): Long = Valuation.netWorthCents(entries, marketDate.today())

    private companion object {
        const val PROBE_ID = "check"

        // The types the form offers once the opening balance exists (03 decision 2).
        val ENTRY_TYPES = setOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT)

        // Not blank and one line, so the review's second K1 pass judges only the date and balance rules.
        const val PROBE_NOTE = "check"

        // The screens that show the bottom bar (spec 6: always visible; 02b decision 1).
        val TAB_BAR_SCREENS = setOf(Screen.TABS, Screen.FORM, Screen.REVIEW)
    }
}
