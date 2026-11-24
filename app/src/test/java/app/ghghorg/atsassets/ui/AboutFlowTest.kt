package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.BuildConfig
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

// 41 (CR5, AC-70): About from More, its rows opening in place, and Back to More (wireframe 11; the ViewModel wiring test).
@OptIn(ExperimentalCoroutinesApi::class)
class AboutFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun store(): FakeStore = FakeStore(stored = listOf(CashEntry("o1", LocalDate.of(2026, 1, 1), 960_000L, CashType.OPENING_BALANCE, "", 1L)))

    private fun AppViewModel.at(): Pair<Screen, Tab> = state.value.screen to state.value.tab

    private fun onAbout(): AppViewModel {
        val vm = started(store())
        vm.selectTab(Tab.MORE)
        vm.openAbout()
        return vm
    }

    @Test
    fun fromMore_itOpens_withTheBuildsVersion_andBackReturnsToMore() {
        val vm = onAbout()
        assertEquals(Screen.ABOUT to Tab.MORE, vm.at())
        assertEquals(About.of(BuildConfig.VERSION_NAME, emptySet()), vm.state.value.about)
        vm.closeAbout()
        assertEquals(Screen.TABS to Tab.MORE, vm.at())
    }

    @Test
    fun onAnotherTab_openingIsIgnored() {
        val vm = started(store())
        vm.openAbout()
        assertEquals(Screen.TABS to Tab.OVERALL, vm.at())
    }

    @Test
    fun aRow_opensAndCloses_moreThanOneCanBeOpen_andAllAreClosedOnTheNextVisit() {
        val vm = onAbout()
        vm.toggleAboutRow("SQLCipher for Android")
        vm.toggleAboutRow("AndroidX Room")
        assertEquals(setOf("AndroidX Room", "SQLCipher for Android"), vm.state.value.about.rows.filter { it.open }.map { it.name }.toSet())
        vm.toggleAboutRow("AndroidX Room")
        assertEquals(listOf("SQLCipher for Android"), vm.state.value.about.rows.filter { it.open }.map { it.name })
        vm.closeAbout()
        vm.openAbout()
        assertEquals(false, vm.state.value.about.rows.any { it.open })
    }

    @Test
    fun anUnknownRow_isIgnored() {
        val vm = onAbout()
        vm.toggleAboutRow("Not a library")
        assertEquals(emptySet<String>(), vm.state.value.aboutOpen)
    }

    // The bottom bar stays visible on the screen (spec 6): a tab tap leaves it for that tab.
    @Test
    fun aTabTapOnTheScreen_goesToThatTab() {
        val vm = onAbout()
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS to Tab.CASH, vm.at())
    }
}
