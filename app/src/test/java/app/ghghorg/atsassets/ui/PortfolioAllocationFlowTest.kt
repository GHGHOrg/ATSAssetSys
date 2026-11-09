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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

// 19 step 3: opening Allocation from a portfolio and from More, its chooser, Back, and Create a portfolio from it
// (view 4, wireframe 04; navigation outline; 10d; decisions 1 and 2).
@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioAllocationFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 960_000L, CashType.OPENING_BALANCE, "", 1L)

    private val mainAndTrust = listOf(Portfolio("p1", "Main", 1L), Portfolio("p2", "Trust", 2L))

    private fun store(portfolios: List<Portfolio> = mainAndTrust): FakeStore = FakeStore(stored = listOf(opening)).apply { storedPortfolios = portfolios }

    private fun AppViewModel.at(): Pair<Screen, Tab> = state.value.screen to state.value.tab

    // Marker 1: from a portfolio's screen it opens on that portfolio; the chooser redraws it; Back returns to that portfolio.
    @Test
    fun fromAPortfolio_itOpensOnThatPortfolio_andBackReturnsToIt_whateverTheChooserShows() {
        val vm = started(store())
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p2")
        vm.openAllocation()
        assertEquals(Screen.ALLOCATION, vm.state.value.screen)
        assertEquals("p2", vm.state.value.portfolioAllocation.shown?.id)
        vm.openPortfolioChoice()
        assertTrue(vm.state.value.portfolioChoiceOpen)
        vm.choosePortfolio("p1")
        assertEquals("p1", vm.state.value.portfolioAllocation.shown?.id)
        assertFalse("A choice closes the layer", vm.state.value.portfolioChoiceOpen)
        vm.closeAllocation()
        assertEquals(Screen.PORTFOLIO to Tab.PORTFOLIOS, vm.at())
        assertEquals("p2", vm.state.value.portfolioId)
        // Not saved: opened again, it is on the portfolio you came from.
        vm.openAllocation()
        assertEquals("p2", vm.state.value.portfolioAllocation.shown?.id)
    }

    // Decision 1: from More it opens on the first portfolio; Back returns to More.
    @Test
    fun fromMore_itOpensOnTheFirstPortfolio_andBackReturnsToMore() {
        val vm = started(store())
        vm.selectTab(Tab.MORE)
        vm.openAllocation()
        assertEquals(Screen.ALLOCATION to Tab.MORE, vm.at())
        assertEquals("p1", vm.state.value.portfolioAllocation.shown?.id)
        vm.closeAllocation()
        assertEquals(Screen.TABS to Tab.MORE, vm.at())
    }

    // Only a portfolio's screen and More open it; the chooser opens only on it; an unlock closes the chooser.
    @Test
    fun elsewhere_itDoesNotOpen_andTheChooserOnlyOnIt_closedByAnUnlock() {
        val vm = started(store())
        vm.openAllocation()
        assertEquals(Screen.TABS to Tab.OVERALL, vm.at())
        vm.openPortfolioChoice()
        assertFalse(vm.state.value.portfolioChoiceOpen)
        vm.selectTab(Tab.MORE)
        vm.openAllocation()
        vm.openPortfolioChoice()
        vm.onUnlocked()
        assertFalse(vm.state.value.portfolioChoiceOpen)
        assertEquals(Screen.ALLOCATION, vm.state.value.screen)
        vm.closePortfolioChoice()
        vm.choosePortfolio("unknown")
        assertEquals("p1", vm.state.value.portfolioAllocation.shown?.id)
    }

    // Marker 4 and 10d: with no portfolio, Create a portfolio opens the dialog over this screen, and after Create the
    // screen shows the new portfolio.
    @Test
    fun withNoPortfolio_createAPortfolio_opensHere_andTheScreenShowsIt() {
        val vm = started(store(emptyList()), newId = { "new1" })
        vm.selectTab(Tab.MORE)
        vm.openAllocation()
        assertEquals(null, vm.state.value.portfolioAllocation.shown)
        vm.openPortfolioDialog()
        assertNotNull(vm.state.value.portfolioDialog)
        vm.onPortfolioNameChange("Growth")
        vm.createPortfolio()
        assertEquals(Screen.ALLOCATION to Tab.MORE, vm.at())
        assertEquals(PortfolioChoiceModel("new1", "Growth"), vm.state.value.portfolioAllocation.shown)
    }

    // Marker 4 and 4.2: before the opening balance exists, Create a portfolio says so instead of opening the dialog.
    @Test
    fun beforeTheOpeningBalance_createAPortfolio_asksForItHere() {
        val vm = started(FakeStore(stored = emptyList()))
        vm.selectTab(Tab.MORE)
        vm.openAllocation()
        vm.openPortfolioDialog()
        assertEquals(null, vm.state.value.portfolioDialog)
        assertTrue(vm.state.value.portfolioNeedsOpeningBalance)
        assertEquals(Screen.ALLOCATION, vm.state.value.screen)
    }
}
