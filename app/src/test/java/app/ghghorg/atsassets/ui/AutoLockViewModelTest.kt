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
import org.junit.Before
import org.junit.Test

/** 20: the auto-lock choice in the screen state, loaded with the records and saved on change (decision 3). */
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

    @Test
    fun beforeTheRecordsAreLoaded_theChoiceIsNotKnown() {
        val vm = AppViewModel(
            storeProvider = { FakeStore() },
            clock = clockAt("2026-10-26T12:00:00Z"),
            newId = { "c1" },
            priceSource = FakeSource(),
            isOnline = { true },
        )
        assertNull(vm.state.value.autoLock)
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
        val vm = started(store)
        vm.chooseAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        assertEquals(listOf(AutoLock.PHONE_LOCK_OR_RESTART), store.savedAutoLocks)
        assertEquals(AutoLock.PHONE_LOCK_OR_RESTART, vm.state.value.autoLock)
        assertFalse(vm.state.value.saving)
        assertNull(vm.state.value.autoLockRefusal)
    }

    @Test
    fun aChoiceIsAppliedOnlyOnceItIsSaved() {
        val gate = CompletableDeferred<Unit>()
        val vm = started(FakeStore(gate = gate))
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertEquals(AutoLock.EVERY_LEAVE, vm.state.value.autoLock)
        gate.complete(Unit)
        assertEquals(AutoLock.IDLE_1_MIN, vm.state.value.autoLock)
    }

    @Test
    fun aSecondChoiceWhileOneIsSaving_isIgnored() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeStore(gate = gate)
        val vm = started(store)
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        vm.chooseAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        gate.complete(Unit)
        assertEquals(listOf(AutoLock.IDLE_1_MIN), store.savedAutoLocks)
        assertEquals(AutoLock.IDLE_1_MIN, vm.state.value.autoLock)
    }

    @Test
    fun aFailedSave_keepsTheChoiceBefore_andSaysSo() {
        val store = FakeStore(saveError = IllegalStateException("disk"))
        store.storedAutoLock = AutoLock.IDLE_5_MIN
        val vm = started(store)
        vm.chooseAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        assertEquals(AutoLock.IDLE_5_MIN, vm.state.value.autoLock)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.autoLockRefusal)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun anUnreadableStoreOnSave_showsTheUnreadableScreen() {
        val vm = started(FakeStore(saveError = UnreadableDataException("unreadable")))
        vm.chooseAutoLock(AutoLock.IDLE_1_MIN)
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
        assertEquals(AutoLock.EVERY_LEAVE, vm.state.value.autoLock)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun aChoiceBeforeTheRecordsAreLoaded_isIgnored() {
        val store = FakeStore()
        val vm = AppViewModel(
            storeProvider = { store },
            clock = clockAt("2026-10-26T12:00:00Z"),
            newId = { "c1" },
            priceSource = FakeSource(),
            isOnline = { true },
        )
        vm.chooseAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        assertEquals(emptyList<AutoLock>(), store.savedAutoLocks)
        assertNull(vm.state.value.autoLock)
    }

    @Test
    fun theSameChoiceAgain_savesNothing() {
        val store = FakeStore()
        val vm = started(store)
        vm.chooseAutoLock(AutoLock.EVERY_LEAVE)
        assertEquals(emptyList<AutoLock>(), store.savedAutoLocks)
        assertEquals(AutoLock.EVERY_LEAVE, vm.state.value.autoLock)
    }
}
