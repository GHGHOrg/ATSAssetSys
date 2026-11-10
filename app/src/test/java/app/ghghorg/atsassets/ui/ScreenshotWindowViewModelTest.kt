package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Tab
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

/** 21: the screenshot window's time left in the screen state, as MainActivity passes it (K8 owns the window). */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotWindowViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun offAtStart() {
        assertNull(started(FakeStore()).state.value.screenshotWindowLeftMs)
    }

    @Test
    fun theTimeLeft_isShownAsPassed_eachTick() {
        val vm = started(FakeStore())
        vm.onScreenshotWindow(180_000L)
        assertEquals(180_000L, vm.state.value.screenshotWindowLeftMs)
        vm.onScreenshotWindow(161_000L)
        assertEquals(161_000L, vm.state.value.screenshotWindowLeftMs)
    }

    @Test
    fun theWindowEnding_showsOff() {
        val vm = started(FakeStore())
        vm.onScreenshotWindow(42_000L)
        vm.onScreenshotWindow(null)
        assertNull(vm.state.value.screenshotWindowLeftMs)
    }

    /** Started, on Settings. */
    private fun onSettings(): AppViewModel = started(FakeStore()).also {
        it.selectTab(Tab.MORE)
        it.openSettings()
    }

    @Test
    fun theLayer_opensOnSettingsOnly_andCloses() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.MORE)
        vm.openScreenshotLayer()
        assertFalse(vm.state.value.screenshotLayerOpen)
        vm.openSettings()
        vm.openScreenshotLayer()
        assertTrue(vm.state.value.screenshotLayerOpen)
        vm.closeScreenshotLayer()
        assertFalse(vm.state.value.screenshotLayerOpen)
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
    }

    @Test
    fun theLayer_staysOpen_whileTheWindowStartsAndEnds_showingItsTimeLeft() {
        val vm = onSettings()
        vm.openScreenshotLayer()
        vm.onScreenshotWindow(161_000L)
        assertTrue(vm.state.value.screenshotLayerOpen)
        assertEquals(ScreenshotTimeLeft("2:41", 2, 41), vm.state.value.settings.screenshotLeft)
        vm.onScreenshotWindow(null)
        assertTrue(vm.state.value.screenshotLayerOpen)
        assertNull(vm.state.value.settings.screenshotLeft)
    }

    @Test
    fun theLayer_isNotOpenedOverTheAutoLockChoice() {
        val vm = onSettings()
        vm.openAutoLockChoice()
        vm.openScreenshotLayer()
        assertFalse(vm.state.value.screenshotLayerOpen)
    }

    @Test
    fun backToMore_aTab_orAnUnlock_closesTheLayer() {
        val back = onSettings().also { it.openScreenshotLayer() }
        back.closeSettings()
        assertFalse(back.state.value.screenshotLayerOpen)

        val tab = onSettings().also { it.openScreenshotLayer() }
        tab.selectTab(Tab.CASH)
        assertFalse(tab.state.value.screenshotLayerOpen)

        val unlock = onSettings().also { it.openScreenshotLayer() }
        unlock.onUnlocked()
        assertFalse(unlock.state.value.screenshotLayerOpen)
        assertEquals(Screen.SETTINGS, unlock.state.value.screen)
    }

    @Test
    fun theTimeLeft_doesNotTouchTheScreenOrTheRecords() {
        val vm = started(FakeStore())
        vm.selectTab(Tab.MORE)
        vm.openSettings()
        val before = vm.state.value
        vm.onScreenshotWindow(90_000L)
        assertEquals(before.copy(screenshotWindowLeftMs = 90_000L), vm.state.value)
    }
}
