package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOrigin
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.EffectiveClass
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
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

// 24 step 4: a holding's override in the screen state (4.9, view 6, wireframe 10f frame H; decisions 2 and 3; AC-22).
@OptIn(ExperimentalCoroutinesApi::class)
class ClassOverrideFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)

    private val aaplBuy =
        Transaction(
            id = "b1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 1, 2),
            enteredSeq = 2L,
            quantity = BigDecimal("10"),
            totalCents = 150_000L,
        )

    private val aaplLine = CashEntry("c-b1", LocalDate.of(2026, 1, 2), -150_000L, CashType.TRADE, "", 2L, transactionId = "b1")

    private val healthCare = ClassOverride("p1", "AAPL", ClassList.SECTOR, "Health Care")

    // AAPL's source gave "technology" and "US"; the sector list also holds "Health Care".
    private fun store(): FakeStore = FakeStore(stored = listOf(opening(), aaplLine)).also {
        it.storedPortfolios = listOf(main)
        it.storedTransactions = listOf(aaplBuy)
        it.storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, profile = SourceProfile("technology", "US")))
        it.storedClassLists = ClassLists(sectors = listOf("Technology", "Health Care"), geographies = listOf("US"))
    }

    /** Started on AAPL's Classification tab, offline so no refresh runs. */
    private fun onClassification(store: FakeStore = store()): AppViewModel {
        val vm = started(store, online = { false })
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        vm.selectHoldingTab(HoldingTab.CLASSIFICATION)
        return vm
    }

    private fun sectorNames(vm: AppViewModel): List<SliceName> = vm.state.value.allocation(AllocationBy.SECTOR)!!.rows.map { it.name }

    // AC-12: the stored overrides load with the records, and the breakdowns use them (4.9: the override wins).
    @Test
    fun theStoredOverrides_loadWithTheRecords_andTheBreakdownsUseThem() {
        val vm = onClassification(store().apply { storedOverrides = listOf(healthCare) })
        assertEquals(listOf(healthCare), vm.state.value.overrides)
        assertEquals(listOf(SliceName.Cash, SliceName.Named("Health Care")), sectorNames(vm))
    }

    // CR2's wiring test: the picker's screen and the Classification tab draw from the state's models.
    @Test
    fun thePickerAndTheTab_drawFromTheStatesModels() {
        val vm = onClassification()
        vm.openClassPicker(ClassList.SECTOR)
        assertEquals(
            listOf(ClassPickerRowModel("Technology", selected = true), ClassPickerRowModel("Health Care", selected = false)),
            vm.state.value.classPickerModel!!.rows,
        )
        vm.chooseClassValue("Health Care")
        vm.saveClassOverride()
        assertEquals(EffectiveClass("Health Care", ClassOrigin.OVERRIDE), vm.state.value.holdingClassification!!.sector)
        assertEquals(EffectiveClass("US", ClassOrigin.SOURCE), vm.state.value.holdingClassification!!.geography)
        assertEquals(null, vm.state.value.classPickerModel)
    }

    // Decision 2: Edit on a row opens the picker for that list, on the value shown (frame H: the current value checked).
    @Test
    fun edit_opensThePickerForItsList_onTheValueShown() {
        val vm = onClassification()
        vm.openClassPicker(ClassList.SECTOR)
        assertEquals(Screen.CLASS_PICKER, vm.state.value.screen)
        assertEquals(ClassList.SECTOR, vm.state.value.classPicker)
        assertEquals("Technology", vm.state.value.classPickerChoice)
    }

    // 4.9 and AC-22: Save stores the pick and applies it once stored; the tab and the breakdowns follow at once.
    @Test
    fun save_storesThePick_backOnTheTab_andTheBreakdownFollows() {
        val store = store()
        val vm = onClassification(store)
        vm.openClassPicker(ClassList.SECTOR)
        vm.chooseClassValue("Health Care")
        vm.saveClassOverride()
        assertEquals(listOf(healthCare), store.storedOverrides)
        assertEquals(listOf(healthCare), vm.state.value.overrides)
        assertEquals(Screen.HOLDING, vm.state.value.screen)
        assertEquals(HoldingTab.CLASSIFICATION, vm.state.value.holdingTab)
        assertEquals(listOf(SliceName.Cash, SliceName.Named("Health Care")), sectorNames(vm))
    }

    // Frame H: Cancel (or Back) changes nothing.
    @Test
    fun cancel_changesNothing() {
        val store = store()
        val vm = onClassification(store)
        vm.openClassPicker(ClassList.SECTOR)
        vm.chooseClassValue("Health Care")
        vm.cancelClassPicker()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
        assertEquals(emptyList<ClassOverride>(), store.storedOverrides)
        assertEquals(listOf(SliceName.Cash, SliceName.Named("Technology")), sectorNames(vm))
    }

    // 4.9 and AC-22: Clear override returns to the source's value; the breakdown follows.
    @Test
    fun clear_returnsToTheSourcesValue() {
        val store = store().apply { storedOverrides = listOf(healthCare) }
        val vm = onClassification(store)
        vm.clearClassOverride(ClassList.SECTOR)
        assertEquals(emptyList<ClassOverride>(), store.storedOverrides)
        assertEquals(emptyList<ClassOverride>(), vm.state.value.overrides)
        assertEquals(listOf(SliceName.Cash, SliceName.Named("Technology")), sectorNames(vm))
    }

    // Risk 2: a failed save keeps the picker and the pick and says so; a failed clear keeps the override and says so.
    @Test
    fun aFailedSaveOrClear_keepsEverything_andSaysSo() {
        val store = store().apply { overrideSaveError = IllegalStateException("disk full") }
        val vm = onClassification(store)
        vm.openClassPicker(ClassList.SECTOR)
        vm.chooseClassValue("Health Care")
        vm.saveClassOverride()
        assertEquals(Screen.CLASS_PICKER, vm.state.value.screen)
        assertEquals("Health Care", vm.state.value.classPickerChoice)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.overrideRefusal)
        assertEquals(emptyList<ClassOverride>(), vm.state.value.overrides)

        val kept = store().apply { storedOverrides = listOf(healthCare) }
        val other = onClassification(kept)
        kept.overrideSaveError = IllegalStateException("disk full")
        other.clearClassOverride(ClassList.SECTOR)
        assertEquals(Screen.HOLDING, other.state.value.screen)
        assertEquals(listOf(healthCare), other.state.value.overrides)
        assertEquals(SaveRefusal.SAVE_FAILED, other.state.value.overrideRefusal)
    }

    // C18: unreadable data on a save shows its screen.
    @Test
    fun anUnreadableStoreOnSave_showsTheUnreadableScreen() {
        val store = store()
        val vm = onClassification(store)
        vm.openClassPicker(ClassList.SECTOR)
        assertEquals(Screen.CLASS_PICKER, vm.state.value.screen)
        store.overrideSaveError = UnreadableDataException("The stored data could not be opened.")
        vm.chooseClassValue("Health Care")
        vm.saveClassOverride()
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
    }

    // Step 7 fix (your A; phone check): Edit, then Save at once, stores nothing and marks nothing "Override"; the picker
    // stays open with Save off until another value is picked, and picking the shown value again turns it off again.
    @Test
    fun saveWithTheValueShown_storesNothing_andSaveIsOffUntilAnotherIsPicked() {
        val store = store()
        val vm = onClassification(store)
        vm.openClassPicker(ClassList.SECTOR)
        assertEquals(false, vm.state.value.classPickerModel!!.canSave)
        vm.saveClassOverride()
        assertEquals(Screen.CLASS_PICKER, vm.state.value.screen)
        assertEquals(emptyList<ClassOverride>(), store.storedOverrides)
        assertEquals(emptyList<ClassOverride>(), vm.state.value.overrides)
        vm.chooseClassValue("Health Care")
        assertEquals(true, vm.state.value.classPickerModel!!.canSave)
        vm.chooseClassValue("Technology")
        assertEquals(false, vm.state.value.classPickerModel!!.canSave)
        vm.cancelClassPicker()
        assertEquals(EffectiveClass("Technology", ClassOrigin.SOURCE), vm.state.value.holdingClassification!!.sector)
    }

    // The same over an override: its own value picked saves nothing; the source's value can replace it.
    @Test
    fun overAnOverride_itsValueSavesNothing_theSourcesValueCanBeSaved() {
        val store = store().apply { storedOverrides = listOf(healthCare) }
        val vm = onClassification(store)
        vm.openClassPicker(ClassList.SECTOR)
        assertEquals("Health Care", vm.state.value.classPickerChoice)
        assertEquals(false, vm.state.value.classPickerModel!!.canSave)
        vm.chooseClassValue("Technology")
        vm.saveClassOverride()
        val technology = ClassOverride("p1", "AAPL", ClassList.SECTOR, "Technology")
        assertEquals(listOf(technology), store.storedOverrides)
        assertEquals(EffectiveClass("Technology", ClassOrigin.OVERRIDE), vm.state.value.holdingClassification!!.sector)
    }

    // Deploy review (your A): a failed clear's "Not saved" belongs to that visit; leaving the holding (Back, or a tab in
    // the bar) drops it, so the holding opened next does not say it.
    @Test
    fun aFailedClearsNotSaved_isDroppedOnLeavingTheHolding() {
        val store = store().apply { storedOverrides = listOf(healthCare) }
        val vm = onClassification(store)
        store.overrideSaveError = IllegalStateException("disk full")
        vm.clearClassOverride(ClassList.SECTOR)
        assertEquals(true, vm.state.value.holdingClassification!!.notSaved)
        vm.closeHolding()
        vm.openHolding("AAPL")
        vm.selectHoldingTab(HoldingTab.CLASSIFICATION)
        assertEquals(false, vm.state.value.holdingClassification!!.notSaved)

        vm.clearClassOverride(ClassList.SECTOR)
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        vm.selectHoldingTab(HoldingTab.CLASSIFICATION)
        assertEquals(false, vm.state.value.holdingClassification!!.notSaved)
    }

    // Deploy review (your A): leaving the picker by a tab in the bar drops it, as Cancel does, with nothing stored.
    @Test
    fun leavingThePickerByATab_dropsIt() {
        val store = store()
        val vm = onClassification(store)
        vm.openClassPicker(ClassList.SECTOR)
        vm.chooseClassValue("Health Care")
        vm.selectTab(Tab.OVERALL)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(null, vm.state.value.classPicker)
        assertEquals(null, vm.state.value.classPickerChoice)
        assertEquals(null, vm.state.value.classPickerModel)
        assertEquals(emptyList<ClassOverride>(), store.storedOverrides)
    }

    // 4.9: only a value of the list can be picked; with none picked (an ETF on an empty choice), Save does nothing.
    @Test
    fun onlyAValueOfTheList_canBePicked_andNothingPicked_savesNothing() {
        val store = store().apply { storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, profile = SourceProfile(null, null))) }
        val vm = onClassification(store)
        vm.openClassPicker(ClassList.SECTOR)
        assertEquals(null, vm.state.value.classPickerChoice)
        vm.chooseClassValue("Energy")
        assertEquals(null, vm.state.value.classPickerChoice)
        vm.saveClassOverride()
        assertEquals(Screen.CLASS_PICKER, vm.state.value.screen)
        assertEquals(emptyList<ClassOverride>(), store.storedOverrides)
    }
}
