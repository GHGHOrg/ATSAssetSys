package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.backup.FileBytes
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.BackupFile
import app.ghghorg.atsassets.core.BackupRecords
import app.ghghorg.atsassets.core.BackupResult
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.RestoreCounts
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
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
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// 35: restoring onto an app that holds data, in the screen state (spec section 8, restore steps 5 to 7; section 9;
// AC-17, AC-56, AC-57; wireframe 10e frames F to H; decisions 1 to 3). Step 2: the summary's counts now. Step 3: step 4,
// the typed word, the export from step 4, Replace all data, Back, Cancel and the locks.
@OptIn(ExperimentalCoroutinesApi::class)
class RestoreOverDataFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val accepted = Lookup.Found(Listing(type = "Common Stock", exchange = "XNAS", currency = "USD"))
    private val note: (CashType, LocalDate) -> String = { type, date -> "Reversal of the $type of $date ET" }

    /** An app with data: one portfolio holding AAPL (one buy, its trade line, the opening balance). */
    private fun dataStore(gate: CompletableDeferred<Unit>? = null): FakeStore {
        val buy = Transaction("b1", "p1", "AAPL", TransactionType.BUY, LocalDate.parse("2026-01-02"), 2L, BigDecimal.TEN, 100_000L)
        val line = CashEntry("c-b1", buy.date, -buy.totalCents, CashType.TRADE, "", buy.enteredSeq, transactionId = buy.id)
        val store = FakeStore(stored = listOf(opening(), line), gate = gate)
        store.storedPortfolios = listOf(Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L))
        store.storedTransactions = listOf(buy)
        store.storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true))
        store.storedAutoLock = AutoLock.IDLE_5_MIN
        return store
    }

    /** The backup file the writer makes of [store]'s records. */
    private fun fileOf(store: FakeStore): ByteArray {
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
        return (BackupFile.write(records) as BackupResult.Written).text.toByteArray()
    }

    private val example: ByteArray get() = File("../spec/example-backup.csv").readBytes()

    private fun vm(store: FakeStore): AppViewModel {
        var made = 0
        val vm =
            AppViewModel(
                storeProvider = { store },
                clock = clockAt("2026-10-26T12:00:00Z"),
                newId = { "app-${++made}" },
                priceSource = FakeSource(answer = accepted),
                isOnline = { true },
                deviceZone = { ZoneOffset.UTC },
                work = Dispatchers.Main,
            )
        vm.start()
        return vm
    }

    /** From More: Backup and restore, Restore, the file chosen and checked. */
    private fun AppViewModel.checked(bytes: ByteArray) {
        selectTab(Tab.MORE)
        openBackup()
        openRestore()
        beginRestoreChoice()
        onRestoreFile(FakeRestoreFile("backup.csv", FileBytes.Read(bytes)))
        checkRestoreFile(note)
    }

    private val AppViewModel.wizard: RestoreWizard get() = requireNotNull(state.value.restore)

    // Section 8, restore step 5; wireframe 10e frame F, marker 13: on an app that holds data the summary counts what the
    // app holds now by the file's rule (the trade line left out), beside the file's counts, with Continue (CR2 wiring).
    @Test
    fun theSummary_onAnAppThatHoldsData_countsWhatItHoldsNow() {
        val vm = vm(dataStore())
        vm.checked(example)
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        assertEquals(RestoreCounts(portfolios = 1, transactions = 1, cashEntries = 1), vm.wizard.now)
        val model = requireNotNull(vm.state.value.restoreScreen)
        assertEquals(RestoreNowModel("1", "1", "1"), model.now)
        assertEquals(RestoreSummaryModel("2", "8", "4", "$9,425.00", "1"), model.summary)
        assertEquals(true, model.canContinue)
    }

    /** The example file checked over [store]'s data, then Continue: step 4. */
    private fun AppViewModel.onStep4() {
        checked(example)
        continueRestore()
        assertEquals(RestoreStep.CONFIRM, wizard.step)
    }

    // Decision 2 (section 6, AC-56: "case and spaces ignored"): every space anywhere and any letter case.
    @Test
    fun theWord_ignoresEverySpace_andTheCase() {
        for (typed in listOf("RESTORE", "restore", " Re Store ", "R E S T O R E", "re store", "\trestore\n")) {
            assertTrue("'$typed' matches", RestoreWord.matches(typed))
        }
        for (typed in listOf("", " ", "RESTOR", "RESTORES", "restore!", "replace", "RE-STORE")) {
            assertFalse("'$typed' does not match", RestoreWord.matches(typed))
        }
    }

    // Section 8, restore step 6; wireframe 10e frame G: Continue opens step 4 with nothing typed, and Replace all data
    // waits for the word. Typing acts only on step 4; Continue only on the summary of an app that holds data.
    @Test
    fun continue_opensStep4_andReplaceWaitsForTheWord() {
        val store = dataStore()
        val vm = vm(store)
        vm.checked(example)
        vm.typeRestoreWord("RESTORE")
        assertEquals("", vm.wizard.typed)
        vm.continueRestore()
        assertEquals(RestoreStep.CONFIRM, vm.wizard.step)
        assertEquals("", vm.wizard.typed)
        assertFalse(vm.state.value.canRestore)
        vm.restoreNow()
        assertEquals(RestoreStep.CONFIRM, vm.wizard.step)
        vm.typeRestoreWord("RESTOR")
        assertFalse(vm.state.value.canRestore)
        vm.typeRestoreWord(" re store ")
        assertEquals(" re store ", vm.wizard.typed)
        assertTrue(vm.state.value.canRestore)
        assertTrue(store.replacements.isEmpty())
        // On an app with no data there is no step 4 (wireframe 10e marker 15).
        val empty = vm(FakeStore())
        empty.checked(fileOf(dataStore()))
        empty.continueRestore()
        assertEquals(RestoreStep.SUMMARY, empty.wizard.step)
    }

    // Section 8, restore step 7; AC-17: Replace all data deletes everything and loads the file as one action (34's
    // write); the state is read again, the result shows, and the last backup is reset ("never backed up").
    @Test
    fun replaceAllData_replacesEverything_andShowsTheResult() {
        val store = dataStore()
        val vm = vm(store)
        vm.onStep4()
        vm.typeRestoreWord("restore")
        vm.restoreNow()
        val (records, instruments) = store.replacements.single()
        assertEquals(listOf("Main", "Retirement"), records.portfolios.map { it.name })
        assertEquals(listOf("AAPL"), instruments.map { it.ticker })
        val state = vm.state.value
        assertEquals(Screen.RESTORE, state.screen)
        assertEquals(RestoreStep.RESULT, vm.wizard.step)
        assertFalse(state.saving)
        assertEquals(listOf("Main", "Retirement"), state.portfolios.map { it.name })
        assertEquals(942_500L, state.cashBalanceCents)
        assertNull(state.lastBackup)
        assertTrue(state.backupDue)
        vm.finishRestore()
        assertEquals(Tab.OVERALL, vm.state.value.tab)
        assertTrue(vm.state.value.backupReminderShown)
    }

    // Wireframe 10e's note: Back on step 4 returns to step 3, the counts kept and the word cleared; Cancel closes, and
    // nothing was changed (C3).
    @Test
    fun backAndCancel_onStep4_changeNothing() {
        val store = dataStore()
        val vm = vm(store)
        vm.onStep4()
        vm.typeRestoreWord("RESTORE")
        vm.restoreBack()
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        assertEquals(RestoreCounts(1, 1, 1), vm.wizard.now)
        assertEquals("", vm.wizard.typed)
        vm.continueRestore()
        assertEquals("", vm.wizard.typed)
        vm.closeRestore()
        assertEquals(Screen.BACKUP, vm.state.value.screen)
        assertNull(vm.state.value.restore)
        assertTrue(store.replacements.isEmpty())
        assertEquals(listOf("Portfolio P"), vm.state.value.portfolios.map { it.name })
    }

    // Wireframe 10e marker 16; section 9; AC-17: step 4 offers the same export as More. It starts only on step 4; while
    // it is under way Replace all data, Back and Cancel wait; its result shows on step 4 ("Backup saved") and the last
    // backup moves. The restore then resets it.
    @Test
    fun backUpNow_onStep4_exportsFirst_andItsResultShowsThere() {
        val store = dataStore()
        val vm = vm(store)
        vm.checked(example)
        assertNull("not from the summary", vm.beginBackup())
        vm.continueRestore()
        vm.typeRestoreWord("RESTORE")
        assertEquals("backup-2026-10-26.csv", vm.beginBackup())
        assertEquals(BackupStage.CHOOSING, vm.state.value.backupStage)
        assertFalse(vm.state.value.canRestore)
        vm.restoreNow()
        vm.restoreBack()
        vm.closeRestore()
        assertEquals(RestoreStep.CONFIRM, vm.wizard.step)
        assertTrue(store.replacements.isEmpty())
        val target = FakeTarget()
        vm.onBackupPlace(target)
        val state = vm.state.value
        assertEquals(BackupOutcome.SAVED, state.backupOutcome)
        assertEquals(Instant.parse("2026-10-26T12:00:00Z"), state.lastBackup)
        assertTrue(requireNotNull(target.text).contains("Portfolio P"))
        assertEquals(RestoreStep.CONFIRM, vm.wizard.step)
        assertTrue(state.canRestore)
        vm.restoreNow()
        assertEquals(RestoreStep.RESULT, vm.wizard.step)
        assertNull(vm.state.value.lastBackup)
        assertNull("the export's result went with step 4", vm.state.value.backupOutcome)
    }

    // 32 decision 1: an export's result lasts only where it shows; Back from step 4 drops it.
    @Test
    fun theExportsResult_goesWithStep4() {
        val vm = vm(dataStore())
        vm.onStep4()
        vm.beginBackup()
        vm.onBackupPlace(null)
        assertEquals(BackupOutcome.NOT_SAVED, vm.state.value.backupOutcome)
        vm.restoreBack()
        assertNull(vm.state.value.backupOutcome)
    }

    // Section 9, AC-57: a lock on step 4 (idle time, the phone locking) ends the wizard; after the unlock it starts
    // again from step 1, nothing changed.
    @Test
    fun aLockOnStep4_returnsToStep1() {
        val store = dataStore()
        val vm = vm(store)
        vm.onStep4()
        vm.typeRestoreWord("RESTORE")
        vm.onLocked()
        vm.onUnlocked()
        assertEquals(RestoreWizard(), vm.wizard)
        vm.restoreNow()
        assertTrue(store.replacements.isEmpty())
    }

    // Section 9, AC-57: a lock while the restore over data is written rolls it back (34's departure 5), the data as it
    // was.
    @Test
    fun aLockDuringTheWrite_overData_rollsItBack() {
        val gate = CompletableDeferred<Unit>()
        val store = dataStore(gate)
        val vm = vm(store)
        vm.onStep4()
        vm.typeRestoreWord("RESTORE")
        vm.restoreNow()
        assertEquals(RestoreStep.RESTORING, vm.wizard.step)
        vm.onLocked()
        gate.complete(Unit)
        assertEquals(RestoreWizard(), vm.wizard)
        assertTrue(store.replacements.isEmpty())
        assertEquals(listOf("Portfolio P"), vm.state.value.portfolios.map { it.name })
    }

    // Wireframe 10e marker 19; C3: a failed write over data changes nothing, and the screen says so.
    @Test
    fun aFailedWrite_overData_changesNothing() {
        val store = dataStore()
        store.replaceError = IOException("disk")
        val vm = vm(store)
        vm.onStep4()
        vm.typeRestoreWord("RESTORE")
        vm.restoreNow()
        assertEquals(RestoreStep.FAILED, vm.wizard.step)
        assertEquals(listOf("Portfolio P"), vm.state.value.portfolios.map { it.name })
        assertEquals(AutoLock.IDLE_5_MIN, vm.state.value.autoLock)
    }

    // Wireframe 10e marker 15: on an app with no data there is nothing to count now.
    @Test
    fun theSummary_onAnEmptyApp_hasNoCountsNow() {
        val vm = vm(FakeStore())
        vm.checked(fileOf(dataStore()))
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        assertNull(vm.wizard.now)
        assertNull(vm.state.value.restoreScreen?.now)
    }
}
