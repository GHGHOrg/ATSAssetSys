package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.ProfileAnswer
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// 18 step 3: the "Allocation by" choice in the screen state (view 1; AC-43; decision 1) and the breakdowns' wiring.
@OptIn(ExperimentalCoroutinesApi::class)
class AllocationFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val at = Instant.ofEpochSecond(1_793_304_000L)

    /** $9,600 cash and 8 AAPL at a cost of $1,200, its last price $160 (Example A). */
    private fun store(): FakeStore = FakeStore(
        stored = listOf(CashEntry("o1", LocalDate.of(2026, 1, 1), 960_000L, CashType.OPENING_BALANCE, "", 1L)),
    ).apply {
        storedPortfolios = listOf(Portfolio("p1", "Portfolio P", 1L))
        storedTransactions =
            listOf(
                Transaction(
                    id = "b2",
                    portfolioId = "p1",
                    ticker = "AAPL",
                    type = TransactionType.BUY,
                    date = LocalDate.of(2026, 2, 1),
                    enteredSeq = 2L,
                    quantity = BigDecimal("8"),
                    totalCents = 120_000L,
                ),
            )
        storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, true, Price(BigDecimal("160"), at)))
    }

    // AC-43: Overall starts on Asset type; the choice is kept while the app is open.
    @Test
    fun theChoice_startsOnAssetType_andIsKeptAcrossTabs() {
        val vm = started(store())
        assertEquals(AllocationBy.ASSET_TYPE, vm.state.value.allocationBy)
        vm.openAllocationMenu()
        assertTrue(vm.state.value.allocationMenuOpen)
        vm.chooseAllocation(AllocationBy.HOLDING)
        assertEquals(AllocationBy.HOLDING, vm.state.value.allocationBy)
        assertFalse("A choice closes the layer", vm.state.value.allocationMenuOpen)
        vm.selectTab(Tab.CASH)
        vm.selectTab(Tab.OVERALL)
        assertEquals(AllocationBy.HOLDING, vm.state.value.allocationBy)
    }

    // View 1: not saved, so each unlock starts on Asset type again, with the layer closed.
    @Test
    fun anUnlock_goesBackToAssetType_andClosesTheLayer() {
        val vm = started(store())
        vm.chooseAllocation(AllocationBy.PORTFOLIO)
        vm.openAllocationMenu()
        vm.onUnlocked()
        assertEquals(AllocationBy.ASSET_TYPE, vm.state.value.allocationBy)
        assertFalse(vm.state.value.allocationMenuOpen)
    }

    // Back or a tap outside closes the layer; while it is open, net worth and the rows under it do not open the screen.
    @Test
    fun whileTheLayerIsOpen_netWorthDoesNotOpen_andClosingKeepsTheChoice() {
        val vm = started(store())
        vm.openAllocationMenu()
        vm.openNetWorth()
        assertEquals(Screen.TABS, vm.state.value.screen)
        vm.closeAllocationMenu()
        assertFalse(vm.state.value.allocationMenuOpen)
        assertEquals(AllocationBy.ASSET_TYPE, vm.state.value.allocationBy)
        vm.openNetWorth()
        assertEquals(Screen.NET_WORTH, vm.state.value.screen)
    }

    // The choice is only on Overall: elsewhere the layer does not open, and a tab change closes it.
    @Test
    fun theLayer_opensOnlyOnOverall_andATabChangeClosesIt() {
        val vm = started(store())
        vm.selectTab(Tab.MORE)
        vm.openAllocationMenu()
        assertFalse(vm.state.value.allocationMenuOpen)
        vm.selectTab(Tab.OVERALL)
        vm.openAllocationMenu()
        vm.selectTab(Tab.CASH)
        assertFalse(vm.state.value.allocationMenuOpen)
    }

    // AC-32: the breakdowns at the stored price; By portfolio carries the composition's figures (wireframe 02 marker 2).
    @Test
    fun theBreakdowns_useTheStoredPrice_andByPortfolioMatchesTheComposition() {
        val state = started(store(), online = { false }).state.value
        assertEquals(listOf("88.2", "11.8", "0.0"), state.allocation(AllocationBy.ASSET_TYPE)!!.rows.map { it.percent })
        val byPortfolio = state.allocation(AllocationBy.PORTFOLIO)!!.rows.map { it.amount }
        val composition = state.netWorthTotal.composition!!
        assertEquals(listOf(composition.cash) + composition.portfolios.map { it.amount }, byPortfolio)
    }

    // 23b, CR2's wiring test: the stored profile through the stored lists reaches both levels' sector and geography.
    @Test
    fun theStoredProfile_reachesTheSectorAndGeographyBreakdowns() {
        val store =
            store().apply {
                storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, true, Price(BigDecimal("160"), at), SourceProfile("technology", "US")))
                storedClassLists = ClassLists(sectors = listOf("Technology"), geographies = listOf("US"))
            }
        val state = started(store, online = { false }).state.value
        assertEquals(listOf(SliceName.Cash, SliceName.Named("Technology")), state.allocation(AllocationBy.SECTOR)!!.rows.map { it.name })
        assertEquals(listOf(SliceName.Cash, SliceName.Named("US")), state.allocation(AllocationBy.GEOGRAPHY)!!.rows.map { it.name })
        assertEquals(
            listOf(AllocationBy.ASSET_TYPE, AllocationBy.HOLDING, AllocationBy.SECTOR, AllocationBy.GEOGRAPHY),
            state.portfolioAllocation.breakdowns!!.map { it.by },
        )
        assertEquals(listOf(SliceName.Named("Technology")), state.portfolioAllocation.breakdowns!![2].rows.map { it.name })
    }

    // 23b: before the source is asked the holding is "Unclassified"; the refresh at start gives its sector.
    @Test
    fun aRefreshThatGivesTheProfile_changesTheSectorBreakdown() {
        val offline = started(store(), online = { false }).state.value
        assertEquals(listOf(SliceName.Cash, SliceName.Unclassified), offline.allocation(AllocationBy.SECTOR)!!.rows.map { it.name })
        val source = FakeSource().apply { profiles = mapOf("AAPL" to ProfileAnswer.Given(SourceProfile("Technology", "US"))) }
        val state = started(store(), source = source).state.value
        assertEquals(listOf(SliceName.Cash, SliceName.Named("Technology")), state.allocation(AllocationBy.SECTOR)!!.rows.map { it.name })
    }
}
