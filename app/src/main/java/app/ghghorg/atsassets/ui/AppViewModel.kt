package app.ghghorg.atsassets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashRules
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

/** The typed text of the opening balance form. Kept as typed, never trimmed or changed. */
data class FormState(
    val dateText: String = "",
    val amountText: String = "",
    val noteText: String = "",
)

/** Reasons only, no wording (D9). A null or false field is fine. */
data class Refusals(
    val amount: AmountError? = null,
    val date: DateRefusal? = null,
    val noteHasLineBreak: Boolean = false,
    val save: SaveRefusal? = null,
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

    /** Android's back button on More, Cash or Portfolios goes to Overall (02b decision 3). */
    val backGoesToOverall: Boolean get() = screen == Screen.TABS && tab != Tab.OVERALL
}

/**
 * The state and actions of the opening balance flow (iteration 02a). It holds the typed form text, so a
 * recreation of the screen keeps it. The store is created on the first [start], after unlock. Refusals
 * are reasons only; their wording is D9 and lives in the screens.
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

    /** Opens the form. The date is filled with today in New York only if it is empty. */
    fun openForm() = mutableState.update { current ->
        val form = current.form
        val filled = if (form.dateText.isEmpty()) form.copy(dateText = marketDate.today().toString()) else form
        current.copy(screen = Screen.FORM, form = filled, refusals = Refusals())
    }

    /** Back from the form to the tab it was opened from. The typed text is kept and the refusals are cleared. Ignored outside FORM. */
    fun closeForm() {
        if (mutableState.value.screen != Screen.FORM) return
        mutableState.update { it.copy(screen = Screen.TABS, refusals = Refusals()) }
    }

    fun onDateChange(text: String) = mutableState.update {
        it.copy(form = it.form.copy(dateText = text), refusals = it.refusals.copy(date = null))
    }

    fun onAmountChange(text: String) = mutableState.update {
        it.copy(form = it.form.copy(amountText = text), refusals = it.refusals.copy(amount = null))
    }

    fun onNoteChange(text: String) = mutableState.update {
        it.copy(form = it.form.copy(noteText = text), refusals = it.refusals.copy(noteHasLineBreak = false))
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
        // A stand-in amount and date let K1 judge the note and "already exists" rules in the same pass.
        val probe =
            OpeningBalance.create(
                existing = current.entries,
                id = PROBE_ID,
                date = date ?: marketDate.today(),
                amountCents = (amount as? AmountResult.Cents)?.value ?: 1L,
                note = form.noteText,
            )
        val rule = (probe as? OpeningBalanceResult.Refused)?.reason
        val refusals = Refusals(
            amount = amountError,
            date = dateRefusal,
            noteHasLineBreak = rule == OpeningBalanceError.NOTE_HAS_LINE_BREAK,
            save = if (rule == OpeningBalanceError.ALREADY_EXISTS) SaveRefusal.ALREADY_EXISTS else null,
        )
        val allFine = amountError == null && dateRefusal == null && rule == null
        mutableState.update {
            it.copy(screen = if (allFine) Screen.REVIEW else Screen.FORM, refusals = refusals)
        }
    }

    /** Back from the review to the form. The typed text is kept. */
    fun backToEdit() {
        val current = mutableState.value
        if (current.screen != Screen.REVIEW || current.saving) return
        mutableState.update { it.copy(screen = Screen.FORM, refusals = Refusals()) }
    }

    /** Saves the opening balance. Ignored outside REVIEW and while a save is running. */
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
        val amount = Money.parseCents(current.form.amountText) as? AmountResult.Cents ?: return null
        val date = MarketDate.parse(current.form.dateText) ?: return null
        val result = OpeningBalance.create(current.entries, newId(), date, amount.value, current.form.noteText)
        return (result as? OpeningBalanceResult.Created)?.entry
    }

    private fun showLoaded(entries: List<CashEntry>) {
        mutableState.update {
            it.copy(screen = Screen.TABS, entries = entries, netWorthCents = netWorthOf(entries))
        }
    }

    private fun netWorthOf(entries: List<CashEntry>): Long = Valuation.netWorthCents(entries, marketDate.today())

    private companion object {
        const val PROBE_ID = "check"

        // The screens that show the bottom bar (spec 6: always visible; 02b decision 1).
        val TAB_BAR_SCREENS = setOf(Screen.TABS, Screen.FORM, Screen.REVIEW)
    }
}
