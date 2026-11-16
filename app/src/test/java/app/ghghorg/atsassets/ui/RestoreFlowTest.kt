package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.backup.FileBytes
import app.ghghorg.atsassets.backup.RestoreFile
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
import app.ghghorg.atsassets.core.RestoreReason
import app.ghghorg.atsassets.core.RestoreSummary
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.prices.CheckRefusal
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

/** A chosen file in memory: it gives [answer], and counts the reads and the limit asked; [gate] makes a read wait. */
internal class FakeRestoreFile(
    override val name: String,
    var answer: FileBytes,
    val gate: CompletableDeferred<Unit>? = null,
) : RestoreFile {
    var reads = 0
    var limit = 0

    override suspend fun read(maxBytes: Int): FileBytes {
        reads++
        limit = maxBytes
        gate?.await()
        return answer
    }
}

// 33b step 3: the restore wizard in the screen state (spec section 8, restore steps 1 to 5; section 9; AC-08, AC-16, AC-56,
// AC-57; decisions 1 to 4): the file chosen, checked off the main thread, the held tickers through K4 with nothing stored,
// the report or the summary, Cancel, Back, a lock and a tab.
@OptIn(ExperimentalCoroutinesApi::class)
class RestoreFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val accepted = Lookup.Found(Listing(type = "Common Stock", exchange = "XNAS", currency = "USD"))

    private fun buy(ticker: String) = Transaction("b-$ticker", "p1", ticker, TransactionType.BUY, LocalDate.parse("2026-01-02"), 2L, BigDecimal.TEN, 100_000L)

    /** One portfolio holding [tickers], each bought once; [verified] of them are verified instruments in the app. */
    private fun store(
        tickers: List<String> = listOf("AAPL"),
        verified: List<String> = tickers,
    ): FakeStore {
        val buys = tickers.mapIndexed { i, t -> buy(t).copy(enteredSeq = 2L + i) }
        val lines = buys.map { CashEntry("c-${it.id}", it.date, -it.totalCents, CashType.TRADE, "", it.enteredSeq, transactionId = it.id) }
        val store = FakeStore(stored = listOf(opening()) + lines)
        store.storedPortfolios = listOf(Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L))
        store.storedTransactions = buys
        store.storedInstruments = verified.map { Instrument(it, AssetType.STOCK, verified = true) }
        return store
    }

    /** The file the writer makes of [store]'s records, with the test source's word. */
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

    private val note: (CashType, LocalDate) -> String = { type, date -> "Reversal of the $type of $date ET" }

    /** Started, online by [online]; the check runs on the test's main dispatcher, so each step ends before the next line. */
    private fun vm(
        store: FakeStore = store(),
        source: FakeSource = FakeSource(),
        online: () -> Boolean = { true },
    ): AppViewModel {
        var made = 0
        val vm =
            AppViewModel(
                storeProvider = { store },
                clock = clockAt("2026-10-26T12:00:00Z"),
                newId = { "app-${++made}" },
                priceSource = source,
                isOnline = online,
                deviceZone = { ZoneOffset.UTC },
                work = Dispatchers.Main,
            )
        vm.start()
        return vm
    }

    private fun AppViewModel.onBackup() {
        selectTab(Tab.MORE)
        openBackup()
    }

    /** On step 1 of the wizard with [file] chosen. */
    private fun AppViewModel.chosen(file: RestoreFile) {
        onBackup()
        openRestore()
        assertTrue(beginRestoreChoice())
        onRestoreFile(file)
    }

    private val AppViewModel.wizard: RestoreWizard get() = requireNotNull(state.value.restore)

    @Test
    fun restore_opensStep1_online() {
        val vm = vm()
        vm.onBackup()
        vm.openRestore()
        assertEquals(Screen.RESTORE, vm.state.value.screen)
        assertEquals(RestoreWizard(), vm.state.value.restore)
    }

    // Section 8, restore step 1; AC-08: offline the wizard does not start, and Backup and restore knows why.
    @Test
    fun restore_doesNotStart_offline() {
        var online = true
        val vm = vm(online = { online })
        vm.onBackup()
        assertTrue(vm.state.value.online)
        online = false
        vm.openRestore()
        assertEquals(Screen.BACKUP, vm.state.value.screen)
        assertNull(vm.state.value.restore)
        assertFalse(vm.state.value.online)
    }

    @Test
    fun theNetwork_isReadOnOpening_andWhenItComesOrGoes() {
        var online = false
        val vm = vm(online = { online })
        vm.onBackup()
        assertFalse(vm.state.value.online)
        online = true
        vm.onOnline()
        assertTrue(vm.state.value.online)
        online = false
        vm.onOffline()
        assertFalse(vm.state.value.online)
        online = true
        vm.onAppShown()
        assertTrue(vm.state.value.online)
    }

    @Test
    fun chooseFile_keepsTheName_andACancelKeepsTheLastChoice() {
        val vm = vm()
        val file = FakeRestoreFile("backup-2026-10-04.csv", FileBytes.Failed)
        vm.chosen(file)
        assertEquals("backup-2026-10-04.csv", vm.wizard.fileName)
        assertFalse(vm.wizard.choosing)
        assertTrue(vm.beginRestoreChoice())
        assertTrue(vm.wizard.choosing)
        // While the file screen is open, Back and a second Choose file do nothing.
        assertFalse(vm.beginRestoreChoice())
        vm.restoreBack()
        assertEquals(Screen.RESTORE, vm.state.value.screen)
        vm.onRestoreFile(null)
        assertEquals("backup-2026-10-04.csv", vm.wizard.fileName)
        assertFalse(vm.wizard.choosing)
    }

    @Test
    fun anAnswerWithNoChoiceWaiting_isIgnored() {
        val vm = vm()
        vm.onBackup()
        vm.openRestore()
        vm.onRestoreFile(FakeRestoreFile("x.csv", FileBytes.Failed))
        assertNull(vm.wizard.fileName)
    }

    // Section 8 steps 2 and 5; decision 1: a clean file whose held ticker the app already verified shows the summary,
    // with no call to the source and nothing stored (C3; decision 2).
    @Test
    fun aCleanFile_showsTheSummary_withNothingStored() {
        val store = store()
        val source = FakeSource()
        val vm = vm(store, source)
        val file = FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store)))
        vm.chosen(file)
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        assertEquals(
            RestoreSummary(portfolios = 1, transactions = 1, cashEntries = 1, cashBalanceCents = 900_000L, holdingsWithShares = 1),
            vm.wizard.summary,
        )
        assertEquals(RESTORE_FILE_LIMIT_BYTES, file.limit)
        assertTrue(source.asked.isEmpty())
        assertTrue(store.savedInstruments.isEmpty())
        assertTrue(store.saved.isEmpty())
        assertEquals(listOf("AAPL"), requireNotNull(vm.restoreReady).instruments.map { it.ticker })
    }

    // Decision 2: a held ticker not verified in the app is checked with the source, and stays in memory only.
    @Test
    fun aHeldTickerNotVerified_isChecked_butNotStored() {
        val store = store(tickers = listOf("AAPL", "MSFT"), verified = listOf("AAPL"))
        val source = FakeSource(answer = accepted)
        val vm = vm(store, source)
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store))))
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        assertEquals(listOf("MSFT"), source.asked)
        assertTrue(store.savedInstruments.isEmpty())
        assertEquals(listOf("AAPL", "MSFT"), requireNotNull(vm.restoreReady).instruments.map { it.ticker }.sorted())
        assertEquals(2, vm.wizard.verified)
        assertEquals(2, vm.wizard.toVerify)
    }

    // Section 8, restore step 3: the second round names each ticker the source refused (4.3).
    @Test
    fun aHeldTickerTheSourceRefuses_isReported_inASecondRound() {
        val store = store(tickers = listOf("MSFT"), verified = emptyList())
        val vm = vm(store, FakeSource(answer = Lookup.NotFound))
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store))))
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.REPORT, vm.wizard.step)
        assertEquals(listOf(TickerError("MSFT", TickerRefusal.NotFound)), vm.wizard.tickerErrors)
        assertTrue(vm.wizard.errors.isEmpty())
        assertNull(vm.restoreReady)
    }

    // Decision 3: a problem of the source stops the round on step 2 with Try again; the tickers are not reported.
    @Test
    fun aSourceProblem_stopsTheRound_andTryAgainChecksAgain() {
        val store = store(tickers = listOf("MSFT"), verified = emptyList())
        val source = FakeSource(answer = Lookup.TooManyCalls)
        val vm = vm(store, source)
        val file = FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store)))
        vm.chosen(file)
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.CHECKING, vm.wizard.step)
        assertEquals(CheckRefusal.TooManyCalls, vm.wizard.sourceProblem)
        assertTrue(vm.wizard.tickerErrors.isEmpty())
        source.answer = accepted
        vm.retryRestoreCheck()
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        assertNull(vm.wizard.sourceProblem)
        assertEquals(2, file.reads)
    }

    // A network lost during the round is the source's problem too (decision 3), not the ticker's.
    @Test
    fun offlineDuringTheRound_isASourceProblem() {
        var online = true
        val store = store(tickers = listOf("MSFT"), verified = emptyList())
        val vm = vm(store, FakeSource(answer = accepted), online = { online })
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store))))
        online = false
        vm.checkRestoreFile(note)
        assertEquals(CheckRefusal.Offline, vm.wizard.sourceProblem)
    }

    // Section 8, restore step 2; AC-16: a file with errors shows the report; the tickers are not checked.
    @Test
    fun aFileWithErrors_showsTheReport_andNoTickerIsChecked() {
        val store = store(tickers = listOf("MSFT"), verified = emptyList())
        val source = FakeSource(answer = accepted)
        val vm = vm(store, source)
        val text = String(fileOf(store)).replace("2026-01-02", "2026-02-30")
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(text.toByteArray())))
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.REPORT, vm.wizard.step)
        assertTrue(vm.wizard.errors.any { it.reason == RestoreReason.DateNotReal })
        assertTrue(source.asked.isEmpty())
    }

    @Test
    fun aFileThatIsNotUtf8_isReportedAsTheFile() {
        val vm = vm()
        vm.chosen(FakeRestoreFile("photo.jpg", FileBytes.Read(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))))
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.REPORT, vm.wizard.step)
        assertEquals(listOf(RestoreReason.NotUtf8), vm.wizard.errors.map { it.reason })
        assertNull(vm.wizard.errors.single().line)
    }

    // Decision 4, and a file that could not be read.
    @Test
    fun aFileTooLarge_orNotRead_isReportedAsTheFile() {
        val vm = vm()
        val file = FakeRestoreFile("big.csv", FileBytes.TooLarge)
        vm.chosen(file)
        vm.checkRestoreFile(note)
        assertEquals(RestoreFileProblem.TOO_LARGE, vm.wizard.fileProblem)
        vm.restoreBack()
        file.answer = FileBytes.Failed
        vm.checkRestoreFile(note)
        assertEquals(RestoreFileProblem.NOT_READ, vm.wizard.fileProblem)
        assertEquals(RestoreStep.REPORT, vm.wizard.step)
    }

    // Wireframe 10e marker 9: Cancel returns to step 1 with the file still chosen; the check's later answer is dropped.
    @Test
    fun cancel_returnsToStep1_andTheCheckIsDropped() {
        val store = store()
        val gate = CompletableDeferred<Unit>()
        val vm = vm(store)
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store)), gate))
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.CHECKING, vm.wizard.step)
        assertEquals(CheckPhase.READING, vm.wizard.phase)
        vm.cancelRestoreCheck()
        gate.complete(Unit)
        assertEquals(RestoreWizard(fileName = "b.csv"), vm.wizard)
        assertNull(vm.restoreReady)
    }

    // Section 9: a lock ends the wizard, which starts again from step 1 with nothing chosen and nothing changed.
    @Test
    fun aLock_startsTheWizardAgainFromStep1() {
        val store = store()
        val gate = CompletableDeferred<Unit>()
        val vm = vm(store)
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store)), gate))
        vm.checkRestoreFile(note)
        vm.onUnlocked()
        gate.complete(Unit)
        assertEquals(Screen.RESTORE, vm.state.value.screen)
        assertEquals(RestoreWizard(), vm.wizard)
        // The chosen file went with it: Check file needs a new choice.
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.CHOOSE, vm.wizard.step)
    }

    @Test
    fun aTab_endsTheWizard() {
        val store = store()
        val vm = vm(store)
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store))))
        vm.checkRestoreFile(note)
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertNull(vm.state.value.restore)
        assertNull(vm.restoreReady)
    }

    // Wireframe 10e: Back returns one step, the file still chosen; from step 1 it leaves the wizard.
    @Test
    fun back_returnsOneStep_thenLeaves() {
        val store = store()
        val vm = vm(store)
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store))))
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.SUMMARY, vm.wizard.step)
        vm.restoreBack()
        assertEquals(RestoreWizard(fileName = "b.csv"), vm.wizard)
        assertNull(vm.restoreReady)
        vm.restoreBack()
        assertEquals(Screen.BACKUP, vm.state.value.screen)
        assertNull(vm.state.value.restore)
    }

    // Decision 1: the summary's Cancel closes the wizard to Backup and restore; there is no Continue yet (34).
    @Test
    fun theSummarysCancel_closesTheWizard() {
        val store = store()
        val vm = vm(store)
        vm.chosen(FakeRestoreFile("b.csv", FileBytes.Read(fileOf(store))))
        vm.checkRestoreFile(note)
        vm.closeRestore()
        assertEquals(Screen.BACKUP, vm.state.value.screen)
        assertNull(vm.state.value.restore)
        assertNull(vm.restoreReady)
    }

    // Wireframe 10e marker 11: Choose another file returns to step 1 with no file chosen.
    @Test
    fun chooseAnotherFile_returnsToStep1_withNoFile() {
        val vm = vm()
        vm.chosen(FakeRestoreFile("big.csv", FileBytes.TooLarge))
        vm.checkRestoreFile(note)
        vm.chooseAnotherRestoreFile()
        assertEquals(RestoreWizard(), vm.wizard)
        vm.checkRestoreFile(note)
        assertEquals(RestoreStep.CHOOSE, vm.wizard.step)
    }

    @Test
    fun checkFile_needsAChosenFile() {
        val vm = vm()
        vm.onBackup()
        vm.openRestore()
        vm.checkRestoreFile(note)
        assertEquals(RestoreWizard(), vm.wizard)
    }
}
