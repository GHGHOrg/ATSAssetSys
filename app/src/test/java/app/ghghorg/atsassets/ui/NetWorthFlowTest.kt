package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Portfolio
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

// 17 step 3: opening Net Worth (Total) from Overall's net worth and from More, and Back to each (navigation outline;
// wireframe 02 marker 1; decision 1).
@OptIn(ExperimentalCoroutinesApi::class)
class NetWorthFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun store(): FakeStore = FakeStore(
        stored = listOf(CashEntry("o1", LocalDate.of(2026, 1, 1), 960_000L, CashType.OPENING_BALANCE, "", 1L)),
    ).apply { storedPortfolios = listOf(Portfolio("p1", "Portfolio P", 1L)) }

    private fun AppViewModel.at(): Pair<Screen, Tab> = state.value.screen to state.value.tab

    @Test
    fun fromOverall_netWorthOpensTheScreen_andBackReturnsToOverall() {
        val vm = started(store())
        vm.openNetWorth()
        assertEquals(Screen.NET_WORTH to Tab.OVERALL, vm.at())
        vm.closeNetWorth()
        assertEquals(Screen.TABS to Tab.OVERALL, vm.at())
    }

    @Test
    fun fromMore_theEntryOpensTheScreen_andBackReturnsToMore() {
        val vm = started(store())
        vm.selectTab(Tab.MORE)
        vm.openNetWorth()
        assertEquals(Screen.NET_WORTH to Tab.MORE, vm.at())
        vm.closeNetWorth()
        assertEquals(Screen.TABS to Tab.MORE, vm.at())
    }

    // Only Overall and More offer it; nothing else opens it, and Back elsewhere does nothing.
    @Test
    fun onOtherTabsAndScreens_openingIsIgnored_andBackElsewhereDoesNothing() {
        val vm = started(store())
        vm.selectTab(Tab.CASH)
        vm.openNetWorth()
        assertEquals(Screen.TABS to Tab.CASH, vm.at())
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openNetWorth()
        assertEquals(Screen.PORTFOLIO to Tab.PORTFOLIOS, vm.at())
        vm.closeNetWorth()
        assertEquals(Screen.PORTFOLIO to Tab.PORTFOLIOS, vm.at())
    }

    // The Buy/Sell menu's layer is over Overall: TalkBack reads only the layer, and the net worth under it does not open.
    @Test
    fun whileTheBuySellMenuIsOpen_netWorthDoesNotOpen() {
        val vm = started(store())
        vm.openBuySellMenu()
        vm.openNetWorth()
        assertEquals(Screen.TABS to Tab.OVERALL, vm.at())
    }

    // Risk 3: a lock and unlock on the screen keeps it, and Back still returns to the tab it came from.
    @Test
    fun anUnlockOnTheScreen_keepsIt_andBackStillReturnsToMore() {
        val vm = started(store())
        vm.selectTab(Tab.MORE)
        vm.openNetWorth()
        vm.onUnlocked()
        assertEquals(Screen.NET_WORTH to Tab.MORE, vm.at())
        vm.closeNetWorth()
        assertEquals(Screen.TABS to Tab.MORE, vm.at())
    }

    // The bottom bar stays visible on the screen (spec 6): a tab tap leaves it for that tab.
    @Test
    fun aTabTapOnTheScreen_goesToThatTab() {
        val vm = started(store())
        vm.openNetWorth()
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS to Tab.CASH, vm.at())
    }
}
