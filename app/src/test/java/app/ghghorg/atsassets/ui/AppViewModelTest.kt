package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PortfolioRefusal
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.Transaction
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

    // Defect found in 05 phone check 5: a blank note hid the date refusal, because K1 stops at the note.
    @Test
    fun aReversalDateBeforeTheOriginalAndABlankNoteAreReportedTogether() {
        val vm = withDeposit()
        vm.openDetail("d2")
        vm.openReversal("Reversal")
        vm.onReversalDateChange("2026-01-04")
        vm.onReversalNoteChange("")
        vm.confirmReversal()
        val refusals = vm.state.value.refusals
        assertTrue(refusals.noteRequired)
        assertEquals(CashEntryRefusal.BeforeOriginal(LocalDate.of(2026, 1, 5)), refusals.cash)
    }

    @Test
    fun aReversalThatMakesCashNegativeAndABlankNoteAreReportedTogether() {
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
        vm.onReversalNoteChange("  ")
        vm.confirmReversal()
        val refusals = vm.state.value.refusals
        assertTrue(refusals.noteRequired)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 30_000L), refusals.cash)
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

    // Iteration 06: the Cash list filters (view 7; 06 decisions 1 to 4).
    private fun withFourEntries(): AppViewModel {
        val stored =
            listOf(
                opening(),
                deposit("2026-01-05", 50_000L, seq = 2L),
                withdrawal("2026-01-06", 30_000L, seq = 3L),
                deposit("2026-01-10", 5_000L, seq = 4L),
            )
        val vm = started(FakeStore(stored = stored))
        vm.selectTab(Tab.CASH)
        return vm
    }

    private fun AppViewModel.shownIds(): List<String> = state.value.cashFilteredRows.map { it.entry.id }

    @Test
    fun withNoFilterEveryRowIsShownAndNoFilterIsActive() {
        val vm = withFourEntries()
        assertFalse(vm.state.value.filterActive)
        assertEquals(vm.state.value.cashRows, vm.state.value.cashFilteredRows)
    }

    @Test
    fun fromAndToNarrowTheRows() {
        val vm = withFourEntries()
        vm.onFilterFromChange("20260105")
        vm.onFilterToChange("20260106")
        assertTrue(vm.state.value.filterActive)
        assertEquals(listOf("w3", "d2"), vm.shownIds())
    }

    @Test
    fun aTypeChipNarrowsTheRows_andKeepsTheRealBalances() {
        val vm = withFourEntries()
        vm.onFilterTypeChange(CashType.DEPOSIT)
        assertEquals(listOf("d4", "d2"), vm.shownIds())
        assertEquals(listOf(1_025_000L, 1_050_000L), vm.state.value.cashFilteredRows.map { it.balanceAfterCents })
    }

    @Test
    fun allClearsTheChip_andOtherTypesAreIgnored() {
        val vm = withFourEntries()
        vm.onFilterTypeChange(CashType.WITHDRAWAL)
        vm.onFilterTypeChange(CashType.OPENING_BALANCE)
        vm.onFilterTypeChange(CashType.TRADE)
        assertEquals(CashType.WITHDRAWAL, vm.state.value.cashFilterForm.type)
        vm.onFilterTypeChange(null)
        assertEquals(null, vm.state.value.cashFilterForm.type)
        assertFalse(vm.state.value.filterActive)
    }

    @Test
    fun anIncompleteDateIsIgnoredWithoutAMessage() {
        val vm = withFourEntries()
        vm.onFilterFromChange("202601")
        assertEquals(null, vm.state.value.filterFromRefusal)
        assertFalse(vm.state.value.filterActive)
        assertEquals(4, vm.state.value.cashFilteredRows.size)
    }

    @Test
    fun aFullLengthDateThatIsNotADateShowsTheMessageAndIsIgnored() {
        val vm = withFourEntries()
        vm.onFilterFromChange("20261301")
        vm.onFilterToChange("20260230")
        assertEquals(DateRefusal.NOT_A_DATE, vm.state.value.filterFromRefusal)
        assertEquals(DateRefusal.NOT_A_DATE, vm.state.value.filterToRefusal)
        assertFalse(vm.state.value.filterActive)
        assertEquals(4, vm.state.value.cashFilteredRows.size)
    }

    @Test
    fun fromAfterToShowsNoRows() {
        val vm = withFourEntries()
        vm.onFilterFromChange("20260108")
        vm.onFilterToChange("20260105")
        assertTrue(vm.state.value.filterActive)
        assertTrue(vm.state.value.cashFilteredRows.isEmpty())
    }

    @Test
    fun clearFiltersResetsTheDatesAndTheChip() {
        val vm = withFourEntries()
        vm.onFilterFromChange("20260105")
        vm.onFilterToChange("20260106")
        vm.onFilterTypeChange(CashType.DEPOSIT)
        vm.clearFilters()
        assertEquals(CashFilterForm(), vm.state.value.cashFilterForm)
        assertFalse(vm.state.value.filterActive)
        assertEquals(4, vm.state.value.cashFilteredRows.size)
    }

    // Iteration 06, after phone check 12 (your request): digits only, dashes shown by the field, an immediate error for
    // any other character (a change to 06 decision 1).
    @Test
    fun aNonDigitShowsTheDigitsMessageAtOnce_andIsNoBound() {
        val vm = withFourEntries()
        vm.onFilterFromChange("2")
        assertEquals(null, vm.state.value.filterFromRefusal)
        vm.onFilterFromChange("2a")
        assertEquals(DateRefusal.NOT_DIGITS, vm.state.value.filterFromRefusal)
        vm.onFilterToChange("2026-")
        assertEquals(DateRefusal.NOT_DIGITS, vm.state.value.filterToRefusal)
        assertFalse(vm.state.value.filterActive)
        assertEquals(4, vm.state.value.cashFilteredRows.size)
    }

    @Test
    fun fewerThanEightDigitsShowNoMessageAndAreNoBound() {
        val vm = withFourEntries()
        vm.onFilterFromChange("2026010")
        assertEquals(null, vm.state.value.filterFromRefusal)
        assertFalse(vm.state.value.filterActive)
    }

    @Test
    fun aNinthDigitIsNotAccepted() {
        val vm = withFourEntries()
        vm.onFilterFromChange("20260105")
        vm.onFilterFromChange("202601051")
        assertEquals("20260105", vm.state.value.cashFilterForm.fromText)
    }

    @Test
    fun aDatePastedWithDashesIsRefusedAsNotDigits() {
        val vm = withFourEntries()
        vm.onFilterFromChange("2026-01-05")
        assertEquals(DateRefusal.NOT_DIGITS, vm.state.value.filterFromRefusal)
        assertFalse(vm.state.value.filterActive)
    }

    @Test
    fun theFiltersLastAcrossATabSwitchTheDetailAndAnUnlock() {
        val vm = withFourEntries()
        vm.onFilterTypeChange(CashType.DEPOSIT)
        vm.selectTab(Tab.OVERALL)
        vm.selectTab(Tab.CASH)
        vm.openDetail("d2")
        vm.closeDetail()
        vm.onUnlocked()
        vm.selectTab(Tab.CASH)
        assertEquals(CashType.DEPOSIT, vm.state.value.cashFilterForm.type)
        assertEquals(listOf("d4", "d2"), vm.shownIds())
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

    // Iteration 07: create a portfolio (spec 4.7, wireframe 10d frames A and C).

    private fun withOpening(portfolios: List<Portfolio> = emptyList()): Pair<AppViewModel, FakeStore> {
        val store = FakeStore(stored = listOf(opening())).apply { storedPortfolios = portfolios }
        return started(store) to store
    }

    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)

    @Test
    fun thePortfoliosLoadAtStartInCreationOrder() {
        val second = Portfolio(id = "p2", name = "Second", createdSeq = 2L)
        val (vm, _) = withOpening(listOf(second, main))
        assertEquals(listOf(main, second), vm.state.value.portfolios)
    }

    @Test
    fun newPortfolioOpensAnEmptyDialogOverTheTab() {
        val (vm, _) = withOpening()
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolioDialog()
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.PORTFOLIOS, state.tab)
        assertEquals(PortfolioDialog(), state.portfolioDialog)
    }

    @Test
    fun createSavesThePortfolioClosesTheDialogAndKeepsTheTab() {
        val (vm, store) = withOpening()
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("  Main  ")
        vm.createPortfolio()
        val state = vm.state.value
        val saved = Portfolio(id = "c1", name = "Main", createdSeq = 1L)
        assertEquals(listOf(saved), store.savedPortfolios)
        assertEquals(listOf(saved), state.portfolios)
        assertEquals(null, state.portfolioDialog)
        assertEquals(Tab.OVERALL, state.tab)
        assertEquals(Screen.TABS, state.screen)
    }

    @Test
    fun aNewPortfolioIsListedLast() {
        val (vm, _) = withOpening(listOf(main))
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("投資信託")
        vm.createPortfolio()
        assertEquals(listOf("Main", "投資信託"), vm.state.value.portfolios.map { it.name })
    }

    @Test
    fun refusedNamesGiveTheirReasonKeepTheTextAndSaveNothing() {
        val cases = mapOf(
            "投資" to PortfolioRefusal.TooShort,
            "  ab  " to PortfolioRefusal.TooShort,
            "123" to PortfolioRefusal.NoLetter,
            "---" to PortfolioRefusal.NoLetter,
            " main " to PortfolioRefusal.Duplicate("Main"),
            "MAIN" to PortfolioRefusal.Duplicate("Main"),
        )
        for ((text, expected) in cases) {
            val (vm, store) = withOpening(listOf(main))
            vm.openPortfolioDialog()
            vm.onPortfolioNameChange(text)
            vm.createPortfolio()
            val dialog = vm.state.value.portfolioDialog
            assertEquals("refusal for '$text'", expected, dialog?.refusal)
            assertEquals("kept '$text'", text, dialog?.nameText)
            assertEquals("saved for '$text'", emptyList<Portfolio>(), store.savedPortfolios)
        }
    }

    @Test
    fun typingClearsTheRefusal() {
        val (vm, _) = withOpening()
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("ab")
        vm.createPortfolio()
        vm.onPortfolioNameChange("abc")
        assertEquals(PortfolioDialog(nameText = "abc"), vm.state.value.portfolioDialog)
    }

    // Decision 4: a line break, typed or pasted, becomes a space.
    @Test
    fun aLineBreakInTheNameBecomesASpace() {
        val (vm, _) = withOpening()
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("Main\nAccount\r\nTwo\rThree")
        assertEquals("Main Account Two Three", vm.state.value.portfolioDialog?.nameText)
    }

    @Test
    fun cancelClosesTheDialogAndSavesNothing() {
        val (vm, store) = withOpening()
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("Main")
        vm.cancelPortfolioDialog()
        assertEquals(null, vm.state.value.portfolioDialog)
        assertEquals(emptyList<Portfolio>(), store.savedPortfolios)
        vm.openPortfolioDialog()
        assertEquals(PortfolioDialog(), vm.state.value.portfolioDialog)
    }

    // Decision 3: unit tested here; the phone check is open item 33.
    @Test
    fun beforeTheOpeningBalanceNewPortfolioAsksForItInsteadOfTheDialog() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolioDialog()
        val state = vm.state.value
        assertEquals(null, state.portfolioDialog)
        assertTrue(state.portfolioNeedsOpeningBalance)
    }

    @Test
    fun theOpeningBalanceMessageGoesAwayOnATabSwitch() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolioDialog()
        vm.selectTab(Tab.OVERALL)
        assertFalse(vm.state.value.portfolioNeedsOpeningBalance)
    }

    @Test
    fun aFailedSaveKeepsTheDialogAndTheText() {
        val store = FakeStore(stored = listOf(opening()), saveError = IllegalStateException("disk"))
        val vm = started(store)
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("Main")
        vm.createPortfolio()
        val state = vm.state.value
        assertEquals(PortfolioDialog(nameText = "Main", save = SaveRefusal.SAVE_FAILED), state.portfolioDialog)
        assertEquals(emptyList<Portfolio>(), state.portfolios)
        assertFalse(state.saving)
    }

    @Test
    fun unreadableDataOnSaveShowsTheUnreadableScreen() {
        val store = FakeStore(stored = listOf(opening()), saveError = UnreadableDataException("unreadable"))
        val vm = started(store)
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("Main")
        vm.createPortfolio()
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
    }

    @Test
    fun createAndCancelAreIgnoredWhileSaving() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeStore(stored = listOf(opening()), gate = gate)
        val vm = started(store)
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("Main")
        vm.createPortfolio()
        vm.createPortfolio()
        vm.cancelPortfolioDialog()
        assertTrue(vm.state.value.saving)
        gate.complete(Unit)
        assertEquals(1, store.savedPortfolios.size)
        assertEquals(null, vm.state.value.portfolioDialog)
    }

    @Test
    fun anUnlockKeepsTheOpenDialogAndItsTab() {
        val (vm, _) = withOpening()
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("Ma")
        vm.onUnlocked()
        val state = vm.state.value
        assertEquals(Tab.PORTFOLIOS, state.tab)
        assertEquals("Ma", state.portfolioDialog?.nameText)
    }

    // Iteration 08: the portfolio screen and rename (spec 4.7, wireframes 3 frame B and 10d frame B).

    private val trust = Portfolio(id = "p2", name = "投資信託", createdSeq = 2L)

    private fun onMain(): Pair<AppViewModel, FakeStore> {
        val (vm, store) = withOpening(listOf(main, trust))
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        return vm to store
    }

    private fun renamingMain(): Pair<AppViewModel, FakeStore> {
        val (vm, store) = onMain()
        vm.openPortfolioMenu()
        vm.openRename()
        return vm to store
    }

    @Test
    fun aRowOpensThePortfolioScreen() {
        val (vm, _) = onMain()
        val state = vm.state.value
        assertEquals(Screen.PORTFOLIO, state.screen)
        assertEquals(Tab.PORTFOLIOS, state.tab)
        assertEquals(main, state.shownPortfolio)
        assertFalse(state.portfolioMenuOpen)
    }

    @Test
    fun anUnknownPortfolioIsIgnored() {
        val (vm, _) = withOpening(listOf(main))
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("missing")
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    @Test
    fun backFromThePortfolioScreenGoesToTheList() {
        val (vm, _) = onMain()
        vm.closePortfolio()
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.PORTFOLIOS, state.tab)
        assertEquals(null, state.shownPortfolio)
    }

    @Test
    fun theMenuOpensAndCloses() {
        val (vm, _) = onMain()
        vm.openPortfolioMenu()
        assertTrue(vm.state.value.portfolioMenuOpen)
        vm.closePortfolioMenu()
        assertFalse(vm.state.value.portfolioMenuOpen)
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
    }

    // Wireframe 10d frame B: the field starts with the current name.
    @Test
    fun renameClosesTheMenuAndOpensTheDialogWithTheCurrentName() {
        val (vm, _) = renamingMain()
        val state = vm.state.value
        assertFalse(state.portfolioMenuOpen)
        assertEquals(PortfolioDialog(nameText = "Main"), state.renameDialog)
    }

    @Test
    fun saveRenamesOnTheScreenAndInTheListInTheSamePlace() {
        val (vm, store) = renamingMain()
        vm.onRenameNameChange("  Main Account ")
        vm.saveRename()
        val state = vm.state.value
        assertEquals(listOf("p1" to "Main Account"), store.renames)
        assertEquals(listOf("Main Account", "投資信託"), state.portfolios.map { it.name })
        assertEquals("Main Account", state.shownPortfolio?.name)
        assertEquals(null, state.renameDialog)
        assertEquals(Screen.PORTFOLIO, state.screen)
        assertFalse(state.saving)
    }

    // Criterion 55: a change of case only is accepted on rename.
    @Test
    fun aChangeOfCaseOnlyIsSaved() {
        val (vm, store) = renamingMain()
        vm.onRenameNameChange("MAIN")
        vm.saveRename()
        assertEquals(listOf("p1" to "MAIN"), store.renames)
        assertEquals("MAIN", vm.state.value.shownPortfolio?.name)
    }

    // Wireframe 10d marker 4: saving the unchanged name closes the dialog and changes nothing.
    @Test
    fun theUnchangedNameClosesTheDialogWithNoWrite() {
        val (vm, store) = renamingMain()
        vm.onRenameNameChange(" Main ")
        vm.saveRename()
        assertEquals(null, vm.state.value.renameDialog)
        assertEquals(emptyList<Pair<String, String>>(), store.renames)
    }

    @Test
    fun refusedRenamesGiveTheirReasonKeepTheTextAndSaveNothing() {
        val cases = mapOf(
            "投資信託" to PortfolioRefusal.Duplicate("投資信託"),
            "投資" to PortfolioRefusal.TooShort,
            "  ab  " to PortfolioRefusal.TooShort,
            "123" to PortfolioRefusal.NoLetter,
            "---" to PortfolioRefusal.NoLetter,
        )
        for ((text, expected) in cases) {
            val (vm, store) = renamingMain()
            vm.onRenameNameChange(text)
            vm.saveRename()
            val dialog = vm.state.value.renameDialog
            assertEquals("refusal for '$text'", expected, dialog?.refusal)
            assertEquals("kept '$text'", text, dialog?.nameText)
            assertEquals("saved for '$text'", emptyList<Pair<String, String>>(), store.renames)
        }
    }

    @Test
    fun typingInRenameClearsTheRefusalAndALineBreakBecomesASpace() {
        val (vm, _) = renamingMain()
        vm.onRenameNameChange("ab")
        vm.saveRename()
        vm.onRenameNameChange("Main\nAccount")
        assertEquals(PortfolioDialog(nameText = "Main Account"), vm.state.value.renameDialog)
    }

    @Test
    fun cancelRenameClosesTheDialogAndSavesNothing() {
        val (vm, store) = renamingMain()
        vm.onRenameNameChange("Other")
        vm.cancelRename()
        val state = vm.state.value
        assertEquals(null, state.renameDialog)
        assertEquals("Main", state.shownPortfolio?.name)
        assertEquals(emptyList<Pair<String, String>>(), store.renames)
    }

    @Test
    fun aFailedRenameKeepsTheDialogAndTheText() {
        val store = FakeStore(stored = listOf(opening()), saveError = IllegalStateException("disk"))
        store.storedPortfolios = listOf(main)
        val vm = started(store)
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openRename()
        vm.onRenameNameChange("Main Account")
        vm.saveRename()
        val state = vm.state.value
        assertEquals(PortfolioDialog(nameText = "Main Account", save = SaveRefusal.SAVE_FAILED), state.renameDialog)
        assertEquals("Main", state.shownPortfolio?.name)
        assertFalse(state.saving)
    }

    @Test
    fun unreadableDataOnRenameShowsTheUnreadableScreen() {
        val store = FakeStore(stored = listOf(opening()), saveError = UnreadableDataException("unreadable"))
        store.storedPortfolios = listOf(main)
        val vm = started(store)
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openRename()
        vm.onRenameNameChange("Main Account")
        vm.saveRename()
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
    }

    @Test
    fun saveCancelAndBackAreIgnoredWhileSavingARename() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeStore(stored = listOf(opening()), gate = gate)
        store.storedPortfolios = listOf(main)
        val vm = started(store)
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openRename()
        vm.onRenameNameChange("Main Account")
        vm.saveRename()
        vm.saveRename()
        vm.cancelRename()
        vm.closePortfolio()
        assertTrue(vm.state.value.saving)
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
        gate.complete(Unit)
        assertEquals(1, store.renames.size)
        assertEquals(null, vm.state.value.renameDialog)
    }

    @Test
    fun anUnlockKeepsThePortfolioScreenAndTheRenameDialog() {
        val (vm, _) = renamingMain()
        vm.onRenameNameChange("Mai")
        vm.onUnlocked()
        val state = vm.state.value
        assertEquals(Screen.PORTFOLIO, state.screen)
        assertEquals("Mai", state.renameDialog?.nameText)
    }

    @Test
    fun aTabFromThePortfolioScreenLeavesIt() {
        val (vm, _) = onMain()
        vm.openPortfolioMenu()
        vm.selectTab(Tab.CASH)
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.CASH, state.tab)
        assertEquals(null, state.shownPortfolio)
        assertFalse(state.portfolioMenuOpen)
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
    var storedPortfolios: List<Portfolio> = emptyList()
    val savedPortfolios = mutableListOf<Portfolio>()
    val renames = mutableListOf<Pair<String, String>>()

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

    override suspend fun loadPortfolios(): List<Portfolio> {
        loadError?.let { throw it }
        return storedPortfolios
    }

    override suspend fun savePortfolio(portfolio: Portfolio) {
        gate?.await()
        saveError?.let { throw it }
        savedPortfolios.add(portfolio)
        storedPortfolios = storedPortfolios + portfolio
    }

    override suspend fun renamePortfolio(
        id: String,
        name: String,
    ) {
        gate?.await()
        saveError?.let { throw it }
        renames.add(id to name)
        storedPortfolios = storedPortfolios.map { if (it.id == id) it.copy(name = name) else it }
    }

    var storedTransactions: List<Transaction> = emptyList()
    val savedBuys = mutableListOf<Pair<Transaction, CashEntry>>()
    var storedInstruments: List<Instrument> = emptyList()
    val savedInstruments = mutableListOf<Instrument>()

    override suspend fun loadTransactions(): List<Transaction> {
        loadError?.let { throw it }
        return storedTransactions
    }

    /** Both or neither, as the Room store: a failure saves nothing. */
    override suspend fun saveBuy(
        transaction: Transaction,
        cashLine: CashEntry,
    ) {
        gate?.await()
        saveError?.let { throw it }
        savedBuys.add(transaction to cashLine)
        storedTransactions = storedTransactions + transaction
        stored = stored + cashLine
    }

    override suspend fun loadInstruments(): List<Instrument> {
        loadError?.let { throw it }
        return storedInstruments
    }

    override suspend fun saveInstrument(instrument: Instrument) {
        saveError?.let { throw it }
        savedInstruments.add(instrument)
        storedInstruments = storedInstruments.filter { it.ticker != instrument.ticker } + instrument
    }
}
