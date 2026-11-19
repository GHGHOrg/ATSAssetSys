package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.BackupState
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Tab
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock the test moves: the app reads it only at its moments (32 decision 3). */
private class MovingClock(
    var moment: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = moment
}

// 32 step 3: the backup reminder in the screen state (spec section 9, Backup reminder; AC-18, AC-56; decisions 1 and 3):
// when a backup is due, the dismissal and what ends it, the export from Overall and where its result shows.
@OptIn(ExperimentalCoroutinesApi::class)
class BackupReminderFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val now: Instant = Instant.parse("2026-11-15T09:30:00Z")
    private val week: Duration = Duration.ofDays(7)

    /** An opening balance and one portfolio; [state] is the stored backup state. */
    private fun store(state: BackupState = BackupState()): FakeStore {
        val store = FakeStore(stored = listOf(opening()))
        store.storedPortfolios = listOf(Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L))
        store.storedBackupState = state
        return store
    }

    /** Started at [clock], shown once (MainActivity's first onStart) and unlocked: on Overall. */
    private fun opened(
        store: FakeStore = store(),
        clock: Clock = MovingClock(now),
    ): AppViewModel {
        val vm = started(store, clock = clock, zone = ZoneId.of("UTC"))
        vm.onAppShown()
        vm.onUnlocked()
        return vm
    }

    /** Leaves the app and returns to it: MainActivity's onStop, then its onStart. */
    private fun AppViewModel.leaveAndReturn() {
        onAppHidden()
        onAppShown()
    }

    @Test
    fun neverBackedUp_withData_isDue_andTheReminderShows() {
        val state = opened().state.value
        assertTrue(state.backupDue)
        assertTrue(state.backupReminderShown)
        assertEquals(now, state.now)
    }

    @Test
    fun neverBackedUp_withNoData_isNotDue() {
        val state = opened(FakeStore()).state.value
        assertFalse(state.backupDue)
        assertFalse(state.backupReminderShown)
    }

    // AC-18 and C15: more than 7 days of elapsed time; exactly 7 days is not due.
    @Test
    fun aBackupOlderThan7Days_isDue_andExactly7DaysIsNot() {
        assertFalse(opened(store(BackupState(lastBackup = now.minus(week)))).state.value.backupDue)
        assertTrue(opened(store(BackupState(lastBackup = now.minus(week).minusMillis(1)))).state.value.backupDue)
        assertFalse(opened(store(BackupState(lastBackup = now.minusSeconds(60)))).state.value.backupDue)
    }

    // 36 (decisions 1 and 2): a ViewModel given a shorter time, as the check build's is, uses it in place of the 7 days,
    // read at the same moments (32 decision 3).
    @Test
    fun aShorterTimeGiven_isUsedInPlaceOf7Days() {
        val tenMinutes = Duration.ofMinutes(10)
        val clock = MovingClock(now)
        val vm = started(store(BackupState(lastBackup = now)), clock = clock, zone = ZoneId.of("UTC"), backupDueAfter = tenMinutes)
        vm.onAppShown()
        vm.onUnlocked()
        assertFalse(vm.state.value.backupDue)
        clock.moment = now.plus(tenMinutes)
        vm.leaveAndReturn()
        assertFalse(vm.state.value.backupDue)
        clock.moment = now.plus(tenMinutes).plusMillis(1)
        vm.leaveAndReturn()
        assertTrue(vm.state.value.backupReminderShown)
    }

    // Decision 3: the moment is read at each return to the app and at each unlock, not while the app stays in view.
    @Test
    fun aBackupThatComesDue_showsAtTheNextReturnOrUnlock_notBefore() {
        val clock = MovingClock(now)
        val vm = opened(store(BackupState(lastBackup = now.minus(Duration.ofDays(6)))), clock)
        assertFalse(vm.state.value.backupDue)
        clock.moment = now.plus(Duration.ofDays(2))
        vm.selectTab(Tab.CASH)
        vm.selectTab(Tab.OVERALL)
        assertFalse(vm.state.value.backupDue)
        vm.leaveAndReturn()
        assertTrue(vm.state.value.backupReminderShown)

        val second = MovingClock(now)
        val unlocked = opened(store(BackupState(lastBackup = now.minus(Duration.ofDays(6)))), second)
        second.moment = now.plus(Duration.ofDays(2))
        unlocked.onUnlocked()
        assertTrue(unlocked.state.value.backupReminderShown)
    }

    // Spec section 9: the dismissal lasts until the app is next opened, and is not stored.
    @Test
    fun dismiss_hidesTheReminder_untilTheAppIsNextOpened() {
        val store = store()
        val vm = opened(store)
        vm.dismissBackupReminder()
        assertTrue(vm.state.value.backupDue)
        assertFalse(vm.state.value.backupReminderShown)
        // Moving about the app does not bring it back.
        vm.selectTab(Tab.MORE)
        vm.selectTab(Tab.OVERALL)
        assertFalse(vm.state.value.backupReminderShown)
        // Leaving and returning does.
        vm.leaveAndReturn()
        assertTrue(vm.state.value.backupReminderShown)
        // Nothing was stored for it: a restart shows it too.
        vm.dismissBackupReminder()
        assertTrue(store.backupSaves.isEmpty())
        assertEquals(BackupState(), store.storedBackupState)
        assertTrue(opened(store).state.value.backupReminderShown)
    }

    // A turn of the phone starts the activity again without the app having left: the reminder stays dismissed. So does
    // an unlock in view (an idle lock), which is no opening of the app.
    @Test
    fun aTurnOfThePhone_orAnUnlockInView_keepsTheReminderDismissed() {
        val vm = opened()
        vm.dismissBackupReminder()
        vm.onAppShown()
        assertFalse(vm.state.value.backupReminderShown)
        vm.onUnlocked()
        assertFalse(vm.state.value.backupReminderShown)
    }

    @Test
    fun dismiss_offOverall_orUnderALayer_doesNothing() {
        val vm = opened()
        vm.selectTab(Tab.CASH)
        vm.dismissBackupReminder()
        vm.selectTab(Tab.OVERALL)
        assertTrue(vm.state.value.backupReminderShown)
        vm.openBuySellMenu()
        vm.dismissBackupReminder()
        vm.closeBuySellMenu()
        assertTrue(vm.state.value.backupReminderShown)
        vm.openAllocationMenu()
        vm.dismissBackupReminder()
        assertNull(vm.beginBackup())
        vm.closeAllocationMenu()
        assertTrue(vm.state.value.backupReminderShown)
    }

    // AC-56 and the navigation outline: the reminder's button starts the same export as Backup and restore's.
    @Test
    fun backUpNow_onTheReminder_startsTheExport_fromOverall() {
        val vm = opened()
        assertEquals("backup-2026-11-15.csv", vm.beginBackup())
        val state = vm.state.value
        assertEquals(BackupStage.CHOOSING, state.backupStage)
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.OVERALL, state.tab)
        assertNull(vm.beginBackup())
    }

    @Test
    fun backUpNow_onOverall_withNoReminderShowing_startsNothing() {
        val recent = opened(store(BackupState(lastBackup = now.minusSeconds(60))))
        assertNull(recent.beginBackup())
        val dismissed = opened()
        dismissed.dismissBackupReminder()
        assertNull(dismissed.beginBackup())
        val elsewhere = opened()
        elsewhere.selectTab(Tab.PORTFOLIOS)
        assertNull(elsewhere.beginBackup())
        assertNull(elsewhere.state.value.backupStage)
    }

    // Decision 1: a completed backup ends the reminder, and its result shows on Overall in the reminder's place.
    @Test
    fun aCompletedExport_fromOverall_endsTheReminder_andItsResultStaysOnOverall() {
        val store = store(BackupState(lastBackup = now.minus(Duration.ofDays(30))))
        val clock = MovingClock(now)
        val vm = opened(store, clock)
        val target = FakeTarget()
        vm.beginBackup()
        // The file screen: the app leaves, time passes, it returns with the export still waiting.
        vm.onAppHidden()
        clock.moment = now.plusSeconds(20)
        vm.onAppShown()
        vm.onBackupPlace(target)
        val state = vm.state.value
        assertEquals(BackupOutcome.SAVED, state.backupOutcome)
        assertEquals(now.plusSeconds(20), state.lastBackup)
        assertEquals(now.plusSeconds(20), state.now)
        assertFalse(state.backupDue)
        assertFalse(state.backupReminderShown)
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.OVERALL, state.tab)
        assertTrue(target.text!!.contains("\nformat,"))
        assertEquals(BackupState(lastBackup = now.plusSeconds(20), unfinished = false), store.storedBackupState)
    }

    // Decision 1: a cancelled or failed export leaves the reminder, with the result under it.
    @Test
    fun aCancelledOrFailedExport_fromOverall_keepsTheReminder_withItsResult() {
        val vm = opened()
        vm.beginBackup()
        vm.leaveAndReturn()
        vm.onBackupPlace(null)
        assertEquals(BackupOutcome.NOT_SAVED, vm.state.value.backupOutcome)
        assertTrue(vm.state.value.backupReminderShown)
        // Back up now works again, and the next export's start drops the line.
        assertEquals("backup-2026-11-15.csv", vm.beginBackup())
        assertNull(vm.state.value.backupOutcome)
        vm.onBackupPlace(FakeTarget(writeError = IOException("full"), deletable = false))
        assertEquals(BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN, vm.state.value.backupOutcome)
        assertTrue(vm.state.value.backupReminderShown)
    }

    // The file is complete but its moment could not be stored: the last backup did not move, so a backup is still due.
    @Test
    fun aCompletedExport_whoseMomentCannotBeStored_keepsTheReminder() {
        val store = store()
        store.backupMomentError = IOException("disk")
        val vm = opened(store)
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget())
        assertEquals(BackupOutcome.SAVED_NOT_RECORDED, vm.state.value.backupOutcome)
        assertTrue(vm.state.value.backupReminderShown)
    }

    // Decision 1: the line goes when Overall is left, by any way, and does not come back with Overall.
    @Test
    fun leavingOverall_dropsTheResult() {
        fun withResult(): AppViewModel {
            val vm = opened()
            vm.beginBackup()
            vm.onBackupPlace(FakeTarget())
            assertEquals(BackupOutcome.SAVED, vm.state.value.backupOutcome)
            return vm
        }
        val byTab = withResult()
        byTab.selectTab(Tab.CASH)
        byTab.selectTab(Tab.OVERALL)
        assertNull(byTab.state.value.backupOutcome)

        val byPortfolio = withResult()
        byPortfolio.openPortfolio("p1")
        assertEquals(Screen.PORTFOLIO, byPortfolio.state.value.screen)
        assertNull(byPortfolio.state.value.backupOutcome)
        byPortfolio.closePortfolio()
        assertNull(byPortfolio.state.value.backupOutcome)

        val byNetWorth = withResult()
        byNetWorth.openNetWorth()
        assertEquals(Screen.NET_WORTH, byNetWorth.state.value.screen)
        assertNull(byNetWorth.state.value.backupOutcome)

        val byBuy = withResult()
        byBuy.openBuySellMenu()
        assertEquals(BackupOutcome.SAVED, byBuy.state.value.backupOutcome)
        byBuy.chooseBuy()
        assertEquals(Screen.BUY, byBuy.state.value.screen)
        assertNull(byBuy.state.value.backupOutcome)
    }

    // Decision 1: the line goes at the next return to the app; an unlock alone keeps it, as the file may have been
    // written while the app was locked (31 decision 7).
    @Test
    fun theResultOnOverall_staysThroughAnUnlock_andGoesAtTheNextReturn() {
        val vm = opened()
        vm.beginBackup()
        vm.leaveAndReturn()
        vm.onBackupPlace(FakeTarget())
        vm.onUnlocked()
        assertEquals(BackupOutcome.SAVED, vm.state.value.backupOutcome)
        // A turn of the phone keeps it too.
        vm.onAppShown()
        assertEquals(BackupOutcome.SAVED, vm.state.value.backupOutcome)
        vm.leaveAndReturn()
        assertNull(vm.state.value.backupOutcome)
    }

    // Deploy finding 1: a result that arrives while the app is out of view was never seen, so the return shows it; it
    // goes at the return after that.
    @Test
    fun aResultThatArrivesWhileTheAppIsAway_isShownAtTheReturn_andGoesAtTheNext() {
        val vm = opened()
        val gate = CompletableDeferred<Unit>()
        vm.beginBackup()
        vm.leaveAndReturn()
        vm.onBackupPlace(FakeTarget(writeError = IOException("full"), deletable = false, gate = gate))
        assertEquals(BackupStage.WRITING, vm.state.value.backupStage)
        vm.onAppHidden()
        gate.complete(Unit)
        vm.onAppShown()
        assertEquals(BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN, vm.state.value.backupOutcome)
        assertTrue(vm.state.value.backupReminderShown)
        vm.leaveAndReturn()
        assertNull(vm.state.value.backupOutcome)
    }

    // 31's screen keeps its result across a return to the app; only Overall's line goes then.
    @Test
    fun theResultOnBackupAndRestore_staysAtAReturnToTheApp() {
        val vm = opened()
        vm.selectTab(Tab.MORE)
        vm.openBackup()
        vm.beginBackup()
        vm.leaveAndReturn()
        vm.onBackupPlace(FakeTarget())
        vm.leaveAndReturn()
        assertEquals(BackupOutcome.SAVED, vm.state.value.backupOutcome)
        // And a backup made there ends the reminder on Overall, with no line.
        vm.selectTab(Tab.OVERALL)
        assertFalse(vm.state.value.backupReminderShown)
        assertNull(vm.state.value.backupOutcome)
    }

    // CR2, the wiring: Overall's reminder model follows the state: the banner, the export's line, then the line alone.
    @Test
    fun theReminderModel_followsTheState() {
        val old = now.minus(Duration.ofDays(30))
        val vm = opened(store(BackupState(lastBackup = old)))
        val banner =
            BackupReminderModel(BackupDue.OLD, LastBackup("2026-10-16", "UTC"), canAct = true, canDismiss = true, writing = false, outcome = null)
        assertEquals(banner, vm.state.value.backupReminder)
        vm.beginBackup()
        assertEquals(banner.copy(canAct = false, canDismiss = false), vm.state.value.backupReminder)
        vm.onBackupPlace(null)
        assertEquals(banner.copy(outcome = BackupOutcome.NOT_SAVED), vm.state.value.backupReminder)
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget())
        val saved = banner.copy(due = null, lastBackup = LastBackup("2026-11-15", "UTC"), outcome = BackupOutcome.SAVED)
        assertEquals(saved, vm.state.value.backupReminder)
        vm.selectTab(Tab.CASH)
        vm.selectTab(Tab.OVERALL)
        assertNull(vm.state.value.backupReminder)
        // Dismissed, and with no data: nothing.
        val dismissed = opened()
        dismissed.dismissBackupReminder()
        assertNull(dismissed.state.value.backupReminder)
        assertNull(opened(FakeStore()).state.value.backupReminder)
    }

    @Test
    fun dismiss_dropsTheResultLineWithTheReminder() {
        val vm = opened()
        vm.beginBackup()
        vm.onBackupPlace(null)
        vm.dismissBackupReminder()
        assertFalse(vm.state.value.backupReminderShown)
        assertNull(vm.state.value.backupOutcome)
    }

    // While the file is written nothing else starts, and Dismiss waits; a result that arrives after Overall was left
    // is not kept for a later visit.
    @Test
    fun whileTheFileIsWritten_dismissWaits_andAResultOffOverallIsDropped() {
        val vm = opened()
        val gate = CompletableDeferred<Unit>()
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget(gate = gate))
        assertEquals(BackupStage.WRITING, vm.state.value.backupStage)
        vm.dismissBackupReminder()
        assertTrue(vm.state.value.backupReminderShown)
        vm.openPortfolio("p1")
        gate.complete(Unit)
        assertNull(vm.state.value.backupOutcome)
        assertNull(vm.state.value.backupStage)
        assertFalse(vm.state.value.backupDue)
    }
}
