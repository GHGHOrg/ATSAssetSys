package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
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
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HoldingDetailFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)
    private val second = Portfolio(id = "p2", name = "Second", createdSeq = 2L)

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

    private fun store(): FakeStore = FakeStore(stored = listOf(opening(), aaplLine)).also {
        it.storedPortfolios = listOf(main, second)
        it.storedTransactions = listOf(aaplBuy)
        it.storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true))
    }

    private var nextId = 0

    /** Started on Main's screen, with AAPL's detail open. */
    private fun onAapl(store: FakeStore = store()): AppViewModel {
        val vm = started(store, newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        return vm
    }

    @Test
    fun aHoldingRow_opensItsDetail_onTransactions_withItsTransactionsAndLots() {
        val state = onAapl().state.value
        assertEquals(Screen.HOLDING, state.screen)
        assertEquals("p1", state.portfolioId)
        assertEquals("AAPL", state.holdingTicker)
        assertEquals(HoldingTab.TRANSACTIONS, state.holdingTab)
        assertEquals("AAPL", state.shownHolding?.ticker)
        assertEquals(listOf("b1"), state.holdingTransactions.map { it.id })
        assertEquals(listOf("b1"), state.holdingLots.map { it.buyId })
    }

    @Test
    fun anUnknownTicker_orAnotherScreen_isIgnored() {
        val vm = started(store())
        vm.openPortfolio("p1")
        vm.openHolding("MSFT")
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
        val onTabs = started(store())
        onTabs.openHolding("AAPL")
        assertEquals(Screen.TABS, onTabs.state.value.screen)
    }

    @Test
    fun back_returnsToThePortfolio() {
        val vm = onAapl()
        vm.closeHolding()
        val state = vm.state.value
        assertEquals(Screen.PORTFOLIO, state.screen)
        assertEquals("p1", state.portfolioId)
        assertEquals(null, state.holdingTicker)
    }

    @Test
    fun theTab_canBeChanged_andStartsOnTransactionsEachTime() {
        val vm = onAapl()
        vm.selectHoldingTab(HoldingTab.LOTS)
        assertEquals(HoldingTab.LOTS, vm.state.value.holdingTab)
        vm.closeHolding()
        vm.openHolding("AAPL")
        assertEquals(HoldingTab.TRANSACTIONS, vm.state.value.holdingTab)
    }

    @Test
    fun aBottomTab_leavesTheHolding() {
        val vm = onAapl()
        vm.selectTab(Tab.CASH)
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.CASH, state.tab)
        assertEquals(null, state.holdingTicker)
        assertEquals(null, state.portfolioId)
    }

    @Test
    fun anUnlock_keepsTheHolding() {
        val vm = onAapl()
        vm.onUnlocked()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
    }

    @Test
    fun buyFromTheHolding_fillsInTheTickerAndPortfolio_andKeepsTheRest() {
        val vm = started(store())
        vm.chooseBuy()
        vm.onBuyQuantityChange("5")
        vm.onBuyPortfolioChange("p2")
        vm.closeBuy()
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        vm.buyFromHolding()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        assertEquals("AAPL", state.buyForm.tickerText)
        assertEquals("p1", state.buyForm.portfolioId)
        assertEquals("5", state.buyForm.quantityText)
        assertEquals("20261026", state.buyForm.dateText)
    }

    @Test
    fun buyFromTheHolding_isIgnoredElsewhere() {
        val vm = started(store())
        vm.openPortfolio("p1")
        vm.buyFromHolding()
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
    }

    @Test
    fun backFromThatBuy_returnsToTheHolding() {
        val vm = onAapl()
        vm.buyFromHolding()
        vm.closeBuy()
        val state = vm.state.value
        assertEquals(Screen.HOLDING, state.screen)
        assertEquals("AAPL", state.holdingTicker)
        assertEquals("p1", state.portfolioId)
    }

    @Test
    fun aSavedBuyFromTheHolding_returnsToIt_withTheNewTransactionAndLot() {
        val vm = onAapl()
        vm.buyFromHolding()
        vm.onBuyDateChange("20260301")
        vm.onBuyQuantityChange("2")
        vm.onBuyTotalChange("320.00")
        vm.reviewBuy()
        vm.confirmBuy()
        val state = vm.state.value
        assertEquals(Screen.HOLDING, state.screen)
        assertEquals("AAPL", state.holdingTicker)
        assertEquals(2, state.holdingTransactions.size)
        assertEquals(LocalDate.of(2026, 3, 1), state.holdingTransactions[0].date)
        assertEquals(listOf(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 3, 1)), state.holdingLots.map { it.buyDate })
    }

    @Test
    fun aBuyFromTheMenu_afterOneFromTheHolding_returnsToTheTab() {
        val vm = onAapl()
        vm.buyFromHolding()
        vm.selectTab(Tab.OVERALL)
        vm.chooseBuy()
        vm.closeBuy()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertFalse(vm.state.value.buyFromHolding)
    }
}
