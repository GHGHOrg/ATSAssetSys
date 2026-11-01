package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SellRefusal
import app.ghghorg.atsassets.core.SplitEffect
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.SplitRefusal
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
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
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SplitFlowTest {
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

    private fun sold(
        id: String,
        date: String,
        quantity: String,
        seq: Long,
        lotId: String,
        ticker: String = "AAPL",
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.SELL,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = 0L,
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

    // Two AAPL lots in Main (10 for $1,000.00 and 10 for $1,500.00), and one SPY share.
    private val lot1 = bought("b1", "2026-01-02", "10", 100_000L, 2L)
    private val lot2 = bought("b2", "2026-02-01", "10", 150_000L, 3L)
    private val spy = bought("b3", "2026-01-03", "1", 50_000L, 4L, ticker = "SPY")

    private fun store(
        transactions: List<Transaction> = listOf(lot1, lot2, spy),
        saveError: Exception? = null,
    ): FakeStore {
        // A split has no cash line (4.5).
        val lines = transactions.filter { it.type != TransactionType.SPLIT }.map { lineOf(it) }
        val store = FakeStore(stored = listOf(opening()) + lines, saveError = saveError)
        store.storedPortfolios = listOf(main, second)
        store.storedTransactions = transactions
        return store
    }

    private var nextId = 0

    private fun onHolding(
        store: FakeStore = store(),
        ticker: String = "AAPL",
    ): AppViewModel {
        val vm = started(store, newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        vm.openHolding(ticker)
        vm.splitFromHolding()
        return vm
    }

    private fun AppViewModel.ratio(
        to: String,
        from: String,
    ) {
        onSplitToChange(to)
        onSplitFromChange(from)
    }

    private fun shares(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    @Test
    fun split_fromTheHolding_opensWithItsPortfolioAndTicker_datedToday() {
        val state = onHolding().state.value
        assertEquals(Screen.SPLIT, state.screen)
        assertEquals(SplitForm(portfolioId = "p1", ticker = "AAPL", dateText = "20261026"), state.splitForm)
        assertEquals(listOf("AAPL", "SPY"), state.splitTickers)
        assertEquals(SplitRefusals(), state.splitRefusals)
    }

    @Test
    fun theChips_offerTheTickersWithABuy_andAnotherPortfolio_choosesItsFirst() {
        val vm = onHolding()
        vm.onSplitPortfolioChange("p2")
        assertEquals("p2", vm.state.value.splitForm.portfolioId)
        assertNull(vm.state.value.splitForm.ticker)
        assertEquals(emptyList<String>(), vm.state.value.splitTickers)
        vm.onSplitPortfolioChange("p1")
        assertEquals("AAPL", vm.state.value.splitForm.ticker)
        vm.onSplitTickerChange("SPY")
        assertEquals("SPY", vm.state.value.splitForm.ticker)
        vm.onSplitTickerChange("MSFT")
        vm.onSplitPortfolioChange("p9")
        assertEquals(SplitForm(portfolioId = "p1", ticker = "SPY", dateText = "20261026"), vm.state.value.splitForm)
    }

    @Test
    fun review_showsTheSharesBeforeAndAfter_andConfirm_savesTheSplit_withNoCashLine_andReturnsToTheHolding() {
        val store = store()
        val vm = onHolding(store)
        vm.ratio("2", "1")
        vm.reviewSplit()
        assertEquals(Screen.SPLIT_REVIEW, vm.state.value.screen)
        assertEquals(SplitEffect(shares(20), shares(40)), vm.state.value.splitEffect)
        val cashBefore = store.stored
        vm.confirmSplit()
        val state = vm.state.value
        assertEquals(Screen.HOLDING, state.screen)
        assertEquals("AAPL", state.holdingTicker)
        val split = store.savedSplits.single()
        assertEquals(TransactionType.SPLIT, split.type)
        assertEquals(SplitRatio(2, 1), split.split)
        assertEquals(LocalDate.of(2026, 10, 26), split.date)
        assertEquals(cashBefore, store.stored)
        assertEquals(shares(40), state.shownHolding?.shares)
        assertEquals(Fraction.ofCents(250_000L), state.shownHolding?.cost)
        assertEquals(SplitForm(), state.splitForm)
        assertNull(state.splitReview)
    }

    @Test
    fun theRatioFields_refuseEmpty_notWhole_zero_andAboveAMillion_together_andKeepTheText() {
        val vm = onHolding()
        vm.ratio("1.5", "")
        vm.reviewSplit()
        assertEquals(Screen.SPLIT, vm.state.value.screen)
        assertEquals(RatioError.NOT_WHOLE, vm.state.value.splitRefusals.to)
        assertEquals(RatioError.EMPTY, vm.state.value.splitRefusals.from)
        assertEquals("1.5", vm.state.value.splitForm.toText)
        vm.ratio("0", "1000001")
        vm.reviewSplit()
        assertEquals(RatioError.ZERO, vm.state.value.splitRefusals.to)
        assertEquals(RatioError.TOO_LARGE, vm.state.value.splitRefusals.from)
        assertNull(vm.state.value.splitRefusals.rule)
        vm.ratio("1000000", "0001")
        vm.reviewSplit()
        assertEquals(Screen.SPLIT_REVIEW, vm.state.value.screen)
    }

    @Test
    fun aRatioNumber_isDigitsOnly_from1To1000000() {
        assertEquals(RatioError.EMPTY, ratioError(""))
        assertEquals(RatioError.NOT_WHOLE, ratioError("-2"))
        assertEquals(RatioError.NOT_WHOLE, ratioError(" 2"))
        assertEquals(RatioError.NOT_WHOLE, ratioError("２"))
        assertEquals(RatioError.ZERO, ratioError("000"))
        assertEquals(RatioError.TOO_LARGE, ratioError("99999999999"))
        assertNull(ratioError("1000000"))
        assertEquals(7, ratioNumber("007"))
        assertNull(ratioNumber("1000001"))
    }

    @Test
    fun equalNumbers_areRefusedByTheRule_notAsAField() {
        val vm = onHolding()
        vm.ratio("3", "3")
        vm.reviewSplit()
        assertEquals(SplitRefusals(rule = SplitRefusal.RatioEqual), vm.state.value.splitRefusals)
    }

    @Test
    fun aDateBeforeTheOpeningBalance_showsBesideABadNumber() {
        val vm = onHolding()
        vm.onSplitDateChange("20251231")
        vm.ratio("x", "1")
        vm.reviewSplit()
        val refusals = vm.state.value.splitRefusals
        assertEquals(RatioError.NOT_WHOLE, refusals.to)
        assertEquals(SplitRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 1)), refusals.rule)
    }

    @Test
    fun theDate_takesDigitsOnly_andEightDigitsThatAreNoDate_areRefusedAtReview() {
        val vm = onHolding()
        vm.onSplitDateChange("2026-")
        assertEquals(DateRefusal.NOT_DIGITS, vm.state.value.splitRefusals.date)
        vm.onSplitDateChange("202610261")
        assertEquals("2026-", vm.state.value.splitForm.dateText)
        vm.onSplitDateChange("20261301")
        vm.ratio("2", "1")
        vm.reviewSplit()
        assertEquals(DateRefusal.NOT_A_DATE, vm.state.value.splitRefusals.date)
    }

    @Test
    fun aReverseSplit_thatLeavesALaterSellShort_isRefused_namingThatSellsDate_andARatioStandIn_hidesIt() {
        val sale = sold("s1", "2026-03-01", "10", 5L, lotId = "b1")
        val vm = onHolding(store(listOf(lot1, lot2, spy, sale)))
        vm.onSplitDateChange("20260115")
        vm.ratio("1", "10")
        vm.reviewSplit()
        assertEquals(SplitRefusal.SellShort(LocalDate.of(2026, 3, 1)), vm.state.value.splitRefusals.rule)
        vm.ratio("1", "y")
        vm.reviewSplit()
        assertEquals(SplitRefusals(from = RatioError.NOT_WHOLE), vm.state.value.splitRefusals)
    }

    @Test
    fun backToEdit_andBack_keepTheText_andReturnToTheHolding_aBottomTabLeavesTheSplit() {
        val vm = onHolding()
        vm.ratio("2", "1")
        vm.reviewSplit()
        vm.backToSplit()
        assertEquals(Screen.SPLIT, vm.state.value.screen)
        assertEquals("2", vm.state.value.splitForm.toText)
        vm.closeSplit()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
        assertEquals("AAPL", vm.state.value.holdingTicker)
        vm.splitFromHolding()
        assertEquals("1", vm.state.value.splitForm.fromText)
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    @Test
    fun aFailedSave_staysOnTheReview_andSavesNothing() {
        val store = store(saveError = IllegalStateException("disk"))
        val vm = onHolding(store)
        vm.ratio("2", "1")
        vm.reviewSplit()
        vm.confirmSplit()
        assertEquals(Screen.SPLIT_REVIEW, vm.state.value.screen)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.splitRefusals.save)
        assertEquals(emptyList<Transaction>(), store.savedSplits)
    }

    /**
     * AAPL bought in lots of [quantities] shares on 2026-01-02, then a saved split of [to] for [from] on 2026-03-01;
     * Sell opened from the holding (dated today, 2026-10-26) with Sell all pressed.
     */
    private fun sellAllAfterSplit(
        quantities: List<String>,
        to: Int,
        from: Int,
    ): Pair<FakeStore, AppViewModel> {
        val buys = quantities.mapIndexed { index, quantity -> bought("b$index", "2026-01-02", quantity, 10_000L, 2L + index) }
        val split =
            Transaction(
                id = "x1",
                portfolioId = "p1",
                ticker = "AAPL",
                type = TransactionType.SPLIT,
                date = LocalDate.of(2026, 3, 1),
                enteredSeq = 2L + quantities.size,
                quantity = BigDecimal.ZERO,
                totalCents = 0L,
                split = SplitRatio(to, from),
            )
        val store = store(buys + split)
        val vm = started(store, newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        vm.sellFromHolding()
        vm.sellAll()
        return store to vm
    }

    /** Sell all fills [shownSum], Review passes, and the saved sell leaves AAPL at exactly 0 with no lots listed. */
    private fun assertSellAllSavesAndEmptiesTheHolding(
        quantities: List<String>,
        to: Int,
        from: Int,
        shownSum: String,
    ) {
        val (store, vm) = sellAllAfterSplit(quantities, to, from)
        assertEquals(shownSum, vm.state.value.sellForm.quantityText)
        vm.onSellTotalChange("100.00")
        vm.reviewSell()
        assertEquals(SellRefusals(), vm.state.value.sellRefusals)
        assertEquals(Screen.SELL_REVIEW, vm.state.value.screen)
        vm.confirmSell()
        val state = vm.state.value
        assertEquals(Screen.HOLDING, state.screen)
        assertEquals(BigDecimal(shownSum), store.savedSells.single().first.quantity)
        assertEquals(Fraction.ZERO, state.shownHolding?.shares)
        assertEquals(emptyList<String>(), state.holdingLots.map { it.buyId })
    }

    // P1, phone check 6 (12): 2, 10 and 2 shares, then 1 for 3; two of the three remainders round up.
    @Test
    fun p1_sellAllAfterAOneForThree_onLotsOf2_10And2_savesAndEmptiesTheHolding() {
        assertSellAllSavesAndEmptiesTheHolding(listOf("2", "10", "2"), 1, 3, "4.66666667")
    }

    // P2: a single lot of 5/3, rounded up.
    @Test
    fun p2_sellAllOfASingleLotOf5Over3_savesAndEmptiesTheHolding() {
        assertSellAllSavesAndEmptiesTheHolding(listOf("5"), 1, 3, "1.66666667")
    }

    // P3: three lots of 2/3; the shown sum (2.00000001) is above the holding shown at 8 places (2).
    @Test
    fun p3_sellAllOfThreeLotsOf2Over3_sells2_00000001_andEmptiesTheHolding() {
        assertSellAllSavesAndEmptiesTheHolding(listOf("2", "2", "2"), 1, 3, "2.00000001")
    }

    // P4: four lots of 3/7 (0.42857142857...), each shown as 0.42857143; the holding of 12/7 shows 1.71428571.
    @Test
    fun p4_sellAllOfFourLotsOf3Over7_sells1_71428572_andEmptiesTheHolding() {
        assertSellAllSavesAndEmptiesTheHolding(listOf("3", "3", "3", "3"), 1, 7, "1.71428572")
    }

    // P5: two lots of 1/7 (0.14285714285...), each shown as 0.14285714; the holding of 2/7 shows 0.28571429.
    @Test
    fun p5_sellAllOfTwoLotsOf1Over7_sells0_28571428_andTheShownHoldingFigure_isRefusedAsMoreThanHeld() {
        assertSellAllSavesAndEmptiesTheHolding(listOf("1", "1"), 1, 7, "0.28571428")
        val (_, vm) = sellAllAfterSplit(listOf("1", "1"), 1, 7)
        vm.onSellQuantityChange("0.28571429")
        vm.onSellTotalChange("100.00")
        vm.reviewSell()
        assertEquals(Screen.SELL, vm.state.value.screen)
        assertEquals(
            SellRefusal.MoreThanHeld(LocalDate.of(2026, 10, 26), Fraction.of(BigDecimal("0.28571428")), BigDecimal("0.28571429")),
            vm.state.value.sellRefusals.rule,
        )
    }

    @Test
    fun aHoldingAtZeroShares_canBeSplit_andTheReviewShowsZeroToZero() {
        val sale = sold("s1", "2026-02-01", "1", 5L, lotId = "b3", ticker = "SPY")
        val store = store(listOf(lot1, lot2, spy, sale))
        val vm = onHolding(store, ticker = "SPY")
        assertEquals(Screen.SPLIT, vm.state.value.screen)
        assertEquals("SPY", vm.state.value.splitForm.ticker)
        vm.ratio("2", "1")
        vm.reviewSplit()
        assertEquals(SplitEffect(Fraction.ZERO, Fraction.ZERO), vm.state.value.splitEffect)
        vm.confirmSplit()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
        assertEquals(1, store.savedSplits.size)
    }
}
