package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.UnreadableDataException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadWithNoEntriesShowsOverallWithNoData() {
        val vm = started(FakeStore())
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertFalse(state.holdsData)
        assertEquals(0L, state.netWorthCents)
    }

    @Test
    fun loadWithOpeningBalanceShowsNetWorth() {
        val vm = started(FakeStore(stored = listOf(opening())))
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertTrue(state.holdsData)
        assertEquals(1_000_000L, state.netWorthCents)
    }

    @Test
    fun futureDatedOpeningBalanceHoldsDataButNetWorthIsZero() {
        val vm = started(FakeStore(stored = listOf(opening(date = "2026-12-01"))))
        val state = vm.state.value
        assertTrue(state.holdsData)
        assertEquals(0L, state.netWorthCents)
    }

    @Test
    fun unreadableDataShowsTheUnreadableScreen() {
        val vm = started(FakeStore(loadError = UnreadableDataException("unreadable")))
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
        assertFalse(vm.state.value.holdsData)
    }

    @Test
    fun startTwiceCreatesAndLoadsTheStoreOnce() {
        val store = FakeStore()
        var created = 0
        val vm = AppViewModel(
            storeProvider = {
                created++
                store
            },
            clock = clockAt("2026-10-26T12:00:00Z"),
            newId = { "c1" },
        )
        vm.start()
        vm.start()
        assertEquals(1, created)
        assertEquals(1, store.loadCalls)
    }

    @Test
    fun defaultDateIsTodayInNewYorkNotInTokyo() {
        val vm = started(FakeStore(), clockAt("2026-03-01T02:00:00Z", ZoneId.of("Asia/Tokyo")))
        vm.openForm()
        assertEquals(Screen.FORM, vm.state.value.screen)
        assertEquals("2026-02-28", vm.state.value.form.dateText)
    }

    @Test
    fun refusedAmountsGiveTheirReasonAndKeepTheTypedText() {
        val cases = mapOf(
            "" to AmountError.EMPTY,
            "0" to AmountError.ZERO,
            "1.234" to AmountError.TOO_MANY_DECIMALS,
            "abc" to AmountError.BAD_FORMAT,
            "\$5" to AmountError.BAD_FORMAT,
            "1,000" to AmountError.BAD_FORMAT,
            "-5" to AmountError.BAD_FORMAT,
            ".5" to AmountError.BAD_FORMAT,
        )
        for ((text, expected) in cases) {
            val vm = started(FakeStore())
            vm.openForm()
            vm.onAmountChange(text)
            vm.review()
            val state = vm.state.value
            assertEquals("amount '$text'", expected, state.refusals.amount)
            assertEquals("screen for '$text'", Screen.FORM, state.screen)
            assertEquals("kept '$text'", text, state.form.amountText)
        }
    }

    @Test
    fun refusedDatesGiveTheirReasonAndKeepTheTypedText() {
        val cases = mapOf(
            "" to DateRefusal.EMPTY,
            "   " to DateRefusal.EMPTY,
            "2026-02-30" to DateRefusal.NOT_A_DATE,
            "10/05/2026" to DateRefusal.NOT_A_DATE,
        )
        for ((text, expected) in cases) {
            val vm = started(FakeStore())
            vm.openForm()
            vm.fill(date = text, amount = "100.00")
            vm.review()
            val state = vm.state.value
            assertEquals("date '$text'", expected, state.refusals.date)
            assertEquals("screen for '$text'", Screen.FORM, state.screen)
            assertEquals("kept '$text'", text, state.form.dateText)
        }
    }

    @Test
    fun aNoteWithALineBreakIsRefused() {
        val vm = started(FakeStore())
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "100.00", note = "a\nb")
        vm.review()
        assertTrue(vm.state.value.refusals.noteHasLineBreak)
        assertEquals(Screen.FORM, vm.state.value.screen)
    }

    @Test
    fun severalWrongFieldsAreAllReportedTogether() {
        val vm = started(FakeStore())
        vm.openForm()
        vm.fill(date = "nope", amount = "abc", note = "a\nb")
        vm.review()
        val refusals = vm.state.value.refusals
        assertEquals(AmountError.BAD_FORMAT, refusals.amount)
        assertEquals(DateRefusal.NOT_A_DATE, refusals.date)
        assertTrue(refusals.noteHasLineBreak)
        assertEquals(Screen.FORM, vm.state.value.screen)
    }

    @Test
    fun aValidFormGoesToReviewAndSavesNothing() {
        val store = FakeStore()
        val vm = started(store)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        assertEquals(Screen.REVIEW, vm.state.value.screen)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun backToEditKeepsTheTypedText() {
        val vm = started(FakeStore())
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00", note = "Start")
        vm.review()
        vm.backToEdit()
        assertEquals(Screen.FORM, vm.state.value.screen)
        assertEquals(FormState("2026-01-01", "10000.00", "Start"), vm.state.value.form)
    }

    @Test
    fun confirmSavesOneOpeningBalanceAndShowsOverall() {
        val store = FakeStore()
        val vm = started(store)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00", note = "Start")
        vm.review()
        vm.confirm()
        assertEquals(1, store.saved.size)
        val entry = store.saved.single().single()
        assertEquals("c1", entry.id)
        assertEquals(CashType.OPENING_BALANCE, entry.type)
        assertEquals(LocalDate.of(2026, 1, 1), entry.date)
        assertEquals(1_000_000L, entry.amountCents)
        assertEquals("Start", entry.note)
        assertEquals(1L, entry.enteredSeq)
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(1, state.entries.size)
        assertTrue(state.holdsData)
        assertEquals(1_000_000L, state.netWorthCents)
        assertEquals(FormState(), state.form)
    }

    // Replaced in 03 (step 4, your decision): the form opens as a deposit once the opening balance exists
    // (wireframe 10b marker 16), so a second one cannot be started. K1 still refuses it (OpeningBalanceTest).
    @Test
    fun withAnOpeningBalanceASecondOneCannotBeStarted() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.openForm()
        assertEquals(CashType.DEPOSIT, vm.state.value.form.type)
        vm.onTypeChange(CashType.OPENING_BALANCE)
        assertEquals(CashType.DEPOSIT, vm.state.value.form.type)
        vm.fill(date = "2026-01-01", amount = "500.00")
        vm.review()
        vm.confirm()
        val saved = store.saved.single().single()
        assertEquals(CashType.DEPOSIT, saved.type)
        assertEquals(1, vm.state.value.entries.count { it.type == CashType.OPENING_BALANCE })
    }

    @Test
    fun aFailedSaveStaysOnReviewAndChangesNothing() {
        val store = FakeStore(saveError = IllegalStateException("disk"))
        val vm = started(store)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        vm.confirm()
        val state = vm.state.value
        assertEquals(Screen.REVIEW, state.screen)
        assertEquals(SaveRefusal.SAVE_FAILED, state.refusals.save)
        assertTrue(state.entries.isEmpty())
        assertFalse(state.saving)
    }

    @Test
    fun anUnreadableStoreDuringSaveShowsTheUnreadableScreen() {
        val store = FakeStore(saveError = UnreadableDataException("unreadable"))
        val vm = started(store)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        vm.confirm()
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
    }

    @Test
    fun confirmTwiceQuicklySavesOnce() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeStore(gate = gate)
        val vm = started(store)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        vm.confirm()
        vm.confirm()
        gate.complete(Unit)
        assertEquals(1, store.saved.size)
    }

    @Test
    fun confirmOutsideReviewDoesNothing() {
        val store = FakeStore()
        val vm = started(store)
        assertEquals(Screen.TABS, vm.state.value.screen)
        vm.confirm()
        assertTrue(store.saved.isEmpty())
        vm.openForm()
        assertEquals(Screen.FORM, vm.state.value.screen)
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.confirm()
        assertTrue(store.saved.isEmpty())
        assertEquals(Screen.FORM, vm.state.value.screen)
    }

    @Test
    fun closeFormReturnsToOverallAndKeepsTheText() {
        val vm = started(FakeStore())
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "abc", note = "Start")
        vm.review()
        assertEquals(AmountError.BAD_FORMAT, vm.state.value.refusals.amount)
        vm.closeForm()
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(FormState("2026-01-01", "abc", "Start"), state.form)
        assertEquals(Refusals(), state.refusals)
        vm.openForm()
        assertEquals(Screen.FORM, vm.state.value.screen)
        assertEquals(FormState("2026-01-01", "abc", "Start"), vm.state.value.form)
    }

    @Test
    fun closeFormOutsideTheFormDoesNothing() {
        val store = FakeStore()
        val vm = started(store)
        vm.closeForm()
        assertEquals(Screen.TABS, vm.state.value.screen)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        assertEquals(Screen.REVIEW, vm.state.value.screen)
        vm.closeForm()
        assertEquals(Screen.REVIEW, vm.state.value.screen)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun defaultTabOrderIsMoreCashPortfoliosOverall() {
        assertEquals(listOf(Tab.MORE, Tab.CASH, Tab.PORTFOLIOS, Tab.OVERALL), DEFAULT_TAB_ORDER)
    }

    @Test
    fun loadOpensOnTheOverallTab() {
        val state = started(FakeStore(stored = listOf(opening()))).state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.OVERALL, state.tab)
    }

    @Test
    fun selectTabShowsThatTab() {
        val vm = started(FakeStore())
        for (tab in Tab.entries) {
            vm.selectTab(tab)
            assertEquals(Screen.TABS, vm.state.value.screen)
            assertEquals(tab, vm.state.value.tab)
        }
    }

    @Test
    fun theFormOpenedFromCashClosesBackToCash() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.CASH)
        vm.openForm()
        assertEquals(Screen.FORM, vm.state.value.screen)
        vm.closeForm()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.CASH, vm.state.value.tab)
    }

    @Test
    fun confirmFromCashReturnsToCash() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.CASH)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        vm.confirm()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.CASH, vm.state.value.tab)
    }

    @Test
    fun confirmFromOverallReturnsToOverall() {
        val vm = started(FakeStore())
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        vm.confirm()
        assertEquals(Tab.OVERALL, vm.state.value.tab)
    }

    @Test
    fun selectTabFromTheFormKeepsTheTypedTextAndClearsRefusals() {
        val vm = started(FakeStore())
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "abc", note = "Start")
        vm.review()
        vm.selectTab(Tab.MORE)
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.MORE, state.tab)
        assertEquals(FormState("2026-01-01", "abc", "Start"), state.form)
        assertEquals(Refusals(), state.refusals)
    }

    @Test
    fun selectTabFromTheReviewSavesNothingAndKeepsTheText() {
        val store = FakeStore()
        val vm = started(store)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00", note = "Start")
        vm.review()
        vm.selectTab(Tab.PORTFOLIOS)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(FormState("2026-01-01", "10000.00", "Start"), vm.state.value.form)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun backGoesToOverallOnlyFromTheOtherTabs() {
        val vm = started(FakeStore())
        for (tab in listOf(Tab.MORE, Tab.CASH, Tab.PORTFOLIOS)) {
            vm.selectTab(tab)
            assertTrue("back on $tab", vm.state.value.backGoesToOverall)
        }
        vm.selectTab(Tab.OVERALL)
        assertFalse(vm.state.value.backGoesToOverall)
        vm.selectTab(Tab.CASH)
        vm.openForm()
        assertFalse(vm.state.value.backGoesToOverall)
    }

    @Test
    fun unlockOnATabResetsToOverall() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.CASH)
        vm.onUnlocked()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.OVERALL, vm.state.value.tab)
    }

    @Test
    fun unlockWithTheFormOpenKeepsTheFormAndItsTab() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.CASH)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "12")
        vm.onUnlocked()
        assertEquals(Screen.FORM, vm.state.value.screen)
        assertEquals(Tab.CASH, vm.state.value.tab)
        assertEquals("12", vm.state.value.form.amountText)
        vm.review()
        vm.onUnlocked()
        assertEquals(Screen.REVIEW, vm.state.value.screen)
    }

    @Test
    fun selectTabIsIgnoredOnTheUnreadableScreen() {
        val vm = started(FakeStore(loadError = UnreadableDataException("unreadable")))
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
        assertEquals(Tab.OVERALL, vm.state.value.tab)
    }

    @Test
    fun cashTabWithNoEntriesShowsZeroNoRowsAndTheCashEntryButton() {
        val state = started(FakeStore()).state.value
        assertEquals(0L, state.cashBalanceCents)
        assertTrue(state.cashRows.isEmpty())
        assertEquals(CashType.OPENING_BALANCE, state.cashEntryType)
    }

    @Test
    fun cashTabAfterSavingShowsTheBalanceAndOneRowAndTheButtonOpensADeposit() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.CASH)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        vm.confirm()
        val state = vm.state.value
        assertEquals(1_000_000L, state.cashBalanceCents)
        assertEquals(listOf("c1"), state.cashRows.map { it.entry.id })
        assertEquals(1_000_000L, state.cashRows.single().balanceAfterCents)
        assertEquals(CashType.DEPOSIT, state.cashEntryType)
    }

    @Test
    fun futureDatedOpeningBalanceCountsOnTheCashTabButNotInNetWorth() {
        val state = started(FakeStore(stored = listOf(opening(date = "2026-12-01")))).state.value
        assertEquals(1_000_000L, state.cashBalanceCents)
        assertEquals(0L, state.netWorthCents)
        assertEquals(CashType.DEPOSIT, state.cashEntryType)
    }

    @Test
    fun theFormIsTheOpeningBalanceUntilItExists() {
        val vm = started(FakeStore())
        vm.openForm()
        assertEquals(CashType.OPENING_BALANCE, vm.state.value.form.type)
    }

    @Test
    fun withAnOpeningBalanceTheFormOpensAsADeposit() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.selectTab(Tab.CASH)
        vm.openForm()
        assertEquals(CashType.DEPOSIT, vm.state.value.form.type)
    }

    // Narrowed in 04 (your decision, step 3): Adjustment and Interest/dividend can be chosen from 04
    // (theFourTypesCanBeChosen); the opening balance and trade lines stay ignored here.
    @Test
    fun theTypeChangesToWithdrawalAndBack_openingBalanceAndTradeAreIgnored() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        vm.onTypeChange(CashType.WITHDRAWAL)
        assertEquals(CashType.WITHDRAWAL, vm.state.value.form.type)
        for (other in listOf(CashType.OPENING_BALANCE, CashType.TRADE)) {
            vm.onTypeChange(other)
            assertEquals("$other", CashType.WITHDRAWAL, vm.state.value.form.type)
        }
        vm.onTypeChange(CashType.DEPOSIT)
        assertEquals(CashType.DEPOSIT, vm.state.value.form.type)
    }

    @Test
    fun theOpeningBalanceTypeCannotBeChanged() {
        val vm = started(FakeStore())
        vm.openForm()
        vm.onTypeChange(CashType.DEPOSIT)
        assertEquals(CashType.OPENING_BALANCE, vm.state.value.form.type)
    }

    @Test
    fun aDepositIsSavedPositiveWithTheNextSeqAndReturnsToCash() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.selectTab(Tab.CASH)
        vm.openForm()
        vm.fill(date = "2026-01-05", amount = "500.00", note = "Pay")
        vm.review()
        assertEquals(Screen.REVIEW, vm.state.value.screen)
        vm.confirm()
        val entry = store.saved.single().single()
        assertEquals(CashType.DEPOSIT, entry.type)
        assertEquals(50_000L, entry.amountCents)
        assertEquals(2L, entry.enteredSeq)
        assertEquals("Pay", entry.note)
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.CASH, state.tab)
        assertEquals(1_050_000L, state.cashBalanceCents)
        assertEquals(1_050_000L, state.netWorthCents)
    }

    @Test
    fun aWithdrawalIsSavedNegative() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.openForm()
        vm.onTypeChange(CashType.WITHDRAWAL)
        vm.fill(date = "2026-01-05", amount = "300.00")
        vm.review()
        vm.confirm()
        assertEquals(-30_000L, store.saved.single().single().amountCents)
        assertEquals(970_000L, vm.state.value.cashBalanceCents)
    }

    // Example C with a withdrawal in place of the buy (03 file, section 3).
    @Test
    fun exampleC_cashOnly_isRefusedAtReview_keepsTheTextAndSavesNothing() {
        val existing = listOf(opening(cents = 100_000L), withdrawal("2026-01-10", 80_000L, seq = 2L))
        val store = FakeStore(stored = existing)
        val vm = started(store)
        vm.openForm()
        vm.onTypeChange(CashType.WITHDRAWAL)
        vm.fill(date = "2026-01-05", amount = "300.00", note = "Rent")
        vm.review()
        val state = vm.state.value
        assertEquals(Screen.FORM, state.screen)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 10_000L), state.refusals.cash)
        assertEquals(FormState("2026-01-05", "300.00", "Rent", CashType.WITHDRAWAL), state.form)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun aDateBeforeTheOpeningBalanceIsRefusedNamingIt() {
        val vm = started(FakeStore(stored = listOf(opening(date = "2026-01-10"))))
        vm.openForm()
        vm.fill(date = "2026-01-09", amount = "5.00")
        vm.review()
        assertEquals(CashEntryRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 10)), vm.state.value.refusals.cash)
        assertEquals(Screen.FORM, vm.state.value.screen)
    }

    @Test
    fun aDepositWithSeveralWrongFieldsReportsThemTogether() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        vm.fill(date = "nope", amount = "abc", note = "a\nb")
        vm.review()
        val refusals = vm.state.value.refusals
        assertEquals(AmountError.BAD_FORMAT, refusals.amount)
        assertEquals(DateRefusal.NOT_A_DATE, refusals.date)
        assertTrue(refusals.noteHasLineBreak)
        assertEquals(null, refusals.cash)
        assertEquals(Screen.FORM, vm.state.value.screen)
    }

    @Test
    fun aLineBreakAndABlockedWithdrawalAreReportedTogether() {
        val vm = started(FakeStore(stored = listOf(opening(cents = 100_000L))))
        vm.openForm()
        vm.onTypeChange(CashType.WITHDRAWAL)
        vm.fill(date = "2026-01-05", amount = "2000.00", note = "a\nb")
        vm.review()
        val refusals = vm.state.value.refusals
        assertTrue(refusals.noteHasLineBreak)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 5), 100_000L), refusals.cash)
    }

    @Test
    fun aBalanceThatWouldBeTooLargeIsRefused() {
        val vm = started(FakeStore(stored = listOf(opening(cents = Long.MAX_VALUE - 10L))))
        vm.openForm()
        vm.fill(date = "2026-01-05", amount = "1.00")
        vm.review()
        assertEquals(CashEntryRefusal.TooLarge, vm.state.value.refusals.cash)
        assertEquals(Screen.FORM, vm.state.value.screen)
    }

    @Test
    fun editingTheDateOrAmountClearsTheCashRefusal() {
        val vm = started(FakeStore(stored = listOf(opening(date = "2026-01-10"))))
        vm.openForm()
        vm.fill(date = "2026-01-09", amount = "5.00")
        vm.review()
        vm.onDateChange("2026-01-11")
        assertEquals(null, vm.state.value.refusals.cash)
        vm.onDateChange("2026-01-09")
        vm.review()
        vm.onAmountChange("6.00")
        assertEquals(null, vm.state.value.refusals.cash)
    }

    // Iteration 04: interest/dividend and adjustment (04 decisions 1, 2 and 4).
    @Test
    fun theFourTypesCanBeChosen() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        for (type in listOf(CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT, CashType.WITHDRAWAL, CashType.DEPOSIT)) {
            vm.onTypeChange(type)
            assertEquals("$type", type, vm.state.value.form.type)
        }
    }

    @Test
    fun anAdjustmentStartsWithTheInDirection() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        assertEquals(CashDirection.IN, vm.state.value.form.direction)
    }

    @Test
    fun theDirectionIsKeptAcrossATypeSwitch() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.onDirectionChange(CashDirection.OUT)
        vm.onTypeChange(CashType.DEPOSIT)
        vm.onTypeChange(CashType.ADJUSTMENT)
        assertEquals(CashDirection.OUT, vm.state.value.form.direction)
    }

    @Test
    fun theTypeAdjustmentIsKeptWhenTheFormIsClosedAndOpenedAgain() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.closeForm()
        vm.openForm()
        assertEquals(CashType.ADJUSTMENT, vm.state.value.form.type)
    }

    @Test
    fun interestDividendIsSavedPositive() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.openForm()
        vm.onTypeChange(CashType.INTEREST_DIVIDEND)
        vm.fill(date = "2026-01-05", amount = "12.34")
        vm.review()
        vm.confirm()
        val entry = store.saved.single().single()
        assertEquals(CashType.INTEREST_DIVIDEND, entry.type)
        assertEquals(1_234L, entry.amountCents)
    }

    @Test
    fun anAdjustmentInIsSavedPositiveWithItsNote() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.fill(date = "2026-01-05", amount = "20.00", note = "Match broker balance")
        vm.review()
        vm.confirm()
        val entry = store.saved.single().single()
        assertEquals(CashType.ADJUSTMENT, entry.type)
        assertEquals(2_000L, entry.amountCents)
        assertEquals("Match broker balance", entry.note)
    }

    @Test
    fun anAdjustmentOutIsSavedNegative() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.onDirectionChange(CashDirection.OUT)
        vm.fill(date = "2026-01-05", amount = "5.00", note = "Fee")
        vm.review()
        vm.confirm()
        assertEquals(-500L, store.saved.single().single().amountCents)
        assertEquals(999_500L, vm.state.value.cashBalanceCents)
    }

    @Test
    fun aLeftOverOutDirectionDoesNotFlipADeposit() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.onDirectionChange(CashDirection.OUT)
        vm.onTypeChange(CashType.DEPOSIT)
        vm.fill(date = "2026-01-05", amount = "5.00")
        vm.review()
        vm.confirm()
        assertEquals(500L, store.saved.single().single().amountCents)
    }

    @Test
    fun anAdjustmentWithoutANoteIsRefused_keepsTheTextAndSavesNothing() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.fill(date = "2026-01-05", amount = "20.00", note = "   ")
        vm.review()
        val state = vm.state.value
        assertTrue(state.refusals.noteRequired)
        assertEquals(Screen.FORM, state.screen)
        assertEquals("   ", state.form.noteText)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun noteRequiredIsReportedTogetherWithTheAmountAndDate() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.fill(date = "nope", amount = "abc", note = "")
        vm.review()
        val refusals = vm.state.value.refusals
        assertEquals(AmountError.BAD_FORMAT, refusals.amount)
        assertEquals(DateRefusal.NOT_A_DATE, refusals.date)
        assertTrue(refusals.noteRequired)
    }

    @Test
    fun editingTheNoteClearsNoteRequired() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.fill(date = "2026-01-05", amount = "20.00", note = "")
        vm.review()
        vm.onNoteChange("Fix")
        assertFalse(vm.state.value.refusals.noteRequired)
    }

    @Test
    fun anOutgoingAdjustmentThatOverdrawsIsRefused() {
        val vm = started(FakeStore(stored = listOf(opening(cents = 100_000L))))
        vm.openForm()
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.onDirectionChange(CashDirection.OUT)
        vm.fill(date = "2026-01-05", amount = "2000.00", note = "Fix")
        vm.review()
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 5), 100_000L), vm.state.value.refusals.cash)
    }

    // Iteration 05: the cash entry detail and the reversal (spec 4.6; 05 decisions 1 and 4).
    private fun withDeposit(): AppViewModel {
        val vm = started(FakeStore(stored = listOf(opening(), deposit("2026-01-05", 50_000L, seq = 2L))))
        vm.selectTab(Tab.CASH)
        return vm
    }

    @Test
    fun openDetailShowsTheEntryAndOffersReverse() {
        val vm = withDeposit()
        vm.openDetail("d2")
        val state = vm.state.value
        assertEquals(Screen.DETAIL, state.screen)
        assertEquals("d2", state.detailEntry?.id)
        assertTrue(state.reverseOffered)
        assertEquals(null, state.detailReversedBy)
        assertEquals(null, state.detailReverses)
    }

    @Test
    fun theOpeningBalanceDetailDoesNotOfferReverse() {
        val vm = withDeposit()
        vm.openDetail("o1")
        assertEquals(Screen.DETAIL, vm.state.value.screen)
        assertFalse(vm.state.value.reverseOffered)
    }

    @Test
    fun openDetailWithAnUnknownIdIsIgnored() {
        val vm = withDeposit()
        vm.openDetail("nope")
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    @Test
    fun closeDetailReturnsToCash() {
        val vm = withDeposit()
        vm.openDetail("d2")
        vm.closeDetail()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.CASH, vm.state.value.tab)
    }

    @Test
    fun openReversalStartsWithTheOriginalDateAndTheGivenNote() {
        val vm = withDeposit()
        vm.openDetail("d2")
        vm.openReversal("Reversal of the deposit of 2026-01-05 ET")
        val state = vm.state.value
        assertEquals(Screen.REVERSAL, state.screen)
        assertEquals(ReversalForm("2026-01-05", "Reversal of the deposit of 2026-01-05 ET"), state.reversal)
    }

    @Test
    fun openReversalIsIgnoredWhenReverseIsNotOffered() {
        val vm = withDeposit()
        vm.openDetail("o1")
        vm.openReversal("x")
        assertEquals(Screen.DETAIL, vm.state.value.screen)
    }

    @Test
    fun confirmReversalSavesTheOppositeAdjustmentAndReturnsToTheOriginalsDetail() {
        val store = FakeStore(stored = listOf(opening(), deposit("2026-01-05", 50_000L, seq = 2L)))
        val vm = started(store)
        vm.selectTab(Tab.CASH)
        vm.openDetail("d2")
        vm.openReversal("Reversal of the deposit of 2026-01-05 ET")
        vm.confirmReversal()
        val saved = store.saved.single().single()
        assertEquals(CashType.ADJUSTMENT, saved.type)
        assertEquals(-50_000L, saved.amountCents)
        assertEquals("d2", saved.reversesId)
        assertEquals("Reversal of the deposit of 2026-01-05 ET", saved.note)
        val state = vm.state.value
        assertEquals(Screen.DETAIL, state.screen)
        assertEquals("d2", state.detailEntry?.id)
        assertEquals("c1", state.detailReversedBy?.id)
        assertFalse(state.reverseOffered)
        assertEquals(1_000_000L, state.cashBalanceCents)
        assertEquals(ReversalForm(), state.reversal)
    }

    @Test
    fun theReversalsDetailLinksToTheOriginalAndOffersNoReverse() {
        val vm = withDeposit()
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.confirmReversal()
        vm.openDetail("c1")
        val state = vm.state.value
        assertEquals("d2", state.detailReverses?.id)
        assertFalse(state.reverseOffered)
    }

    @Test
    fun aReversalDatedBeforeTheOriginalIsRefused_keepsTheTextAndSavesNothing() {
        val store = FakeStore(stored = listOf(opening(), deposit("2026-01-05", 50_000L, seq = 2L)))
        val vm = started(store)
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.onReversalDateChange("2026-01-04")
        vm.confirmReversal()
        val state = vm.state.value
        assertEquals(Screen.REVERSAL, state.screen)
        assertEquals(CashEntryRefusal.BeforeOriginal(LocalDate.of(2026, 1, 5)), state.refusals.cash)
        assertEquals(ReversalForm("2026-01-04", "Reversal"), state.reversal)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun aBlankReversalNoteIsRefused() {
        val vm = withDeposit()
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.onReversalNoteChange("  ")
        vm.confirmReversal()
        assertTrue(vm.state.value.refusals.noteRequired)
        assertEquals(Screen.REVERSAL, vm.state.value.screen)
    }

    @Test
    fun aBadReversalDateAndALineBreakAreReportedTogether() {
        val vm = withDeposit()
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.onReversalDateChange("nope")
        vm.onReversalNoteChange("a\nb")
        vm.confirmReversal()
        val refusals = vm.state.value.refusals
        assertEquals(DateRefusal.NOT_A_DATE, refusals.date)
        assertTrue(refusals.noteHasLineBreak)
        assertEquals(null, refusals.cash)
    }

    @Test
    fun aReversalThatMakesLaterCashNegativeIsRefused() {
        val stored =
            listOf(
                opening(cents = 100_000L),
                deposit("2026-01-05", 50_000L, seq = 2L),
                withdrawal("2026-01-06", 50_000L, seq = 3L),
                withdrawal("2026-01-10", 80_000L, seq = 4L),
            )
        val vm = started(FakeStore(stored = stored))
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.confirmReversal()
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 30_000L), vm.state.value.refusals.cash)
    }

    @Test
    fun cancelReversalReturnsToTheDetailAndSavesNothing() {
        val store = FakeStore(stored = listOf(opening(), deposit("2026-01-05", 50_000L, seq = 2L)))
        val vm = started(store)
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.cancelReversal()
        assertEquals(Screen.DETAIL, vm.state.value.screen)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun editingTheReversalDateOrNoteClearsItsRefusals() {
        val vm = withDeposit()
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.onReversalDateChange("2026-01-04")
        vm.confirmReversal()
        vm.onReversalDateChange("2026-01-06")
        assertEquals(null, vm.state.value.refusals.cash)
        vm.onReversalNoteChange("")
        vm.confirmReversal()
        vm.onReversalNoteChange("Fix")
        assertFalse(vm.state.value.refusals.noteRequired)
    }

    @Test
    fun theBarWorksFromTheDetailAndTheReversal() {
        val vm = withDeposit()
        vm.openDetail("d2")
        vm.selectTab(Tab.OVERALL)
        assertEquals(Screen.TABS, vm.state.value.screen)
        vm.selectTab(Tab.CASH)
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.selectTab(Tab.MORE)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.MORE, vm.state.value.tab)
    }

    @Test
    fun selectTabIsIgnoredWhileSaving() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeStore(gate = gate)
        val vm = started(store)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.review()
        vm.confirm()
        vm.selectTab(Tab.MORE)
        assertEquals(Screen.REVIEW, vm.state.value.screen)
        gate.complete(Unit)
        assertEquals(Tab.OVERALL, vm.state.value.tab)
        assertEquals(1, store.saved.size)
    }
}

private fun clockAt(
    instant: String,
    zone: ZoneId = ZoneOffset.UTC,
): Clock = Clock.fixed(Instant.parse(instant), zone)

private fun started(
    store: FakeStore,
    clock: Clock = clockAt("2026-10-26T12:00:00Z"),
): AppViewModel {
    val vm = AppViewModel(storeProvider = { store }, clock = clock, newId = { "c1" })
    vm.start()
    return vm
}

private fun AppViewModel.fill(
    date: String,
    amount: String,
    note: String = "",
) {
    onDateChange(date)
    onAmountChange(amount)
    onNoteChange(note)
}

private fun opening(
    date: String = "2026-01-01",
    cents: Long = 1_000_000L,
): CashEntry = CashEntry(
    id = "o1",
    date = LocalDate.parse(date),
    amountCents = cents,
    type = CashType.OPENING_BALANCE,
    note = "",
    enteredSeq = 1L,
)

private fun deposit(
    date: String,
    cents: Long,
    seq: Long,
): CashEntry = CashEntry(
    id = "d$seq",
    date = LocalDate.parse(date),
    amountCents = cents,
    type = CashType.DEPOSIT,
    note = "",
    enteredSeq = seq,
)

private fun withdrawal(
    date: String,
    cents: Long,
    seq: Long,
): CashEntry = CashEntry(
    id = "w$seq",
    date = LocalDate.parse(date),
    amountCents = -cents,
    type = CashType.WITHDRAWAL,
    note = "",
    enteredSeq = seq,
)

/** A store in memory. It records every save, and can fail or wait on demand. */
private class FakeStore(
    var stored: List<CashEntry> = emptyList(),
    val loadError: Throwable? = null,
    val saveError: Throwable? = null,
    val gate: CompletableDeferred<Unit>? = null,
) : RecordStore {
    var loadCalls = 0
    val saved = mutableListOf<List<CashEntry>>()

    override suspend fun loadCashEntries(): List<CashEntry> {
        loadCalls++
        loadError?.let { throw it }
        return stored
    }

    override suspend fun saveCashEntries(entries: List<CashEntry>) {
        gate?.await()
        saveError?.let { throw it }
        saved.add(entries)
        stored = stored + entries
    }
}
