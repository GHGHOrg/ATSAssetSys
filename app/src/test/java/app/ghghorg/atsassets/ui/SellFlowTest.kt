package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.LotUnavailable
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.QuantityError
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SellRefusal
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
import java.math.BigInteger
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SellFlowTest {
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
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        ticker: String = "AAPL",
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

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        -transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    // Example A's two AAPL lots in Main, and one SPY share.
    private val lot1 = bought("b1", "2026-01-02", "10", 100_000L, 2L)
    private val lot2 = bought("b2", "2026-02-01", "10", 150_000L, 3L)
    private val spy = bought("b3", "2026-01-03", "1", 50_000L, 4L, ticker = "SPY")

    private fun store(transactions: List<Transaction> = listOf(lot1, lot2, spy)): FakeStore {
        val store = FakeStore(stored = listOf(opening()) + transactions.map { lineOf(it) })
        store.storedPortfolios = listOf(main, second)
        store.storedTransactions = transactions
        return store
    }

    private var nextId = 0

    private fun onSell(store: FakeStore = store()): AppViewModel {
        val vm = started(store, newId = { "id${++nextId}" })
        vm.openBuySellMenu()
        vm.chooseSell()
        return vm
    }

    /** Example A's sell of 12: picks lot 1 x 10 and lot 2 x 2, total $2,100.00, on 2026-03-01. */
    private fun AppViewModel.fillExampleA() {
        onSellDateChange("20260301")
        onSellQuantityChange("12")
        onSellTotalChange("2100.00")
        toggleLot("b1")
        toggleLot("b2")
        onPickChange("b2", "2")
    }

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    @Test
    fun sell_withNothingHeld_saysSo() {
        val vm = onSell(store(transactions = emptyList()))
        assertEquals(BuyNotice.NOTHING_TO_SELL, vm.state.value.buyNotice)
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    @Test
    fun sell_opensOnTheFirstPortfolio_andItsFirstHeldTicker_datedToday_withItsLotsOldestFirst() {
        val state = onSell().state.value
        assertEquals(Screen.SELL, state.screen)
        assertFalse(state.buySellMenuOpen)
        assertEquals("p1", state.sellForm.portfolioId)
        assertEquals(listOf("AAPL", "SPY"), state.sellTickers)
        assertEquals("AAPL", state.sellForm.ticker)
        assertEquals("20261026", state.sellForm.dateText)
        assertEquals(emptyMap<String, String>(), state.sellForm.picks)
        assertEquals(listOf("b1", "b2"), state.sellLotChoices.map { it.buyId })
    }

    @Test
    fun theTickerChips_offerOnlyTickersHeldInThePortfolio() {
        val vm = onSell()
        vm.onSellTickerChange("SPY")
        assertEquals("SPY", vm.state.value.sellForm.ticker)
        assertEquals(listOf("b3"), vm.state.value.sellLotChoices.map { it.buyId })
        vm.onSellTickerChange("MSFT")
        assertEquals("SPY", vm.state.value.sellForm.ticker)
        vm.onSellPortfolioChange("p2")
        assertEquals("p2", vm.state.value.sellForm.portfolioId)
        assertEquals(emptyList<String>(), vm.state.value.sellTickers)
        assertEquals(null, vm.state.value.sellForm.ticker)
        assertEquals(emptyList<String>(), vm.state.value.sellLotChoices.map { it.buyId })
    }

    @Test
    fun aTap_picksALotWithItsFullRemainder_aSecondTapUnpicksIt() {
        val vm = onSell()
        vm.toggleLot("b1")
        assertEquals(mapOf("b1" to "10"), vm.state.value.sellForm.picks)
        assertEquals(BigDecimal("10"), vm.state.value.sellPicked)
        vm.toggleLot("b1")
        assertEquals(emptyMap<String, String>(), vm.state.value.sellForm.picks)
    }

    @Test
    fun aGreyedOutLot_cannotBePicked() {
        val vm = onSell()
        vm.onSellDateChange("20260115")
        assertEquals(LotUnavailable.BOUGHT_AFTER_SELL_DATE, vm.state.value.sellLotChoices[1].unavailable)
        vm.toggleLot("b2")
        assertEquals(emptyMap<String, String>(), vm.state.value.sellForm.picks)
    }

    @Test
    fun aPartialPick_isTyped_andThePickedLineCountsIt() {
        val vm = onSell()
        vm.toggleLot("b1")
        vm.toggleLot("b2")
        vm.onPickChange("b2", "2")
        assertEquals(mapOf("b1" to "10", "b2" to "2"), vm.state.value.sellForm.picks)
        assertEquals(BigDecimal("12"), vm.state.value.sellPicked)
        vm.onPickChange("b3", "1")
        assertFalse(vm.state.value.sellForm.picks.containsKey("b3"))
    }

    @Test
    fun sellAll_picksEveryLotThatPasses_andFillsTheQuantity() {
        val vm = onSell()
        vm.onSellDateChange("20260115")
        vm.sellAll()
        assertEquals(mapOf("b1" to "10"), vm.state.value.sellForm.picks)
        assertEquals("10", vm.state.value.sellForm.quantityText)
        vm.onSellDateChange("20260301")
        vm.sellAll()
        assertEquals(mapOf("b1" to "10", "b2" to "10"), vm.state.value.sellForm.picks)
        assertEquals("20", vm.state.value.sellForm.quantityText)
    }

    @Test
    fun aDateChange_dropsPicksThatNoLongerPass_withALine() {
        val vm = onSell()
        vm.toggleLot("b1")
        vm.toggleLot("b2")
        assertFalse(vm.state.value.sellPicksDropped)
        vm.onSellDateChange("2026011")
        assertEquals(setOf("b1", "b2"), vm.state.value.sellForm.picks.keys)
        vm.onSellDateChange("20260115")
        assertEquals(setOf("b1"), vm.state.value.sellForm.picks.keys)
        assertTrue(vm.state.value.sellPicksDropped)
        vm.toggleLot("b1")
        assertFalse(vm.state.value.sellPicksDropped)
    }

    @Test
    fun aTickerOrPortfolioChange_clearsThePicks() {
        val vm = onSell()
        vm.toggleLot("b1")
        vm.onSellTickerChange("SPY")
        assertEquals(emptyMap<String, String>(), vm.state.value.sellForm.picks)
        vm.onSellTickerChange("AAPL")
        vm.toggleLot("b1")
        vm.onSellPortfolioChange("p2")
        assertEquals(emptyMap<String, String>(), vm.state.value.sellForm.picks)
    }

    @Test
    fun review_exampleA_showsTheLotsUsedTheirCostAndTheRealizedGain_andSavesNothing() {
        val store = store()
        val vm = onSell(store)
        vm.fillExampleA()
        assertEquals(BigDecimal("12"), vm.state.value.sellPicked)
        assertEquals(dollars(175), vm.state.value.sellPricePerShare)
        vm.reviewSell()
        val state = vm.state.value
        assertEquals(Screen.SELL_REVIEW, state.screen)
        assertEquals(SellRefusals(), state.sellRefusals)
        assertEquals(listOf(dollars(1_000), dollars(300)), state.sellPieces.map { it.cost })
        assertEquals(dollars(800), state.sellPieces.fold(Fraction.ZERO) { sum, piece -> sum + piece.gain })
        assertEquals(emptyList<Pair<Transaction, CashEntry>>(), store.savedSells)
    }

    @Test
    fun review_showsEveryRefusalTogether_andKeepsTheText() {
        val vm = onSell()
        vm.onSellDateChange("20251231")
        vm.onSellQuantityChange("0")
        vm.onSellTotalChange("12.345")
        vm.reviewSell()
        val state = vm.state.value
        assertEquals(Screen.SELL, state.screen)
        assertEquals(QuantityError.ZERO, state.sellRefusals.quantity)
        assertEquals(AmountError.TOO_MANY_DECIMALS, state.sellRefusals.total)
        assertEquals(null, state.sellRefusals.rule)
        assertEquals("20251231", state.sellForm.dateText)
        assertEquals("0", state.sellForm.quantityText)
        vm.onSellTotalChange("100")
        vm.reviewSell()
        assertEquals(QuantityError.ZERO, vm.state.value.sellRefusals.quantity)
        assertEquals(SellRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 1)), vm.state.value.sellRefusals.rule)
    }

    @Test
    fun review_picksThatDoNotAddUp_nameBothNumbers() {
        val vm = onSell()
        vm.onSellDateChange("20260301")
        vm.onSellQuantityChange("12")
        vm.onSellTotalChange("2100")
        vm.toggleLot("b1")
        vm.reviewSell()
        assertEquals(Screen.SELL, vm.state.value.screen)
        assertEquals(SellRefusal.PicksDoNotAddUp(BigDecimal("10"), BigDecimal("12")), vm.state.value.sellRefusals.rule)
    }

    @Test
    fun review_aBadPick_isRefusedOnItsLot_withoutAMismatchMessage() {
        val vm = onSell()
        vm.onSellDateChange("20260301")
        vm.onSellQuantityChange("12")
        vm.onSellTotalChange("2100")
        vm.toggleLot("b1")
        vm.toggleLot("b2")
        vm.onPickChange("b2", "2.123456789")
        vm.reviewSell()
        val refusals = vm.state.value.sellRefusals
        assertEquals(mapOf("b2" to QuantityError.TOO_MANY_DECIMALS), refusals.picks)
        assertEquals(null, refusals.rule)
    }

    @Test
    fun review_moreThanHeld_andAPickOverItsLot_areRefusedByTheRules() {
        val vm = onSell()
        vm.onSellDateChange("20260301")
        vm.onSellQuantityChange("21")
        vm.onSellTotalChange("2100")
        vm.reviewSell()
        assertEquals(
            SellRefusal.MoreThanHeld(LocalDate.of(2026, 3, 1), dollars(20), BigDecimal("21")),
            vm.state.value.sellRefusals.rule,
        )
        vm.onSellQuantityChange("11")
        vm.toggleLot("b1")
        vm.onPickChange("b1", "11")
        vm.reviewSell()
        assertEquals(SellRefusal.PickOverRemainder("b1", dollars(10)), vm.state.value.sellRefusals.rule)
    }

    @Test
    fun aTotalOfZero_isAllowed() {
        val vm = onSell()
        vm.onSellTickerChange("SPY")
        vm.sellAll()
        vm.onSellTotalChange("0")
        assertEquals(Fraction.ZERO, vm.state.value.sellPricePerShare)
        vm.reviewSell()
        assertEquals(Screen.SELL_REVIEW, vm.state.value.screen)
        assertEquals(dollars(-500), vm.state.value.sellPieces.single().gain)
    }

    @Test
    fun confirm_savesTheSellWithItsCashLineAndAllocations_andReturnsToTheTab() {
        val store = store()
        val vm = onSell(store)
        vm.fillExampleA()
        vm.reviewSell()
        vm.confirmSell()
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        val (sell, line) = store.savedSells.single()
        assertEquals(TransactionType.SELL, sell.type)
        assertEquals(listOf(SellAllocation("b1", dollars(10)), SellAllocation("b2", dollars(2))), sell.allocations)
        assertEquals(210_000L, line.amountCents)
        assertEquals(sell.id, line.transactionId)
        assertEquals(SellForm(), state.sellForm)
        val aapl = state.holdingsOf("p1").first { it.ticker == "AAPL" }
        assertEquals(dollars(8), aapl.shares)
        assertEquals(dollars(1_200), aapl.cost)
        assertTrue(state.entries.contains(line))
    }

    @Test
    fun aFailedSave_staysOnTheReview_andSavesNothing() {
        val store = FakeStore(stored = store().stored, saveError = IllegalStateException("disk")).also {
            it.storedPortfolios = listOf(main, second)
            it.storedTransactions = listOf(lot1, lot2, spy)
        }
        val vm = onSell(store)
        vm.fillExampleA()
        vm.reviewSell()
        vm.confirmSell()
        assertEquals(Screen.SELL_REVIEW, vm.state.value.screen)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.sellRefusals.save)
        assertEquals(emptyList<Pair<Transaction, CashEntry>>(), store.savedSells)
    }

    @Test
    fun backToEdit_andBack_keepTheText_aBottomTabLeavesTheSell() {
        val vm = onSell()
        vm.fillExampleA()
        vm.reviewSell()
        vm.backToSell()
        assertEquals(Screen.SELL, vm.state.value.screen)
        assertEquals("12", vm.state.value.sellForm.quantityText)
        vm.closeSell()
        assertEquals(Screen.TABS, vm.state.value.screen)
        vm.openBuySellMenu()
        vm.chooseSell()
        assertEquals(mapOf("b1" to "10", "b2" to "2"), vm.state.value.sellForm.picks)
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    @Test
    fun sellFromTheHolding_choosesItsPortfolioAndTicker_andBackReturnsToIt() {
        val vm = started(store(), newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        vm.openHolding("SPY")
        vm.sellFromHolding()
        val state = vm.state.value
        assertEquals(Screen.SELL, state.screen)
        assertEquals("p1", state.sellForm.portfolioId)
        assertEquals("SPY", state.sellForm.ticker)
        vm.closeSell()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
        assertEquals("SPY", vm.state.value.holdingTicker)
    }

    @Test
    fun aSavedSellFromTheHolding_returnsToIt_andASoldOutHoldingStays_atZeroShares() {
        val vm = started(store(), newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        vm.openHolding("SPY")
        vm.sellFromHolding()
        vm.sellAll()
        vm.onSellTotalChange("0")
        vm.reviewSell()
        vm.confirmSell()
        val state = vm.state.value
        assertEquals(Screen.HOLDING, state.screen)
        assertEquals("SPY", state.holdingTicker)
        assertEquals(Fraction.ZERO, state.shownHolding?.shares)
        assertEquals(emptyList<String>(), state.holdingLots.map { it.buyId })
        assertEquals(2, state.holdingsOf("p1").size)
        vm.sellFromHolding()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
        assertEquals(BuyNotice.NOTHING_TO_SELL_IN_HOLDING, vm.state.value.buyNotice)
        vm.dismissBuyNotice()
        assertEquals(null, vm.state.value.buyNotice)
    }

    @Test
    fun whenEveryHoldingIsSoldOut_sellSaysNothingToSell() {
        val vm = onSell(store(transactions = listOf(spy)))
        vm.sellAll()
        vm.onSellTotalChange("10")
        vm.reviewSell()
        vm.confirmSell()
        vm.openBuySellMenu()
        vm.chooseSell()
        assertEquals(BuyNotice.NOTHING_TO_SELL, vm.state.value.buyNotice)
        assertEquals(1, vm.state.value.holdingsOf("p1").size)
    }
}
