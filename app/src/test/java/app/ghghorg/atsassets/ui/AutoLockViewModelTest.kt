package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AutoLock
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

/**
 * 20: the auto-lock choice in the screen state, loaded with the records and saved on change (decision 3), and Settings
 * with its choice layer, opened from More's gear (wireframe 10f).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoLockViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Started, on Settings with the Auto-lock choice layer open, where a choice is made. */
    private fun choosing(store: FakeStore): AppViewModel = started(store).also {
        it.selectTab(Tab.MORE)
        it.openSettings()
        it.openAutoLockChoice()
    }

    private fun notStarted(store: FakeStore) = AppViewModel(
        storeProvider = { store },
        clock = clockAt("2026-10-26T12:00:00Z"),
        newId = { "c1" },
        priceSource = FakeSource(),
        isOnline = { true },
    )

    @Test
    fun beforeTheRecordsAreLoaded_theChoiceIsNotKnown() {
        assertNull(notStarted(FakeStore()).state.value.autoLock)
    }

    @Test
    fun noSavedChoice_loadsTheDefault() {
        assertEquals(AutoLock.EVERY_LEAVE, started(FakeStore()).state.value.autoLock)
    }

    @Test
    fun aSavedChoice_isLoadedWithTheRecords() {
        val store = FakeStore().also { it.storedAutoLock = AutoLock.IDLE_5_MIN }
        val vm = started(store)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(AutoLock.IDLE_5_MIN, vm.state.value.autoLock)
    }

    @Test
    fun unreadableData_showsTheUnreadableScreen_andTheChoiceStaysUnknown() {
        val vm = started(FakeStore(loadError = UnreadableDataException("unreadable")))
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
        assertNull(vm.state.value.autoLock)
    }

    @Test
    fun aChoiceIsSaved_andAppliedAtOnce() {
        val store = FakeStore()
        val vm = choosing(store)
        vm.chooseAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        assertEquals(listOf(AutoLock.PHONE_LOCK_OR_RESTART), store.savedAutoLocks)
        assertEquals(AutoLock.PHONE_LOCK_OR_RESTART, vm.state.value.autoLock)
        assertFalse(vm.state.value.saving)
        assertNull(vm.state.value.autoLockRefusal)
    }

    @Test
    fun aChoiceIsAppliedOnlyOnceItIsSaved() {
        val gate = CompletableDeferred<Unit>()
        val vm = choosing(FakeStore(gate = gate))
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertEquals(AutoLock.EVERY_LEAVE, vm.state.value.autoLock)
        gate.complete(Unit)
        assertEquals(AutoLock.IDLE_1_MIN, vm.state.value.autoLock)
    }

    @Test
    fun aSecondChoiceWhileOneIsSaving_isIgnored() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeStore(gate = gate)
        val vm = choosing(store)
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        vm.openAutoLockChoice()
        vm.chooseAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        gate.complete(Unit)
        assertEquals(listOf(AutoLock.IDLE_1_MIN), store.savedAutoLocks)
        assertEquals(AutoLock.IDLE_1_MIN, vm.state.value.autoLock)
    }

    @Test
    fun aFailedSave_keepsTheChoiceBefore_andSaysSo() {
        val store = FakeStore(saveError = IllegalStateException("disk"))
        store.storedAutoLock = AutoLock.IDLE_5_MIN
        val vm = choosing(store)
        vm.chooseAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        assertEquals(AutoLock.IDLE_5_MIN, vm.state.value.autoLock)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.autoLockRefusal)
        assertFalse(vm.state.value.saving)
        assertTrue(vm.state.value.settings.notSaved)
        assertEquals(AutoLock.IDLE_5_MIN, vm.state.value.settings.autoLock)
    }

    @Test
    fun anUnreadableStoreOnSave_showsTheUnreadableScreen() {
        val vm = choosing(FakeStore(saveError = UnreadableDataException("unreadable")))
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
        assertEquals(AutoLock.EVERY_LEAVE, vm.state.value.autoLock)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun aChoiceBeforeTheRecordsAreLoaded_isIgnored() {
        val store = FakeStore()
        val vm = notStarted(store)
        vm.chooseAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        assertEquals(emptyList<AutoLock>(), store.savedAutoLocks)
        assertNull(vm.state.value.autoLock)
    }

    @Test
    fun theSameChoiceAgain_savesNothing_andClosesTheLayer() {
        val store = FakeStore()
        val vm = choosing(store)
        vm.chooseAutoLock(AutoLock.EVERY_LEAVE)
        assertEquals(emptyList<AutoLock>(), store.savedAutoLocks)
        assertEquals(AutoLock.EVERY_LEAVE, vm.state.value.autoLock)
        assertFalse(vm.state.value.autoLockChoiceOpen)
    }

    // Settings and its choice layer (wireframe 10f frames A to C).

    @Test
    fun theGearOnMore_opensSettings_andBackReturnsToMore() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.MORE)
        vm.openSettings()
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
        assertEquals(Tab.MORE, vm.state.value.tab)
        vm.closeSettings()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.MORE, vm.state.value.tab)
    }

    @Test
    fun settingsOpensOnlyFromMore() {
        val vm = started(FakeStore())
        vm.openSettings()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.OVERALL, vm.state.value.tab)
    }

    @Test
    fun theChoiceLayer_opensOnSettingsOnly_andAChoiceClosesItAndShowsInTheRow() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.MORE)
        vm.openAutoLockChoice()
        assertFalse(vm.state.value.autoLockChoiceOpen)
        vm.openSettings()
        vm.openAutoLockChoice()
        assertTrue(vm.state.value.autoLockChoiceOpen)
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertFalse(vm.state.value.autoLockChoiceOpen)
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
        assertEquals(AutoLock.IDLE_1_MIN, vm.state.value.settings.autoLock)
    }

    @Test
    fun aChoiceWithTheLayerClosed_isIgnored() {
        val store = FakeStore()
        val vm = started(store)
        vm.selectTab(Tab.MORE)
        vm.openSettings()
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertEquals(emptyList<AutoLock>(), store.savedAutoLocks)
        vm.selectTab(Tab.OVERALL)
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertEquals(emptyList<AutoLock>(), store.savedAutoLocks)
    }

    @Test
    fun backOrATapOutside_closesTheLayer_andKeepsTheChoice() {
        val vm = choosing(FakeStore())
        vm.closeAutoLockChoice()
        assertFalse(vm.state.value.autoLockChoiceOpen)
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
        assertEquals(AutoLock.EVERY_LEAVE, vm.state.value.autoLock)
    }

    @Test
    fun aRefusal_clearsWhenSettingsOrTheLayerOpensAgain() {
        val vm = choosing(FakeStore(saveError = IllegalStateException("disk")))
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertTrue(vm.state.value.settings.notSaved)
        vm.openAutoLockChoice()
        assertFalse(vm.state.value.settings.notSaved)
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertTrue(vm.state.value.settings.notSaved)
        vm.closeSettings()
        vm.openSettings()
        assertFalse(vm.state.value.settings.notSaved)
    }

    @Test
    fun aTabFromSettings_showsThatTab_andClosesTheLayer() {
        val vm = choosing(FakeStore())
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.CASH, vm.state.value.tab)
        assertFalse(vm.state.value.autoLockChoiceOpen)
    }

    @Test
    fun anUnlock_keepsSettings_andClosesTheLayer() {
        val vm = choosing(FakeStore())
        vm.onUnlocked()
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
        assertFalse(vm.state.value.autoLockChoiceOpen)
    }
}
