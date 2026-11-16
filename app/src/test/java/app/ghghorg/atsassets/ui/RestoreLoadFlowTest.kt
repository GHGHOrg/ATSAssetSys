package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.backup.FileBytes
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.BackupFile
import app.ghghorg.atsassets.core.BackupRecords
import app.ghghorg.atsassets.core.BackupResult
import app.ghghorg.atsassets.core.BackupState
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Quantity
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
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// 34 step 3: the restore itself in the screen state (spec section 8, restore steps 5 and 7; section 9; AC-04, AC-18,
// AC-25, AC-56; decisions 1, 3 and 4): on an app with no data, Restore deletes and loads as one action, the state is
// read again from the store, the result shows, and Go to Overall opens the app as it now is. A failed write changes
// nothing. On an app that holds data there is no Restore (35).
@OptIn(ExperimentalCoroutinesApi::class)
class RestoreLoadFlowTest {
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

    /** An app with data: one portfolio holding AAPL, a backup made, a lock choice and a tab order of its own. */
    private fun sourceStore(): FakeStore {
        val buy = Transaction("b1", "p1", "AAPL", TransactionType.BUY, LocalDate.parse("2026-01-02"), 2L, BigDecimal.TEN, 100_000L)
        val line = CashEntry("c-b1", buy.date, -buy.totalCents, CashType.TRADE, "", buy.enteredSeq, transactionId = buy.id)
        val store = FakeStore(stored = listOf(opening(), line))
        store.storedPortfolios = listOf(Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L))
        store.storedTransactions = listOf(buy)
        store.storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true))
        store.storedAutoLock = AutoLock.IDLE_5_MIN
        store.storedTabOrder = TabOrder.parse("overall;portfolios;cash;more")
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

    /** An app with no data: a saved price key and an old backup moment are the only things stored. */
    private fun emptyStore(): FakeStore {
        val store = FakeStore()
        store.storedBackupState = BackupState(lastBackup = Instant.parse("2026-10-04T03:00:00Z"))
        return store
    }

    private fun vm(
        store: FakeStore,
        source: FakeSource = FakeSource(answer = accepted),
    ): AppViewModel {
        var made = 0
        val vm =
            AppViewModel(
                storeProvider = { store },
                clock = clockAt("2026-10-26T12:00:00Z"),
                newId = { "app-${++made}" },
                priceSource = source,
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
        assertTrue(beginRestoreChoice())
        onRestoreFile(FakeRestoreFile("backup.csv", FileBytes.Read(bytes)))
        checkRestoreFile(note)
    }

    private val AppViewModel.wizard: RestoreWizard get() = requireNotNull(state.value.restore)

    // Section 8, restore step 7; decisions 1 and 4: one delete-and-load with the file's records and the verified
    // instruments; the state read again; the result on the screen; the key gone; "never backed up".
    @Test
    fun onAnEmptyApp_restoreLoadsTheFile_andShowsTheResult() {
        val store = emptyStore()
        val source = FakeSource(answer = accepted)
        val vm = vm(store, source)
        assertTrue(vm.state.value.priceKeySet)
        vm.checked(fileOf(sourceStore()))
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        assertTrue(vm.state.value.canRestore)
        vm.restoreNow()
        val (records, instruments) = store.replacements.single()
        assertEquals(1, records.portfolios.size)
        assertEquals(listOf("AAPL"), instruments.map { it.ticker })
        val state = vm.state.value
        assertEquals(Screen.RESTORE, state.screen)
        assertEquals(RestoreStep.RESULT, vm.wizard.step)
        assertEquals(1, vm.wizard.summary?.portfolios)
        assertFalse(state.saving)
        // The state is what the store now holds.
        assertEquals(store.stored, state.entries)
        assertEquals(listOf("Portfolio P"), state.portfolios.map { it.name })
        assertEquals(AutoLock.IDLE_5_MIN, state.autoLock)
        assertEquals("overall;portfolios;cash;more", state.tabOrder.text)
        // Decision 1: the key went with every other setting, and the source no longer uses it.
        assertFalse(state.priceKeySet)
        assertNull(source.keyInUse)
        // Section 8: the last backup is reset, so a backup is due (AC-18: the banner after a restore).
        assertNull(state.lastBackup)
        assertTrue(state.backupDue)
        assertNull(vm.restoreReady)
    }

    // CR2: the state hands the wizard's screen its model through the restore: Restore offered on the summary of an app
    // with no data, then the result with the counts loaded.
    @Test
    fun theState_wiresTheRestoresModel() {
        val vm = vm(emptyStore())
        vm.checked(fileOf(sourceStore()))
        val summary = requireNotNull(vm.state.value.restoreScreen)
        assertTrue(summary.canRestore)
        assertEquals("4", summary.totalSteps)
        vm.restoreNow()
        val result = requireNotNull(vm.state.value.restoreScreen)
        assertEquals(RestoreSummaryModel("1", "1", "1", "$9,000.00", "1"), result.loaded)
        assertFalse(result.backEnabled)
        assertEquals("4", result.totalSteps)
    }

    // Wireframe 10e marker 18: Go to Overall opens the app as it now is; there is no way back into the wizard.
    @Test
    fun goToOverall_opensOverall_withTheReminder() {
        val vm = vm(emptyStore())
        vm.checked(fileOf(sourceStore()))
        vm.restoreNow()
        vm.restoreBack()
        assertEquals("Back does not return into the wizard", RestoreStep.RESULT, vm.wizard.step)
        vm.finishRestore()
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.OVERALL, state.tab)
        assertNull(state.restore)
        assertTrue(state.backupReminderShown)
    }

    // AC-04: backup, then restore on a clean install: the same portfolios, transactions, cash entries, lots, cash
    // balance and holdings; "no price" until the first refresh.
    @Test
    fun aBackupRestoredOnACleanInstall_reproducesTheApp() {
        val original = vm(sourceStore()).state.value
        val vm = vm(emptyStore())
        vm.checked(fileOf(sourceStore()))
        vm.restoreNow()
        val restored = vm.state.value
        assertEquals(original.portfolios.map { it.name }, restored.portfolios.map { it.name })
        assertEquals(original.cashBalanceCents, restored.cashBalanceCents)
        assertEquals(original.entries.map { Triple(it.date, it.amountCents, it.type) }, restored.entries.map { Triple(it.date, it.amountCents, it.type) })
        assertEquals(
            original.transactions.map { listOf(it.ticker, it.type, it.date, it.quantity, it.totalCents) },
            restored.transactions.map { listOf(it.ticker, it.type, it.date, it.quantity, it.totalCents) },
        )
        assertEquals(original.holdings.map { it.ticker to Quantity.format(it.shares) }, restored.holdings.map { it.ticker to Quantity.format(it.shares) })
        assertTrue(restored.instruments.all { it.price == null })
    }

    // AC-25: spec/example-backup.csv restores with the results of spec/example-backup-expected.md.
    @Test
    fun theExampleFile_restoresWithItsExpectedResults() {
        val store = emptyStore()
        val vm = vm(store)
        vm.checked(File("../spec/example-backup.csv").readBytes())
        vm.restoreNow()
        val state = vm.state.value
        assertEquals(RestoreStep.RESULT, vm.wizard.step)
        assertEquals(listOf("Main", "Retirement"), state.portfolios.map { it.name })
        assertEquals(942_500L, state.cashBalanceCents)
        assertEquals(8, state.transactions.size)
        assertEquals(7, state.entries.count { it.type == CashType.TRADE })
        assertEquals(4, state.entries.count { it.type != CashType.TRADE })
        val main = state.portfolios.first { it.name == "Main" }.id
        val held = state.holdings.filter { it.shares.signum() > 0 }
        assertEquals(listOf("AAPL" to "16"), held.map { it.ticker to Quantity.format(it.shares) })
        assertEquals(listOf(HiddenHolding(main, "OLDCO")), state.hidden)
        assertEquals(
            setOf(ClassOverride(main, "AAPL", ClassList.SECTOR, "Technology"), ClassOverride(main, "AAPL", ClassList.GEOGRAPHY, "United States")),
            state.overrides.toSet(),
        )
        assertEquals(setOf("Diversified", "Technology"), state.classLists.sectors.toSet())
        assertEquals(setOf("Global", "United States"), state.classLists.geographies.toSet())
        assertEquals(AutoLock.IDLE_5_MIN, state.autoLock)
        assertEquals("overall;portfolios;cash;more", state.tabOrder.text)
        assertNull(state.lastBackup)
        // AAPL is verified and stored; OLDCO is sold out, so it is not (C8).
        assertEquals(listOf("AAPL"), store.storedInstruments.map { it.ticker })
    }

    // Wireframe 10e marker 19; C3: a failed write changes nothing, and the screen says so.
    @Test
    fun aFailedWrite_changesNothing_andSaysSo() {
        val store = emptyStore()
        store.replaceError = IOException("disk")
        val vm = vm(store)
        vm.checked(fileOf(sourceStore()))
        vm.restoreNow()
        assertEquals(RestoreStep.FAILED, vm.wizard.step)
        val state = vm.state.value
        assertFalse(state.saving)
        assertTrue(state.entries.isEmpty())
        assertTrue(state.priceKeySet)
        assertEquals(Instant.parse("2026-10-04T03:00:00Z"), state.lastBackup)
        assertTrue(store.replacements.isEmpty())
        vm.closeRestore()
        assertEquals(Screen.BACKUP, vm.state.value.screen)
    }

    // Decision 3: on an app that holds data there is no Restore in 34 (35 adds step 4).
    @Test
    fun onAnAppThatHoldsData_thereIsNoRestore() {
        val store = sourceStore()
        val vm = vm(store)
        vm.checked(fileOf(sourceStore()))
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        assertFalse(vm.state.value.canRestore)
        vm.restoreNow()
        assertTrue(store.replacements.isEmpty())
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
    }

    // Restore acts only on the summary of a file that passed.
    @Test
    fun restore_needsAPassedFile() {
        val store = emptyStore()
        val vm = vm(store)
        vm.selectTab(Tab.MORE)
        vm.openBackup()
        vm.openRestore()
        vm.restoreNow()
        assertTrue(store.replacements.isEmpty())
    }

    // The write is one action: a lock while it runs does not end it, and its result shows after the unlock (as an
    // export's, 31 decision 7); Back and a tab wait while it runs.
    @Test
    fun aLockDuringTheWrite_keepsTheResult() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeStore(gate = gate)
        val vm = vm(store)
        vm.checked(fileOf(sourceStore()))
        vm.restoreNow()
        assertEquals(RestoreStep.RESTORING, vm.wizard.step)
        assertTrue(vm.state.value.saving)
        vm.restoreBack()
        vm.selectTab(Tab.CASH)
        assertEquals(RestoreStep.RESTORING, vm.wizard.step)
        vm.onUnlocked()
        gate.complete(Unit)
        assertEquals(RestoreStep.RESULT, vm.wizard.step)
        assertEquals(1, store.replacements.size)
    }

    @Test
    fun unreadableData_duringTheWrite_showsTheUnreadableScreen() {
        val store = emptyStore()
        store.replaceError = UnreadableDataException("unreadable")
        val vm = vm(store)
        vm.checked(fileOf(sourceStore()))
        vm.restoreNow()
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
    }
}
