package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOrigin
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.Classification
import app.ghghorg.atsassets.core.EffectiveClass
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.ListRefusal
import app.ghghorg.atsassets.core.ListResult
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.RememberedText
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.UnreadableDataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 25 step 4: Settings, Lists in the screen state (4.9, section 6, wireframe 10f frames F and G; decisions 1 to 4; AC-22, AC-58).
@OptIn(ExperimentalCoroutinesApi::class)
class ClassListFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)

    private fun buy(
        id: String,
        ticker: String,
        date: LocalDate = LocalDate.of(2026, 1, 2),
    ) = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.BUY,
        date = date,
        enteredSeq = id.drop(1).toLong() + 1,
        quantity = BigDecimal("10"),
        totalCents = 150_000L,
    )

    private fun line(transaction: Transaction): CashEntry {
        val amount = -transaction.totalCents
        return CashEntry("c-${transaction.id}", transaction.date, amount, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)
    }

    private val aapl = buy("b1", "AAPL")
    private val msft = buy("b2", "MSFT")

    // AC-58's sample: Technology also matches "Information Technology"; AAPL uses Technology and United States. MSFT is
    // hidden and uses Diversified, so hidden holdings count (4.9).
    private val lists =
        ClassLists(
            sectors = listOf("Technology", "Diversified"),
            geographies = listOf("United States", "Global"),
            remembered = listOf(RememberedText(ClassList.SECTOR, "Information Technology", "Technology")),
        )

    private fun store(): FakeStore = FakeStore(stored = listOf(opening(), line(aapl), line(msft))).also {
        it.storedPortfolios = listOf(main)
        it.storedTransactions = listOf(aapl, msft)
        it.storedHidden = listOf(HiddenHolding("p1", "MSFT"))
        it.storedInstruments =
            listOf(
                Instrument("AAPL", AssetType.STOCK, verified = true, profile = SourceProfile("Information Technology", "United States")),
                Instrument("MSFT", AssetType.STOCK, verified = true, profile = SourceProfile("Diversified", null)),
            )
        it.storedClassLists = lists
    }

    /** Started on Settings' [list] screen, offline so no refresh runs. */
    private fun onList(
        store: FakeStore = store(),
        list: ClassList = ClassList.SECTOR,
    ): AppViewModel {
        val vm = started(store, online = { false })
        vm.selectTab(Tab.MORE)
        vm.openSettings()
        vm.openClassList(list)
        return vm
    }

    private fun used(
        vm: AppViewModel,
        list: ClassList,
        value: String,
    ): Int = vm.state.value.let { Classification.holdingsUsing(it.classLists, list, value, it.listUsers) }

    private fun sectorNames(vm: AppViewModel): List<SliceName> = vm.state.value.allocation(AllocationBy.SECTOR)!!.rows.map { it.name }

    // Section 6: Settings' Lists opens a list's screen; "<" goes back to Settings. The texts load with the lists.
    @Test
    fun aListOpensFromSettings_withItsRememberedTexts_andClosesBackToSettings() {
        val vm = onList()
        assertEquals(Screen.CLASS_LIST, vm.state.value.screen)
        assertEquals(ClassList.SECTOR, vm.state.value.classListShown)
        assertEquals(lists, vm.state.value.classLists)
        vm.closeClassList()
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
        assertEquals(null, vm.state.value.classListShown)
    }

    // AC-58 and 4.9: each value's holdings, the hidden MSFT included.
    @Test
    fun eachValuesHoldings_countTheHiddenOnes() {
        val vm = onList()
        assertEquals(1, used(vm, ClassList.SECTOR, "Technology"))
        assertEquals(1, used(vm, ClassList.SECTOR, "Diversified"))
        assertEquals(1, used(vm, ClassList.GEOGRAPHY, "United States"))
        assertEquals(0, used(vm, ClassList.GEOGRAPHY, "Global"))
    }

    // My reading (section 4): a holding whose only buy is dated after today is still a holding that uses its value.
    @Test
    fun aHoldingBoughtAfterToday_usesItsValue() {
        val later = buy("b3", "VT", LocalDate.of(2099, 1, 2))
        val store =
            store().apply {
                storedTransactions = storedTransactions + later
                storedInstruments = storedInstruments + Instrument("VT", AssetType.ETF, verified = true, profile = SourceProfile(null, "Global"))
            }
        val vm = onList(store, ClassList.GEOGRAPHY)
        assertEquals(1, used(vm, ClassList.GEOGRAPHY, "Global"))
    }

    // 4.9: Add value, Name, Save: the value is stored and listed last; the layer closes and says it was added.
    @Test
    fun add_storesTheValue_closesTheLayer_andSaysSo() {
        val store = store()
        val vm = onList(store)
        vm.openAddClassValue()
        assertEquals(ClassValueEdit(renaming = null, text = ""), vm.state.value.classValueEdit)
        vm.typeClassValueName(" Energy ")
        vm.saveClassValueName()
        assertEquals(listOf("Technology", "Diversified", "Energy"), store.storedClassLists.sectors)
        assertEquals(listOf("Technology", "Diversified", "Energy"), vm.state.value.classLists.sectors)
        assertEquals(null, vm.state.value.classValueEdit)
        assertEquals(ListResult.Added(store.storedClassLists, "Energy"), vm.state.value.listOutcome)
    }

    // AC-58 and frame G: a name that matches a value is refused, the layer and the typed text stay, nothing is stored.
    @Test
    fun aRefusedName_keepsTheLayerAndTheText_andStoresNothing() {
        val store = store()
        val vm = onList(store)
        vm.openAddClassValue()
        vm.typeClassValueName("technology ")
        vm.saveClassValueName()
        assertEquals(ClassValueEdit(renaming = null, text = "technology "), vm.state.value.classValueEdit)
        assertEquals(ListResult.Refused(ListRefusal.Duplicate("Technology")), vm.state.value.listOutcome)
        assertEquals(lists, store.storedClassLists)
        vm.typeClassValueName("Information technology")
        vm.saveClassValueName()
        assertEquals(ListResult.Refused(ListRefusal.Remembered("Information Technology", "Technology")), vm.state.value.listOutcome)
        vm.typeClassValueName("")
        vm.saveClassValueName()
        assertEquals(ListResult.Refused(ListRefusal.Blank), vm.state.value.listOutcome)
        assertEquals(lists, store.storedClassLists)
    }

    // Typing again clears the refusal; Cancel closes the layer and changes nothing.
    @Test
    fun typingClearsTheRefusal_andCancelChangesNothing() {
        val store = store()
        val vm = onList(store)
        vm.openAddClassValue()
        vm.typeClassValueName("Technology")
        vm.saveClassValueName()
        vm.typeClassValueName("Technology X")
        assertEquals(null, vm.state.value.listOutcome)
        vm.cancelClassValueName()
        assertEquals(null, vm.state.value.classValueEdit)
        assertEquals(Screen.CLASS_LIST, vm.state.value.screen)
        assertEquals(lists, store.storedClassLists)
    }

    // AC-22 and AC-58: Rename opens on the value's name; after Technology becomes Tech, AAPL's source text still maps to
    // it, so the Classification tab and the breakdowns show Tech at once.
    @Test
    fun rename_changesTheTabAndTheBreakdownsAtOnce() {
        val store = store()
        val vm = onList(store)
        vm.openRenameClassValue("Technology")
        assertEquals(ClassValueEdit(renaming = "Technology", text = "Technology"), vm.state.value.classValueEdit)
        vm.typeClassValueName("Tech")
        vm.saveClassValueName()
        assertEquals(listOf("Tech", "Diversified"), store.storedClassLists.sectors)
        assertEquals(store.storedClassLists, vm.state.value.classLists)
        assertEquals(null, vm.state.value.classValueEdit)
        assertEquals(ListResult.Renamed(store.storedClassLists, "Technology", "Tech", null), vm.state.value.listOutcome)
        // MSFT's Diversified slice is there too: the fake store keeps its 10 shares although it is hidden.
        assertEquals(listOf(SliceName.Cash, SliceName.Named("Tech"), SliceName.Named("Diversified")), sectorNames(vm))
        vm.closeClassList()
        vm.closeSettings()
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        assertEquals(EffectiveClass("Tech", ClassOrigin.SOURCE), vm.state.value.holdingClassification!!.sector)
    }

    // Decision 1 and AC-58: renaming United States, AAPL's source text, remembers it; AAPL's geography becomes USA.
    @Test
    fun renamingASourceValue_remembersItsText() {
        val store = store()
        val vm = onList(store, ClassList.GEOGRAPHY)
        vm.openRenameClassValue("United States")
        vm.typeClassValueName("USA")
        vm.saveClassValueName()
        val remembered = RememberedText(ClassList.GEOGRAPHY, "United States", "USA")
        assertEquals(lists.remembered + remembered, store.storedClassLists.remembered)
        assertEquals(store.storedClassLists, vm.state.value.classLists)
        assertEquals(1, used(vm, ClassList.GEOGRAPHY, "USA"))
        assertEquals("USA", vm.state.value.holdingClasses.valueFor(ClassList.GEOGRAPHY, "p1", "AAPL"))
    }

    // 24 decision 3: an override names its value's row, so a rename carries it over; the state follows the store.
    @Test
    fun anOverride_followsARename() {
        val diversified = ClassOverride("p1", "AAPL", ClassList.SECTOR, "Diversified")
        val store = store().apply { storedOverrides = listOf(diversified) }
        val vm = onList(store)
        vm.openRenameClassValue("Diversified")
        vm.typeClassValueName("Multi")
        vm.saveClassValueName()
        assertEquals(listOf(diversified.copy(value = "Multi")), store.storedOverrides)
        assertEquals(listOf(diversified.copy(value = "Multi")), vm.state.value.overrides)
        assertEquals(EffectiveClass("Multi", ClassOrigin.OVERRIDE), vm.state.value.holdingClasses.effective(ClassList.SECTOR, "p1", "AAPL"))
    }

    // Decision 2: renaming a value back to its own remembered text is refused; the layer stays.
    @Test
    fun renamingBackToItsOwnRememberedText_isRefused() {
        val store = store()
        val vm = onList(store, ClassList.GEOGRAPHY)
        vm.openRenameClassValue("United States")
        vm.typeClassValueName("USA")
        vm.saveClassValueName()
        vm.openRenameClassValue("USA")
        vm.typeClassValueName("united states")
        vm.saveClassValueName()
        assertEquals(ListResult.Refused(ListRefusal.Remembered("United States", "USA")), vm.state.value.listOutcome)
        assertEquals(ClassValueEdit(renaming = "USA", text = "united states"), vm.state.value.classValueEdit)
        assertEquals(listOf("USA", "Global"), store.storedClassLists.geographies)
    }

    // A rename to the same name stores nothing and closes the layer.
    @Test
    fun aRenameToTheSameName_storesNothing() {
        val store = store().apply { listSaveError = IllegalStateException("not to be called") }
        val vm = onList(store)
        vm.openRenameClassValue("Technology")
        vm.saveClassValueName()
        assertEquals(null, vm.state.value.classValueEdit)
        assertEquals(null, vm.state.value.listSaveRefusal)
        assertEquals(lists, store.storedClassLists)
    }

    // AC-58 and 4.9: a value in use cannot be deleted, a hidden holding's value included; nothing changes.
    @Test
    fun deletingAValueInUse_isRefused_aHiddenHoldingsToo() {
        val store = store()
        val vm = onList(store)
        vm.deleteClassValue("Technology")
        assertEquals(ListResult.Refused(ListRefusal.InUse("Technology", 1)), vm.state.value.listOutcome)
        vm.deleteClassValue("Diversified")
        assertEquals(ListResult.Refused(ListRefusal.InUse("Diversified", 1)), vm.state.value.listOutcome)
        assertEquals(lists, store.storedClassLists)
        assertEquals(lists, vm.state.value.classLists)
    }

    // AC-58: deleting an unused value works; decision 3: an override of a holding no longer there goes with it.
    @Test
    fun deletingAnUnusedValue_removesIt_andTheOverridesOfHoldingsNoLongerThere() {
        val gone = ClassOverride("p1", "GONE", ClassList.GEOGRAPHY, "Global")
        val store = store().apply { storedOverrides = listOf(gone) }
        val vm = onList(store, ClassList.GEOGRAPHY)
        vm.deleteClassValue("Global")
        assertEquals(listOf("United States"), store.storedClassLists.geographies)
        assertEquals(store.storedClassLists, vm.state.value.classLists)
        assertEquals(emptyList<ClassOverride>(), store.storedOverrides)
        assertEquals(emptyList<ClassOverride>(), vm.state.value.overrides)
        assertEquals(ListResult.Deleted(store.storedClassLists, "Global", listOf(gone)), vm.state.value.listOutcome)
    }

    // Risk: a failed save keeps the layer and the typed text and says so; a failed delete keeps the value and says so.
    @Test
    fun aFailedSaveOrDelete_keepsEverything_andSaysSo() {
        val store = store().apply { listSaveError = IllegalStateException("disk full") }
        val vm = onList(store, ClassList.GEOGRAPHY)
        vm.openAddClassValue()
        vm.typeClassValueName("Europe")
        vm.saveClassValueName()
        assertEquals(ClassValueEdit(renaming = null, text = "Europe"), vm.state.value.classValueEdit)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.listSaveRefusal)
        assertEquals(null, vm.state.value.listOutcome)
        assertEquals(lists, vm.state.value.classLists)
        vm.cancelClassValueName()
        assertEquals(null, vm.state.value.listSaveRefusal)
        vm.deleteClassValue("Global")
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.listSaveRefusal)
        assertEquals(lists, vm.state.value.classLists)
    }

    // C18: unreadable data on a save shows its screen.
    @Test
    fun anUnreadableStoreOnSave_showsTheUnreadableScreen() {
        val store = store()
        val vm = onList(store)
        // A value no holding uses, so that the delete reaches the store.
        vm.openAddClassValue()
        vm.typeClassValueName("Energy")
        vm.saveClassValueName()
        store.listSaveError = UnreadableDataException("The stored data could not be opened.")
        vm.deleteClassValue("Energy")
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
    }

    // Leaving by a tab in the bar drops the list screen, its layer and its lines.
    @Test
    fun leavingByATab_dropsTheListScreenAndItsLayer() {
        val vm = onList()
        vm.openAddClassValue()
        vm.typeClassValueName("Energy")
        vm.selectTab(Tab.OVERALL)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(null, vm.state.value.classListShown)
        assertEquals(null, vm.state.value.classValueEdit)
        assertEquals(null, vm.state.value.listOutcome)
    }

    // Decision 4: a refresh hands the store every held ticker, the hidden MSFT included.
    @Test
    fun aRefresh_passesTheHeldTickers_hiddenOnesIncluded() {
        val store = store()
        started(store)
        assertEquals(listOf(listOf("AAPL", "MSFT")), store.savedHeld.map { it.sorted() })
    }

    // Decision 4: with Diversified gone from the list (deleted while nothing used it), a held instrument's text that maps
    // to no value is added again by the next price save, offline too (the save runs either way), in the store and the state.
    @Test
    fun aRefresh_addsAHeldInstrumentsTextThatMapsToNoValue() {
        val store = store().apply { storedClassLists = lists.copy(sectors = listOf("Technology")) }
        val vm = started(store, online = { false })
        assertEquals(listOf("Technology", "Diversified"), store.storedClassLists.sectors)
        assertEquals(store.storedClassLists, vm.state.value.classLists)
    }
}
