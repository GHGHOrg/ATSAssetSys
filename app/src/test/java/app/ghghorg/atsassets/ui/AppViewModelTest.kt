package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.CashEntry
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
        assertEquals(Screen.OVERALL, state.screen)
        assertFalse(state.holdsData)
        assertEquals(0L, state.netWorthCents)
    }

    @Test
    fun loadWithOpeningBalanceShowsNetWorth() {
        val vm = started(FakeStore(stored = listOf(opening())))
        val state = vm.state.value
        assertEquals(Screen.OVERALL, state.screen)
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
        assertEquals(Screen.OVERALL, state.screen)
        assertEquals(1, state.entries.size)
        assertTrue(state.holdsData)
        assertEquals(1_000_000L, state.netWorthCents)
        assertEquals(FormState(), state.form)
    }

    @Test
    fun reviewIsRefusedWhenAnOpeningBalanceAlreadyExists() {
        val store = FakeStore(stored = listOf(opening()))
        val vm = started(store)
        vm.openForm()
        vm.fill(date = "2026-01-01", amount = "500.00")
        vm.review()
        assertEquals(SaveRefusal.ALREADY_EXISTS, vm.state.value.refusals.save)
        assertEquals(Screen.FORM, vm.state.value.screen)
        assertTrue(store.saved.isEmpty())
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
        assertEquals(Screen.OVERALL, vm.state.value.screen)
        vm.confirm()
        assertTrue(store.saved.isEmpty())
        vm.openForm()
        assertEquals(Screen.FORM, vm.state.value.screen)
        vm.fill(date = "2026-01-01", amount = "10000.00")
        vm.confirm()
        assertTrue(store.saved.isEmpty())
        assertEquals(Screen.FORM, vm.state.value.screen)
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
