package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.backup.BackupTarget
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.BackupFile
import app.ghghorg.atsassets.core.BackupRecords
import app.ghghorg.atsassets.core.BackupResult
import app.ghghorg.atsassets.core.BackupState
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A place for a backup file in memory: it keeps what was written, and can fail, wait or refuse to delete on demand. */
internal class FakeTarget(
    var writeError: Throwable? = null,
    var deletable: Boolean = true,
    val gate: CompletableDeferred<Unit>? = null,
) : BackupTarget {
    var text: String? = null
    var writes = 0
    var deletes = 0

    override suspend fun write(text: String) {
        writes++
        gate?.await()
        writeError?.let { throw it }
        this.text = text
    }

    override suspend fun delete(): Boolean {
        deletes++
        if (deletable) text = null
        return deletable
    }
}

// 31 step 4: Back up now in the screen state (spec sections 8 and 9; decisions 4, 6 and 7): the file written from the
// stored records to the place chosen, the mark around the write, and the last backup moved only by a completed write.
@OptIn(ExperimentalCoroutinesApi::class)
class BackupFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val now = "2026-10-09T16:30:00Z"
    private val earlier: Instant = Instant.parse("2026-10-04T03:00:00Z")

    private val buy =
        Transaction("b1", "p1", "AAPL", TransactionType.BUY, LocalDate.parse("2026-01-02"), 2L, BigDecimal.TEN, 100_000L)

    /** One portfolio holding AAPL; [state] is the stored backup state. */
    private fun store(state: BackupState = BackupState()): FakeStore {
        val line = CashEntry("c-b1", buy.date, -buy.totalCents, CashType.TRADE, "", buy.enteredSeq, transactionId = buy.id)
        val store = FakeStore(stored = listOf(opening(), line))
        store.storedPortfolios = listOf(Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L))
        store.storedTransactions = listOf(buy)
        store.storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true))
        store.storedBackupState = state
        return store
    }

    /** Started at [now], then More and its Backup and restore entry. */
    private fun onBackup(
        store: FakeStore = store(),
        zone: ZoneId = ZoneId.of("UTC"),
    ): AppViewModel {
        val vm = started(store, clock = clockAt(now), zone = zone)
        vm.selectTab(Tab.MORE)
        vm.openBackup()
        return vm
    }

    /** The file the writer makes of [store]'s records, with the test source's word. */
    private fun fileOf(store: FakeStore): String {
        val records =
            BackupRecords(
                store.stored,
                store.storedPortfolios,
                store.storedTransactions,
                store.storedHidden,
                store.storedOverrides,
                store.storedClassLists,
                store.storedAutoLock ?: AutoLock.EVERY_LEAVE,
                store.storedTabOrder ?: TabOrder.DEFAULT,
                "testsource",
            )
        return (BackupFile.write(records) as BackupResult.Written).text
    }

    @Test
    fun atStart_neverBackedUp_andNoMark() {
        val state = started(store()).state.value
        assertNull(state.lastBackup)
        assertFalse(state.backupInterrupted)
        assertNull(state.backupStage)
        assertNull(state.backupOutcome)
    }

    @Test
    fun atStart_theStoredMoment_isTheLastBackup() {
        val state = started(store(BackupState(lastBackup = earlier))).state.value
        assertEquals(earlier, state.lastBackup)
        assertFalse(state.backupInterrupted)
    }

    // Decision 7: a start that finds the mark knows a backup's write never ended.
    @Test
    fun atStart_theStoredMark_isAnInterruptedBackup() {
        val state = started(store(BackupState(lastBackup = earlier, unfinished = true))).state.value
        assertTrue(state.backupInterrupted)
        assertEquals(earlier, state.lastBackup)
    }

    @Test
    fun moresEntry_opensBackupAndRestore_andBackReturnsToMore() {
        val vm = started(store())
        vm.openBackup()
        assertEquals(Screen.TABS, vm.state.value.screen)
        vm.selectTab(Tab.MORE)
        vm.openBackup()
        assertEquals(Screen.BACKUP, vm.state.value.screen)
        assertEquals(Tab.MORE, vm.state.value.tab)
        vm.closeBackup()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.MORE, vm.state.value.tab)
    }

    // Section 8 and C15: the suggested name carries today's date in the device's zone, not New York's or UTC's.
    @Test
    fun backUpNow_givesTheSuggestedName_inTheDevicesZone_andWaitsForThePlace() {
        val vm = onBackup(zone = ZoneId.of("Asia/Tokyo"))
        assertEquals("backup-2026-10-10.csv", vm.beginBackup())
        assertEquals(BackupStage.CHOOSING, vm.state.value.backupStage)
        assertFalse(vm.state.value.saving)
        assertEquals("backup-2026-10-09.csv", onBackup(zone = ZoneId.of("America/New_York")).beginBackup())
    }

    // Decision 6: an app with no data starts no export; such a file could never be restored.
    @Test
    fun backUpNow_withNoData_startsNothing() {
        val vm = started(FakeStore(), clock = clockAt(now))
        vm.selectTab(Tab.MORE)
        vm.openBackup()
        assertEquals(Screen.BACKUP, vm.state.value.screen)
        assertNull(vm.beginBackup())
        assertNull(vm.state.value.backupStage)
    }

    // From 32 Overall's reminder starts an export too (BackupReminderFlowTest), so "off the screen" here is Overall with
    // a recent backup, where no reminder shows, and a tab that never has one.
    @Test
    fun backUpNow_offTheScreen_orWithOneUnderWay_startsNothing() {
        val elsewhere = started(store(BackupState(lastBackup = Instant.parse(now).minusSeconds(60))), clock = clockAt(now))
        assertNull(elsewhere.beginBackup())
        elsewhere.selectTab(Tab.MORE)
        assertNull(elsewhere.beginBackup())
        val noBackupYet = started(store(), clock = clockAt(now))
        noBackupYet.selectTab(Tab.CASH)
        assertNull(noBackupYet.beginBackup())
        val vm = onBackup()
        assertEquals("backup-2026-10-09.csv", vm.beginBackup())
        assertNull(vm.beginBackup())
        assertEquals(BackupStage.CHOOSING, vm.state.value.backupStage)
    }

    // The file is what K6 makes of the stored records, with the source's own word; the moment of the write is the last
    // backup, and the result says so.
    @Test
    fun aChosenPlace_getsTheStoredRecordsAsTheFile_andTheLastBackupMoves() {
        val store = store()
        val vm = onBackup(store)
        val target = FakeTarget()
        vm.beginBackup()
        vm.onBackupPlace(target)
        assertEquals(fileOf(store), target.text)
        assertTrue(target.text!!.contains("setting,,,,,,,,,,,,,,,,price_source,testsource,\n"))
        assertTrue(target.text!!.contains("buy,L1,Portfolio P,2026-01-02,AAPL,10,1000.00,"))
        assertEquals(1, target.writes)
        assertEquals(0, target.deletes)
        val state = vm.state.value
        assertEquals(Instant.parse(now), state.lastBackup)
        assertEquals(BackupOutcome.SAVED, state.backupOutcome)
        assertNull(state.backupStage)
        assertFalse(state.saving)
        assertEquals(listOf("started" to null, "ended" to Instant.parse(now)), store.backupSaves)
        assertEquals(BackupState(lastBackup = Instant.parse(now), unfinished = false), store.storedBackupState)
    }

    // Decision 7 and spec 9: the mark is stored before the write begins, and the moment only once it has ended; while
    // it runs the button is off (the stage), every other save waits, and Back does not leave.
    @Test
    fun whileTheFileIsWritten_theMarkIsStored_theMomentIsNot_andNothingElseStarts() {
        val store = store(BackupState(lastBackup = earlier))
        val vm = onBackup(store)
        val gate = CompletableDeferred<Unit>()
        val target = FakeTarget(gate = gate)
        vm.beginBackup()
        vm.onBackupPlace(target)
        assertEquals(BackupStage.WRITING, vm.state.value.backupStage)
        assertTrue(vm.state.value.saving)
        assertEquals(listOf<Pair<String, Instant?>>("started" to null), store.backupSaves)
        assertTrue(store.storedBackupState.unfinished)
        assertEquals(earlier, vm.state.value.lastBackup)
        assertNull(vm.beginBackup())
        vm.closeBackup()
        vm.selectTab(Tab.OVERALL)
        assertEquals(Screen.BACKUP, vm.state.value.screen)
        gate.complete(Unit)
        assertEquals(BackupOutcome.SAVED, vm.state.value.backupOutcome)
        assertEquals(Instant.parse(now), vm.state.value.lastBackup)
        assertFalse(store.storedBackupState.unfinished)
    }

    // Spec 9 and AC-18: a cancelled export says so and changes nothing; nothing is stored at all.
    @Test
    fun aCancelledFileScreen_changesNothing_andSaysNotSaved() {
        val store = store(BackupState(lastBackup = earlier))
        val vm = onBackup(store)
        vm.beginBackup()
        vm.onBackupPlace(null)
        val state = vm.state.value
        assertEquals(BackupOutcome.NOT_SAVED, state.backupOutcome)
        assertEquals(earlier, state.lastBackup)
        assertNull(state.backupStage)
        assertFalse(state.saving)
        assertTrue(store.backupSaves.isEmpty())
        // And Back up now works again.
        assertEquals("backup-2026-10-09.csv", vm.beginBackup())
        assertNull(vm.state.value.backupOutcome)
    }

    // Decision 4: a failed write deletes the unfinished file, removes the mark and leaves the last backup as it was.
    @Test
    fun aFailedWrite_deletesTheFile_removesTheMark_andKeepsTheLastBackup() {
        val store = store(BackupState(lastBackup = earlier))
        val vm = onBackup(store)
        val target = FakeTarget(writeError = IOException("full"))
        vm.beginBackup()
        vm.onBackupPlace(target)
        val state = vm.state.value
        assertEquals(BackupOutcome.NOT_SAVED, state.backupOutcome)
        assertEquals(earlier, state.lastBackup)
        assertFalse(state.backupInterrupted)
        assertFalse(state.saving)
        assertNull(state.backupStage)
        assertEquals(1, target.deletes)
        assertEquals(listOf<Pair<String, Instant?>>("started" to null, "ended" to null), store.backupSaves)
        assertEquals(BackupState(lastBackup = earlier, unfinished = false), store.storedBackupState)
    }

    // Decision 4: where the place does not allow deleting, the result says a file may remain.
    @Test
    fun aFailedWrite_whoseFileCannotBeDeleted_saysAFileMayRemain() {
        val vm = onBackup()
        val target = FakeTarget(writeError = IOException("gone"), deletable = false)
        vm.beginBackup()
        vm.onBackupPlace(target)
        assertEquals(BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN, vm.state.value.backupOutcome)
        assertEquals(1, target.deletes)
        assertNull(vm.state.value.lastBackup)
    }

    // Decision 7: a write that never ends (a force stop, a crash, power loss) leaves the mark, and the next start shows it.
    @Test
    fun aWriteThatNeverEnds_leavesTheMark_andTheNextStartKnows() {
        val store = store(BackupState(lastBackup = earlier))
        val vm = onBackup(store)
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget(gate = CompletableDeferred()))
        assertEquals(BackupState(lastBackup = earlier, unfinished = true), store.storedBackupState)
        val next = started(store, clock = clockAt(now))
        assertTrue(next.state.value.backupInterrupted)
        assertEquals(earlier, next.state.value.lastBackup)
    }

    // Decision 7: the warning stands until a backup completes; a failed one keeps it, in the state and in the store.
    @Test
    fun afterAnInterruptedBackup_aFailedWrite_keepsTheWarning() {
        val store = store(BackupState(lastBackup = earlier, unfinished = true))
        val vm = onBackup(store)
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget(writeError = IOException("full")))
        assertEquals(BackupOutcome.NOT_SAVED, vm.state.value.backupOutcome)
        assertTrue(vm.state.value.backupInterrupted)
        assertTrue(store.storedBackupState.unfinished)
        assertEquals(listOf<Pair<String, Instant?>>("started" to null), store.backupSaves)
    }

    @Test
    fun afterAnInterruptedBackup_aCompletedWrite_endsTheWarning() {
        val store = store(BackupState(lastBackup = earlier, unfinished = true))
        val vm = onBackup(store)
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget())
        assertFalse(vm.state.value.backupInterrupted)
        assertEquals(Instant.parse(now), vm.state.value.lastBackup)
        assertEquals(BackupState(lastBackup = Instant.parse(now), unfinished = false), store.storedBackupState)
    }

    // Decision 7: a place that arrives with no export waiting (Android restarted the app while the file screen was open)
    // gets nothing written; the empty file is deleted where possible and the last backup does not move.
    @Test
    fun aPlaceWithNoExportWaiting_isDeleted_andNothingIsWritten() {
        val store = store(BackupState(lastBackup = earlier))
        val vm = started(store, clock = clockAt(now))
        val target = FakeTarget()
        vm.onBackupPlace(target)
        assertEquals(0, target.writes)
        assertEquals(1, target.deletes)
        assertTrue(store.backupSaves.isEmpty())
        assertNull(vm.state.value.backupOutcome)
        assertEquals(earlier, vm.state.value.lastBackup)
        // Before the records are loaded too: the app is still at its lock screen.
        val unstarted =
            AppViewModel(storeProvider = { store }, clock = clockAt(now), newId = { "x" }, priceSource = FakeSource(), isOnline = { true })
        val second = FakeTarget()
        unstarted.onBackupPlace(second)
        assertEquals(0, second.writes)
        assertEquals(1, second.deletes)
    }

    // The file is complete but its moment could not be stored: the result says so, the last backup stays, the file too.
    @Test
    fun aCompletedWrite_whoseMomentCannotBeStored_saysSo_andKeepsTheFile() {
        val store = store(BackupState(lastBackup = earlier))
        store.backupEndError = IOException("disk")
        val vm = onBackup(store)
        val target = FakeTarget()
        vm.beginBackup()
        vm.onBackupPlace(target)
        val state = vm.state.value
        assertEquals(BackupOutcome.SAVED_NOT_RECORDED, state.backupOutcome)
        assertEquals(earlier, state.lastBackup)
        assertEquals(fileOf(store), target.text)
        assertEquals(0, target.deletes)
        assertFalse(state.saving)
        assertNull(state.backupStage)
    }

    // Deploy finding 1: the file is complete but its moment could not be stored. The mark is still removed, so the next
    // start does not call that complete file unfinished; the last backup stays as it was.
    @Test
    fun aCompletedWrite_whoseMomentCannotBeStored_stillRemovesTheMark() {
        val store = store(BackupState(lastBackup = earlier))
        store.backupMomentError = IOException("disk")
        val vm = onBackup(store)
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget())
        assertEquals(BackupOutcome.SAVED_NOT_RECORDED, vm.state.value.backupOutcome)
        assertEquals(BackupState(lastBackup = earlier, unfinished = false), store.storedBackupState)
        val next = started(store, clock = clockAt(now))
        assertFalse(next.state.value.backupInterrupted)
        assertEquals(earlier, next.state.value.lastBackup)
    }

    // The mark cannot be stored: nothing is written, the empty file is deleted, and the result is "not saved".
    @Test
    fun whenTheMarkCannotBeStored_nothingIsWritten() {
        val store = store()
        store.backupSaveError = IOException("disk")
        val vm = onBackup(store)
        val target = FakeTarget()
        vm.beginBackup()
        vm.onBackupPlace(target)
        assertEquals(BackupOutcome.NOT_SAVED, vm.state.value.backupOutcome)
        assertEquals(0, target.writes)
        assertEquals(1, target.deletes)
        assertNull(vm.state.value.lastBackup)
        assertFalse(vm.state.value.saving)
    }

    // Stored data that cannot be read ends on the unreadable-data screen, as every other save; the empty file is deleted.
    @Test
    fun unreadableData_duringAnExport_showsTheUnreadableScreen() {
        val store = store()
        store.backupSaveError = UnreadableDataException("The stored data could not be opened.")
        val vm = onBackup(store)
        val target = FakeTarget()
        vm.beginBackup()
        vm.onBackupPlace(target)
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
        assertFalse(vm.state.value.saving)
        assertNull(vm.state.value.backupStage)
        assertEquals(0, target.writes)
        assertEquals(1, target.deletes)
    }

    // Records the writer refuses (a stored line break) are not written; no mark is stored for a write that never began.
    @Test
    fun recordsThatCannotBeWritten_areNotSaved_andNoMarkIsStored() {
        val store = store()
        store.stored = store.stored.map { if (it.type == CashType.OPENING_BALANCE) it.copy(note = "two\nlines") else it }
        val vm = onBackup(store)
        val target = FakeTarget()
        vm.beginBackup()
        vm.onBackupPlace(target)
        assertEquals(BackupOutcome.NOT_SAVED, vm.state.value.backupOutcome)
        assertEquals(0, target.writes)
        assertEquals(1, target.deletes)
        assertTrue(store.backupSaves.isEmpty())
    }

    // The file holds what is stored when the place is chosen: the records are read then, not taken from an older view.
    @Test
    fun theFile_isMadeFromWhatTheStoreHoldsWhenThePlaceIsChosen() {
        val store = store()
        val vm = onBackup(store)
        vm.beginBackup()
        store.storedClassLists = ClassLists(sectors = listOf("Technology"))
        val target = FakeTarget()
        vm.onBackupPlace(target)
        assertEquals(fileOf(store), target.text)
        assertTrue(target.text!!.contains("list_value,,,,,,,,,,,,,,,sector,,Technology,\n"))
    }

    // Decision 7: a lock while the file screen is open does not stop the export, and the unlock after it keeps the
    // screen and its result.
    @Test
    fun anUnlockAfterTheWrite_keepsBackupAndRestore_andItsResult() {
        val vm = onBackup()
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget())
        vm.onUnlocked()
        assertEquals(Screen.BACKUP, vm.state.value.screen)
        assertEquals(BackupOutcome.SAVED, vm.state.value.backupOutcome)
    }

    // The result ends with the screen: Back and a tab change drop it, and the last backup stays.
    @Test
    fun leavingBackupAndRestore_dropsTheResult_notTheLastBackup() {
        val vm = onBackup()
        vm.beginBackup()
        vm.onBackupPlace(FakeTarget())
        vm.closeBackup()
        assertNull(vm.state.value.backupOutcome)
        assertEquals(Instant.parse(now), vm.state.value.lastBackup)
        vm.openBackup()
        vm.beginBackup()
        vm.onBackupPlace(null)
        vm.selectTab(Tab.CASH)
        assertNull(vm.state.value.backupOutcome)
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    // CR2, the wiring: More's line and the screen's model follow the state, in the device's zone.
    @Test
    fun theModels_followTheExport_inTheDevicesZone() {
        val newYork = ZoneId.of("America/New_York")
        val vm = onBackup(zone = newYork)
        assertEquals(More.of(null, newYork), vm.state.value.more)
        assertTrue(vm.state.value.backupScreen.canBackUp)
        vm.beginBackup()
        assertFalse(vm.state.value.backupScreen.canBackUp)
        vm.onBackupPlace(FakeTarget())
        val state = vm.state.value
        assertEquals(More.of(Instant.parse(now), newYork), state.more)
        assertEquals(LastBackup("2026-10-09", "EDT"), state.more.lastBackup)
        assertEquals(
            BackupModel(
                lastBackup = LastBackup("2026-10-09", "EDT"),
                interrupted = false,
                canBackUp = true,
                blocked = null,
                writing = false,
                outcome = BackupOutcome.SAVED,
                backEnabled = true,
                // 33b: Restore is active online.
                canRestore = true,
                restoreOffline = false,
            ),
            state.backupScreen,
        )
    }

    @Test
    fun theModels_withNoData_andAfterAnInterruptedBackup() {
        val empty = started(FakeStore(), clock = clockAt(now))
        assertEquals(BackupBlocked.NO_DATA, empty.state.value.backupScreen.blocked)
        assertFalse(empty.state.value.backupScreen.canBackUp)
        val stored = store(BackupState(lastBackup = earlier, unfinished = true))
        val interrupted = started(stored, clock = clockAt(now), zone = ZoneId.of("UTC"))
        assertTrue(interrupted.state.value.more.backupInterrupted)
        assertTrue(interrupted.state.value.backupScreen.interrupted)
        assertEquals(LastBackup("2026-10-04", "UTC"), interrupted.state.value.more.lastBackup)
    }

    // C15: no zone is stored; the line follows the zone the phone is in when the app is shown again.
    @Test
    fun theLastBackupLine_followsTheDevicesZone_whenTheAppIsShownAgain() {
        var zone = ZoneId.of("America/New_York")
        val vm =
            AppViewModel(
                storeProvider = { store(BackupState(lastBackup = Instant.parse(now))) },
                clock = clockAt(now),
                newId = { "x" },
                priceSource = FakeSource(),
                isOnline = { true },
                deviceZone = { zone },
            )
        vm.start()
        assertEquals("2026-10-09", vm.state.value.more.lastBackup!!.date)
        zone = ZoneId.of("Asia/Tokyo")
        assertEquals("2026-10-09", vm.state.value.more.lastBackup!!.date)
        vm.onAppShown()
        assertEquals("2026-10-10", vm.state.value.more.lastBackup!!.date)
        assertEquals("2026-10-10", vm.state.value.backupScreen.lastBackup!!.date)
    }

    // Spec section 9: nothing of the data reaches the state through the export; the state keeps a moment, a stage and a
    // reason only.
    @Test
    fun theState_keepsNoFileText() {
        val vm = onBackup()
        val target = FakeTarget()
        vm.beginBackup()
        vm.onBackupPlace(target)
        assertFalse(vm.state.value.toString().contains("row_type"))
    }
}
