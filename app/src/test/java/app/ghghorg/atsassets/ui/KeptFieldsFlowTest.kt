package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SplitRatio
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// 15c: through the view model, every kind of save, a new date, another portfolio or ticker, hide and unhide, and a new
// day leave the kept fields equal to a fresh calculation; typing leaves them the same objects.
@OptIn(ExperimentalCoroutinesApi::class)
class KeptFieldsFlowTest {
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

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    // AAPL: b1 sold by s1, b2 held, a 2 for 1 split on Apr 1 (x1). KO sold out. MSFT held, with a buy dated tomorrow (f1).
    private val b1 = bought("b1", "AAPL", "2026-01-02", "10", 100_000L, 2L)
    private val s1 = sold("s1", "AAPL", "2026-01-03", "10", 120_000L, 3L, "b1")
    private val b2 = bought("b2", "AAPL", "2026-02-01", "10", 150_000L, 4L)
    private val x1 =
        Transaction(
            id = "x1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SPLIT,
            date = LocalDate.of(2026, 4, 1),
            enteredSeq = 5L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            split = SplitRatio(2, 1),
        )
    private val k1 = bought("k1", "KO", "2026-01-04", "5", 5_000L, 6L)
    private val k2 = sold("k2", "KO", "2026-02-10", "5", 5_000L, 7L, "k1")
    private val m1 = bought("m1", "MSFT", "2026-01-05", "2", 2_000L, 8L)
    private val f1 = bought("f1", "MSFT", "2026-10-27", "1", 1_000L, 9L)

    private fun store(): FakeStore {
        val transactions = listOf(b1, s1, b2, x1, k1, k2, m1, f1)
        val lines = transactions.filter { it.type != TransactionType.SPLIT }.map { lineOf(it) }
        val store = FakeStore(stored = listOf(opening()) + lines)
        store.storedPortfolios = listOf(main, second)
        store.storedTransactions = transactions
        store.storedInstruments = listOf("AAPL", "KO", "MSFT").map { Instrument(it, AssetType.STOCK, verified = true) }
        return store
    }

    // A clock that the test moves on: 2026-10-26 in New York until [now] is changed.
    private var now: Instant = Instant.parse("2026-10-26T12:00:00Z")

    private class MovingClock(
        private val current: () -> Instant,
        private val zone: ZoneId = ZoneOffset.UTC,
    ) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MovingClock(current, zone)

        override fun instant(): Instant = current()
    }

    private var nextId = 0

    private fun onHolding(
        ticker: String,
        store: FakeStore = store(),
    ): AppViewModel {
        val vm = started(store, clock = MovingClock({ now }), newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        vm.openHolding(ticker)
        return vm
    }

    private val AppViewModel.shown: AppState get() = state.value

    // --- Each kind of save ---

    @Test
    fun aCashEntrySave_refreshesTheNetWorth() {
        val vm = started(store(), clock = MovingClock({ now }), newId = { "id${++nextId}" })
        val before = vm.shown
        vm.openForm()
        vm.onDateChange("2026-03-01")
        vm.onAmountChange("500.00")
        vm.review()
        vm.confirm()
        assertEquals(before.entries.size + 1, vm.shown.entries.size)
        assertNotEquals(before.netWorth, vm.shown.netWorth)
        assertKeptFieldsFresh(vm.shown, "cash entry")
    }

    @Test
    fun aBuySave_fromTheHolding_refreshesItsLots() {
        val vm = onHolding("AAPL")
        vm.buyFromHolding()
        vm.onBuyDateChange("20260301")
        vm.onBuyQuantityChange("5")
        vm.onBuyTotalChange("500.00")
        vm.reviewBuy()
        vm.confirmBuy()
        assertEquals(Screen.HOLDING, vm.shown.screen)
        assertEquals(2, vm.shown.holdingLots.size)
        assertKeptFieldsFresh(vm.shown, "buy")
    }

    @Test
    fun aSellSave_fromTheHolding_emptiesItsLots_andOffersHide() {
        val vm = onHolding("AAPL")
        assertFalse(vm.shown.hideOffered)
        vm.sellFromHolding()
        vm.toggleLot("b2")
        vm.onSellQuantityChange("20")
        vm.onSellTotalChange("3000.00")
        vm.reviewSell()
        vm.confirmSell()
        assertEquals(Screen.HOLDING, vm.shown.screen)
        assertEquals(emptyList<String>(), vm.shown.holdingLots.map { it.buyId })
        assertTrue(vm.shown.hideOffered)
        assertKeptFieldsFresh(vm.shown, "sell")
    }

    @Test
    fun aSplitSave_fromTheHolding_refreshesItsLots() {
        val vm = onHolding("AAPL")
        val before = vm.shown.holdingLots
        vm.splitFromHolding()
        vm.onSplitToChange("3")
        vm.onSplitFromChange("1")
        vm.reviewSplit()
        vm.confirmSplit()
        assertEquals(Screen.HOLDING, vm.shown.screen)
        assertNotEquals(before, vm.shown.holdingLots)
        assertKeptFieldsFresh(vm.shown, "split")
    }

    @Test
    fun aReversalSave_refreshesTheShownTransactionsLot() {
        val vm = onHolding("AAPL")
        vm.openTransaction("b2")
        val before = vm.shown.transactionLot
        vm.openTransactionReversal()
        vm.onTransactionReversalDateChange("20260501")
        vm.confirmTransactionReversal()
        assertEquals(Screen.TRANSACTION, vm.shown.screen)
        assertNotEquals(before, vm.shown.transactionLot)
        assertKeptFieldsFresh(vm.shown, "reversal")
    }

    @Test
    fun reverseAndReenter_keepsTheSellListsFresh_whileReplacing_andAfterTheSave() {
        val vm = onHolding("AAPL")
        vm.openTransaction("s1")
        vm.openReenter()
        assertEquals(Screen.SELL, vm.shown.screen)
        assertKeptFieldsFresh(vm.shown, "replacing")
        vm.onSellTotalChange("1300.00")
        vm.reviewSell()
        assertEquals(Screen.REPLACE_REVIEW, vm.shown.screen)
        vm.changeReplaceDate()
        val before = vm.shown.sellLotChoices
        vm.onReplaceDateChange("20260104")
        assertNotSame(before, vm.shown.sellLotChoices)
        assertKeptFieldsFresh(vm.shown, "reversal date typed")
        vm.onReplaceDateChange("20260103")
        vm.confirmReplace()
        assertEquals(1, vm.shown.transactions.count { it.reversesId == "s1" })
        assertKeptFieldsFresh(vm.shown, "pair saved")
    }

    // --- The sell form's inputs ---

    @Test
    fun aNewSellDate_anotherTicker_andAnotherPortfolio_eachGiveAFreshNewLotList() {
        val vm = onHolding("AAPL")
        vm.sellFromHolding()
        var before = vm.shown.sellLotChoices
        vm.onSellDateChange("20260115")
        assertNotSame(before, vm.shown.sellLotChoices)
        assertKeptFieldsFresh(vm.shown, "date")
        before = vm.shown.sellLotChoices
        vm.onSellTickerChange("MSFT")
        assertNotSame(before, vm.shown.sellLotChoices)
        assertKeptFieldsFresh(vm.shown, "ticker")
        before = vm.shown.sellLotChoices
        vm.onSellPortfolioChange("p2")
        assertNotSame(before, vm.shown.sellLotChoices)
        assertEquals(emptyList<String>(), vm.shown.sellTickers)
        assertKeptFieldsFresh(vm.shown, "portfolio")
    }

    @Test
    fun aDateThatDropsAPick_judgesItAgainstTheNewDatesLotList() {
        val vm = onHolding("AAPL")
        vm.sellFromHolding()
        vm.toggleLot("b2")
        vm.onSellDateChange("20260115")
        assertEquals(emptyMap<String, String>(), vm.shown.sellForm.picks)
        assertTrue(vm.shown.sellPicksDropped)
        assertKeptFieldsFresh(vm.shown, "dropped")
    }

    @Test
    fun typingTheQuantityTotalOrAPick_leavesTheLotListTheSameObject() {
        val vm = onHolding("AAPL")
        vm.sellFromHolding()
        val lots = vm.shown.sellLotChoices
        val kept = vm.shown.keptFrom
        vm.onSellQuantityChange("5")
        assertSame(lots, vm.shown.sellLotChoices)
        vm.onSellTotalChange("750.00")
        assertSame(lots, vm.shown.sellLotChoices)
        vm.toggleLot("b2")
        assertSame(lots, vm.shown.sellLotChoices)
        vm.onPickChange("b2", "5")
        assertSame(lots, vm.shown.sellLotChoices)
        vm.sellAll()
        assertSame(lots, vm.shown.sellLotChoices)
        assertSame(kept, vm.shown.keptFrom)
    }

    // --- Holding detail ---

    @Test
    fun anotherTab_leavesTheHoldingLotsAndHideOfferedUnworked_andAnotherHoldingWorksThemOut() {
        val vm = onHolding("AAPL")
        val lots = vm.shown.holdingLots
        val kept = vm.shown.keptFrom
        vm.selectHoldingTab(HoldingTab.LOTS)
        assertSame(lots, vm.shown.holdingLots)
        assertSame(kept, vm.shown.keptFrom)
        vm.closeHolding()
        vm.openHolding("MSFT")
        assertNotSame(lots, vm.shown.holdingLots)
        assertKeptFieldsFresh(vm.shown, "another holding")
    }

    @Test
    fun hideAndUnhide_keepHideOfferedFresh() {
        val vm = onHolding("KO")
        assertTrue(vm.shown.hideOffered)
        vm.hide()
        assertFalse(vm.shown.hideOffered)
        assertKeptFieldsFresh(vm.shown, "hidden")
        vm.closeHolding()
        vm.openHiddenHoldings()
        vm.unhide("KO")
        assertKeptFieldsFresh(vm.shown, "unhidden")
        vm.closeHiddenHoldings()
        vm.openHolding("KO")
        assertTrue(vm.shown.hideOffered)
        assertKeptFieldsFresh(vm.shown, "shown again")
    }

    // --- A new day ---

    @Test
    fun aSaveOnANewDay_bringsInTheBuyDatedThatDay() {
        val vm = onHolding("MSFT")
        assertEquals(listOf("m1"), vm.shown.holdingLots.map { it.buyId })
        now = Instant.parse("2026-10-27T12:00:00Z")
        vm.buyFromHolding()
        vm.onBuyDateChange("20260301")
        vm.onBuyQuantityChange("1")
        vm.onBuyTotalChange("100.00")
        vm.reviewBuy()
        vm.confirmBuy()
        assertEquals(LocalDate.of(2026, 10, 27), vm.shown.today)
        assertTrue("f1" in vm.shown.holdingLots.map { it.buyId })
        assertKeptFieldsFresh(vm.shown, "new day")
    }
}
