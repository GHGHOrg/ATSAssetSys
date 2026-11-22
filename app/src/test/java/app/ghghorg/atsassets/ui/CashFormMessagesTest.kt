package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ReentryRefusal
import app.ghghorg.atsassets.core.ReversalRefusal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

// 15a (CR2): the messages the cash entry form shows at Review, as a model, worked out without drawing the screen.
@OptIn(ExperimentalCoroutinesApi::class)
class CashFormMessagesTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val negative = CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 10_000L)

    @Test
    fun withNoRefusal_noMessageShows() {
        assertEquals(CashFormMessages(date = null, amount = null, note = null, below = emptyList()), CashForm.messages(Refusals(), replacing = false, replaceRule = null))
    }

    // The 05 phone check 5 case: every refusal shows together, each where it belongs.
    @Test
    fun aDateRefusal_aRequiredNote_andTheCashRule_showTogether() {
        val refusals = Refusals(date = DateRefusal.NOT_A_DATE, noteRequired = true, cash = negative)
        assertEquals(
            CashFormMessages(date = DateRefusal.NOT_A_DATE, amount = null, note = NoteMessage.REQUIRED, below = listOf(CashFormMessage.Cash(negative, replacing = false))),
            CashForm.messages(refusals, replacing = false, replaceRule = null),
        )
    }

    @Test
    fun aLineBreak_isShownInPlaceOfARequiredNote() {
        assertEquals(NoteMessage.LINE_BREAK, CashForm.messages(Refusals(noteHasLineBreak = true, noteRequired = true), false, null).note)
        assertEquals(AmountError.ZERO, CashForm.messages(Refusals(amount = AmountError.ZERO), false, null).amount)
    }

    // While replacing (14b): the save message first, then the cash rule naming the replacement, then the pair's rule.
    @Test
    fun whileReplacing_theMessagesBelowAreTheSaveTheReplacementsCashRuleAndThePairsRule_inThatOrder() {
        val rule = ReentryRefusal.OfReversal(ReversalRefusal.NotReversible)
        val refusals = Refusals(save = SaveRefusal.SAVE_FAILED, cash = negative)
        assertEquals(
            listOf(CashFormMessage.Save(SaveRefusal.SAVE_FAILED), CashFormMessage.Cash(negative, replacing = true), CashFormMessage.Replace(rule)),
            CashForm.messages(refusals, replacing = true, replaceRule = rule).below,
        )
        assertEquals(
            listOf(CashFormMessage.Save(SaveRefusal.SAVE_FAILED), CashFormMessage.Cash(negative, replacing = false)),
            CashForm.messages(refusals, replacing = false, replaceRule = rule).below,
        )
    }

    // Wiring (decision 1): an adjustment out of $20,000.00 with no note, on $10,000.00 of cash, at Review.
    @Test
    fun anAdjustmentWithNoNote_thatTakesCashBelowZero_reachesTheStateWithBothMessages() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.onDirectionChange(CashDirection.OUT)
        vm.onDateChange("2026-10-26")
        vm.onAmountChange("20000.00")
        vm.review()
        val messages = vm.state.value.cashFormMessages
        assertEquals(NoteMessage.REQUIRED, messages.note)
        assertEquals(listOf(CashFormMessage.Cash(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 10, 26), 1_000_000L), replacing = false)), messages.below)
    }
}
