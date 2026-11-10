package app.ghghorg.atsassets.ui

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
