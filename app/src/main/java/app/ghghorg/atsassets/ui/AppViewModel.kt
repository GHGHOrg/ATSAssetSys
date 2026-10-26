package app.ghghorg.atsassets.ui

import androidx.lifecycle.ViewModel
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashRules
import app.ghghorg.atsassets.core.RecordStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock

/** The one screen shown at a time (iteration 02a). */
enum class Screen {
    LOADING,
    UNREADABLE,
    OVERALL,
    FORM,
    REVIEW,
}

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
    val entries: List<CashEntry> = emptyList(),
    val form: FormState = FormState(),
    val refusals: Refusals = Refusals(),
    val saving: Boolean = false,
    val netWorthCents: Long = 0L,
) {
    val holdsData: Boolean get() = CashRules.appHoldsData(entries)
}

/**
 * Stub for commit E: every action does nothing, so the tests fail first.
 * The real logic arrives in commit F.
 */
class AppViewModel(
    private val storeProvider: () -> RecordStore,
    private val clock: Clock,
    private val newId: () -> String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = mutableState.asStateFlow()

    fun start() {}

    fun openForm() {}

    fun onDateChange(text: String) {}

    fun onAmountChange(text: String) {}

    fun onNoteChange(text: String) {}

    fun review() {}

    fun backToEdit() {}

    fun confirm() {}
}
