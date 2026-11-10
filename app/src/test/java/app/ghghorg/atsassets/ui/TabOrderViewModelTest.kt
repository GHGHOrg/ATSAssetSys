package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
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
import org.junit.Before
import org.junit.Test

/**
 * 22: the tab order in the screen state, loaded with the records and saved on each move, and the Tab order screen
 * opened from Settings (wireframe 10f frame E).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabOrderViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val overallFirst = TabOrder.of(listOf(Tab.OVERALL, Tab.MORE, Tab.CASH, Tab.PORTFOLIOS))!!

    /** Started, on the Tab order screen, where a tab is moved. */
    private fun ordering(store: FakeStore): AppViewModel = started(store).also {
        it.selectTab(Tab.MORE)
        it.openSettings()
        it.openTabOrder()
    }

    @Test
    fun noSavedOrder_loadsTheDefault() {
        assertEquals(TabOrder.DEFAULT, started(FakeStore()).state.value.tabOrder)
    }

    @Test
    fun aSavedOrder_isLoadedWithTheRecords() {
        val store = FakeStore().also { it.storedTabOrder = overallFirst }
        assertEquals(overallFirst, started(store).state.value.tabOrder)
    }

    @Test
    fun theTabOrderScreen_opensFromSettingsOnly_andBackReturnsToSettings() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.MORE)
        vm.openTabOrder()
        assertEquals(Screen.TABS, vm.state.value.screen)
        vm.openSettings()
        vm.openTabOrder()
        assertEquals(Screen.TAB_ORDER, vm.state.value.screen)
        assertEquals(Tab.MORE, vm.state.value.tab)
        vm.closeTabOrder()
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
    }

    @Test
    fun theTabOrderScreen_doesNotOpenOverALayer() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.MORE)
        vm.openSettings()
        vm.openAutoLockChoice()
        vm.openTabOrder()
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
    }

    @Test
    fun aMove_isSaved_andAppliedAtOnce() {
        val store = FakeStore()
        val vm = ordering(store)
        vm.moveTabDown(Tab.MORE)
        val moved = TabOrder.of(listOf(Tab.CASH, Tab.MORE, Tab.PORTFOLIOS, Tab.OVERALL))!!
        assertEquals(listOf(moved), store.savedTabOrders)
        assertEquals(moved, vm.state.value.tabOrder)
        assertEquals(Screen.TAB_ORDER, vm.state.value.screen)
        assertEquals(Tab.MORE, vm.state.value.tab)
        assertFalse(vm.state.value.saving)
        assertNull(vm.state.value.tabOrderRefusal)
        vm.moveTabUp(Tab.OVERALL)
        assertEquals(listOf(Tab.CASH, Tab.MORE, Tab.OVERALL, Tab.PORTFOLIOS), vm.state.value.tabOrder.tabs)
    }

    @Test
    fun aMoveIsAppliedOnlyOnceItIsSaved() {
        val gate = CompletableDeferred<Unit>()
        val vm = ordering(FakeStore(gate = gate))
        vm.moveTabDown(Tab.MORE)
        assertEquals(TabOrder.DEFAULT, vm.state.value.tabOrder)
        gate.complete(Unit)
        assertEquals(listOf(Tab.CASH, Tab.MORE, Tab.PORTFOLIOS, Tab.OVERALL), vm.state.value.tabOrder.tabs)
    }

    @Test
    fun aSecondMoveWhileOneIsSaving_isIgnored() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeStore(gate = gate)
        val vm = ordering(store)
        vm.moveTabDown(Tab.MORE)
        vm.moveTabDown(Tab.MORE)
        gate.complete(Unit)
        assertEquals(1, store.savedTabOrders.size)
        assertEquals(listOf(Tab.CASH, Tab.MORE, Tab.PORTFOLIOS, Tab.OVERALL), vm.state.value.tabOrder.tabs)
    }

    @Test
    fun upOnTheFirst_andDownOnTheLast_saveNothing() {
        val store = FakeStore()
        val vm = ordering(store)
        vm.moveTabUp(Tab.MORE)
        vm.moveTabDown(Tab.OVERALL)
        assertEquals(emptyList<TabOrder>(), store.savedTabOrders)
        assertEquals(TabOrder.DEFAULT, vm.state.value.tabOrder)
    }

    @Test
    fun aMoveOffTheTabOrderScreen_isIgnored() {
        val store = FakeStore()
        val vm = started(store)
        vm.selectTab(Tab.MORE)
        vm.openSettings()
        vm.moveTabDown(Tab.MORE)
        assertEquals(emptyList<TabOrder>(), store.savedTabOrders)
        assertEquals(TabOrder.DEFAULT, vm.state.value.tabOrder)
    }

    @Test
    fun aFailedSave_keepsTheOrderBefore_andSaysSo() {
        val store = FakeStore().also { it.tabOrderSaveError = IllegalStateException("disk") }
        store.storedTabOrder = overallFirst
        val vm = ordering(store)
        vm.moveTabDown(Tab.OVERALL)
        assertEquals(overallFirst, vm.state.value.tabOrder)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.tabOrderRefusal)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun anUnreadableStoreOnSave_showsTheUnreadableScreen() {
        // Only this save fails: the start's price refresh would otherwise show the unreadable screen first.
        val store = FakeStore().also { it.tabOrderSaveError = UnreadableDataException("unreadable") }
        val vm = ordering(store)
        assertEquals(Screen.TAB_ORDER, vm.state.value.screen)
        vm.moveTabDown(Tab.MORE)
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
        assertEquals(TabOrder.DEFAULT, vm.state.value.tabOrder)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun aRefusal_clearsWhenTheScreenOpensAgain() {
        val vm = ordering(FakeStore().also { it.tabOrderSaveError = IllegalStateException("disk") })
        vm.moveTabDown(Tab.MORE)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.tabOrderRefusal)
        vm.closeTabOrder()
        vm.openTabOrder()
        assertNull(vm.state.value.tabOrderRefusal)
    }

    @Test
    fun aTabFromTheTabOrderScreen_showsThatTab() {
        val vm = ordering(FakeStore())
        assertEquals(Screen.TAB_ORDER, vm.state.value.screen)
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.CASH, vm.state.value.tab)
    }

    @Test
    fun theScreensModels_followAMove() {
        // CR2: the Tab order screen and Settings' row draw from the state's models.
        val vm = ordering(FakeStore())
        vm.moveTabDown(Tab.MORE)
        assertEquals(listOf(Tab.CASH, Tab.MORE, Tab.PORTFOLIOS, Tab.OVERALL), vm.state.value.tabOrderScreen.rows.map { it.tab })
        assertEquals(listOf(Tab.CASH, Tab.MORE, Tab.PORTFOLIOS, Tab.OVERALL), vm.state.value.settings.tabOrder)
    }

    @Test
    fun anUnlock_opensOverall_whereverItSits() {
        // Spec 6: after unlock the app opens on Overall, here the first tab.
        val store = FakeStore().also { it.storedTabOrder = overallFirst }
        val vm = started(store)
        vm.selectTab(Tab.CASH)
        vm.onUnlocked()
        assertEquals(Tab.OVERALL, vm.state.value.tab)
    }
}
