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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

// 35: restoring onto an app that holds data, in the screen state (spec section 8, restore steps 5 to 7; section 9;
// AC-17, AC-56, AC-57; wireframe 10e frames F to H; decisions 1 to 3). Step 2: the summary's counts now.
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
    private fun dataStore(): FakeStore {
        val buy = Transaction("b1", "p1", "AAPL", TransactionType.BUY, LocalDate.parse("2026-01-02"), 2L, BigDecimal.TEN, 100_000L)
        val line = CashEntry("c-b1", buy.date, -buy.totalCents, CashType.TRADE, "", buy.enteredSeq, transactionId = buy.id)
        val store = FakeStore(stored = listOf(opening(), line))
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
