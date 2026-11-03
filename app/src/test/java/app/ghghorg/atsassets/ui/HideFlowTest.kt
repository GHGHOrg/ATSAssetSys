package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// Iteration 15: Hide on Holding detail, Hidden holdings per portfolio, Unhide, and unhiding by a save (4.4, view 9).
@OptIn(ExperimentalCoroutinesApi::class)
class HideFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)

    private fun bought(
        id: String,
        ticker: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.BUY,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
    )

    private fun sold(
        id: String,
        ticker: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        lotId: String,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.SELL,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        allocations = listOf(SellAllocation(lotId, Fraction.of(BigDecimal(quantity)))),
    )

    // Criterion 60(a)'s AAPL, sold out; KO sold out; MSFT still held.
    private val b1 = bought("b1", "AAPL", "2026-01-02", "10", 10_000L, 2L)
    private val s1 = sold("s1", "AAPL", "2026-01-03", "10", 12_000L, 3L, "b1")
    private val k1 = bought("k1", "KO", "2026-01-04", "5", 5_000L, 4L)
    private val k2 = sold("k2", "KO", "2026-02-10", "5", 5_000L, 5L, "k1")
    private val m1 = bought("m1", "MSFT", "2026-01-05", "2", 2_000L, 6L)
    private val aapl = HiddenHolding("p1", "AAPL")
    private val ko = HiddenHolding("p1", "KO")

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    private fun store(
        hidden: List<HiddenHolding> = emptyList(),
        saveError: Exception? = null,
    ): FakeStore {
        val transactions = listOf(b1, s1, k1, k2, m1)
        val store = FakeStore(stored = listOf(opening()) + transactions.map { lineOf(it) }, saveError = saveError)
        store.storedPortfolios = listOf(main)
        store.storedTransactions = transactions
        store.storedInstruments = listOf("AAPL", "KO", "MSFT").map { Instrument(it, AssetType.STOCK, verified = true) }
        store.storedHidden = hidden
        return store
    }

    private var nextId = 0

    private fun onPortfolio(store: FakeStore): AppViewModel {
        val vm = started(store, newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        return vm
    }

    @Test
    fun hide_isOfferedOnAHoldingAtZeroShares_andNotOnOneWithShares() {
        val vm = onPortfolio(store())
        vm.openHolding("AAPL")
        assertTrue(vm.state.value.hideOffered)
        vm.closeHolding()
        vm.openHolding("MSFT")
        assertFalse(vm.state.value.hideOffered)
    }

    @Test
    fun hide_takesTheHoldingOutOfTheListAndTheCount_andSavesIt_withNoCashEffect() {
        val store = store()
        val vm = onPortfolio(store)
        val cashBefore = vm.state.value.entries
        assertEquals(listOf("AAPL", "KO", "MSFT"), vm.state.value.holdingsOf("p1").map { it.ticker })
        vm.openHolding("AAPL")
        vm.hide()
        val state = vm.state.value
        assertEquals(Screen.HOLDING, state.screen)
        assertEquals(listOf(aapl), store.storedHidden)
        assertEquals(listOf(aapl), state.hidden)
        assertEquals(listOf("KO", "MSFT"), state.holdingsOf("p1").map { it.ticker })
        assertFalse(state.hideOffered)
        assertEquals(cashBefore, state.entries)
    }

    @Test
    fun hiddenHoldings_isPerPortfolio_inTickerOrder_withTheLastTransactionDate() {
        val vm = onPortfolio(store(hidden = listOf(ko, aapl)))
        assertEquals(listOf("MSFT"), vm.state.value.holdingsOf("p1").map { it.ticker })
        vm.openHiddenHoldings()
        val state = vm.state.value
        assertEquals(Screen.HIDDEN_HOLDINGS, state.screen)
        assertEquals(
            listOf(HiddenRow("AAPL", LocalDate.of(2026, 1, 3)), HiddenRow("KO", LocalDate.of(2026, 2, 10))),
            state.hiddenRows("p1"),
        )
        vm.closeHiddenHoldings()
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
    }

    @Test
    fun unhide_returnsTheHoldingToTheListAtZeroShares() {
        val store = store(hidden = listOf(aapl))
        val vm = onPortfolio(store)
        vm.openHiddenHoldings()
        vm.unhide("AAPL")
        val state = vm.state.value
        assertEquals(Screen.HIDDEN_HOLDINGS, state.screen)
        assertEquals(emptyList<HiddenRow>(), state.hiddenRows("p1"))
        assertEquals(emptyList<HiddenHolding>(), store.storedHidden)
        assertEquals(listOf("AAPL", "KO", "MSFT"), state.holdingsOf("p1").map { it.ticker })
    }

    // Decision 3: Back from a holding opened from Hidden holdings returns there; no Hide while hidden.
    @Test
    fun aHoldingOpenedFromHiddenHoldings_offersNoHide_andBackReturnsToHiddenHoldings() {
        val vm = onPortfolio(store(hidden = listOf(aapl)))
        vm.openHiddenHoldings()
        vm.openHiddenHolding("AAPL")
        val state = vm.state.value
        assertEquals(Screen.HOLDING, state.screen)
        assertEquals("AAPL", state.holdingTicker)
        assertTrue(state.shownHoldingHidden)
        assertFalse(state.hideOffered)
        vm.closeHolding()
        assertEquals(Screen.HIDDEN_HOLDINGS, vm.state.value.screen)
        vm.closeHiddenHoldings()
        vm.openHolding("MSFT")
        vm.closeHolding()
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
    }

    @Test
    fun aNewBuyOfAHiddenTicker_unhidesIt_inTheSameSave() {
        val store = store(hidden = listOf(aapl))
        val vm = onPortfolio(store)
        vm.openHiddenHoldings()
        vm.openHiddenHolding("AAPL")
        vm.buyFromHolding()
        vm.onBuyDateChange("20260301")
        vm.onBuyQuantityChange("1")
        vm.onBuyTotalChange("10.00")
        vm.reviewBuy()
        vm.confirmBuy()
        assertEquals(1, store.savedBuys.size)
        assertEquals(emptyList<HiddenHolding>(), store.storedHidden)
        assertEquals(emptyList<HiddenHolding>(), vm.state.value.hidden)
    }

    // Criterion 60(a): the reversal of the sell that sold it out gives it 10 shares and unhides it.
    @Test
    fun criterion60a_theSellsReversal_unhidesTheHolding_inTheSameSave() {
        val store = store(hidden = listOf(aapl))
        val vm = onPortfolio(store)
        vm.openHiddenHoldings()
        vm.openHiddenHolding("AAPL")
        vm.openTransaction("s1")
        vm.openTransactionReversal()
        vm.confirmTransactionReversal()
        assertEquals(1, store.savedReversals.size)
        assertEquals(emptyList<HiddenHolding>(), store.storedHidden)
        assertEquals(emptyList<HiddenHolding>(), vm.state.value.hidden)
        assertEquals(listOf("AAPL", "KO", "MSFT"), vm.state.value.holdingsOf("p1").map { it.ticker })
    }

    @Test
    fun aFailedHide_leavesTheHoldingAsItWas_andSaysSo() {
        val store = store(saveError = IllegalStateException("disk"))
        val vm = onPortfolio(store)
        vm.openHolding("AAPL")
        vm.hide()
        val state = vm.state.value
        assertEquals(emptyList<HiddenHolding>(), state.hidden)
        assertTrue(state.hideOffered)
        assertEquals(SaveRefusal.SAVE_FAILED, state.hideRefusal)
        assertNull(store.storedHidden.firstOrNull())
    }
}
