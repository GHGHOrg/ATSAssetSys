package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.BuyRefusal
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.QuantityError
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.prices.CheckRefusal
import kotlinx.coroutines.CompletableDeferred
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
class BuyFlowTest {
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
    private val aaplListing = Listing(type = "Common Stock", exchange = "XNAS", currency = "USD")
    private val aaplVerified = Instrument("AAPL", AssetType.STOCK, verified = true)

    private fun storeWithCash(
        portfolios: List<Portfolio> = listOf(main),
        cents: Long = 1_000_000L,
    ): FakeStore = FakeStore(stored = listOf(opening(cents = cents))).also { it.storedPortfolios = portfolios }

    private var nextId = 0

    private fun ids(): () -> String = { "id${++nextId}" }

    private fun boughtBefore(): Pair<Transaction, CashEntry> {
        val transaction =
            Transaction(
                id = "b1",
                portfolioId = "p1",
                ticker = "AAPL",
                type = TransactionType.BUY,
                date = LocalDate.of(2026, 1, 2),
                enteredSeq = 2L,
                quantity = BigDecimal("10"),
                totalCents = 100_000L,
            )
        val line = CashEntry("c-b1", LocalDate.of(2026, 1, 2), -100_000L, CashType.TRADE, "", 2L, transactionId = "b1")
        return transaction to line
    }

    private fun AppViewModel.fillBuy(
        ticker: String = "AAPL",
        date: String = "20260201",
        quantity: String = "10",
        total: String = "1500.00",
    ) {
        onBuyTickerChange(ticker)
        onBuyDateChange(date)
        onBuyQuantityChange(quantity)
        onBuyTotalChange(total)
    }

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    @Test
    fun theBuySellMenu_opensOnTheTabs_andCloses() {
        val vm = started(storeWithCash())
        vm.openBuySellMenu()
        assertTrue(vm.state.value.buySellMenuOpen)
        vm.closeBuySellMenu()
        assertFalse(vm.state.value.buySellMenuOpen)
    }

    @Test
    fun theBuySellMenu_isIgnoredOffTheTabs() {
        val vm = started(storeWithCash())
        vm.openForm()
        vm.openBuySellMenu()
        assertFalse(vm.state.value.buySellMenuOpen)
    }

    @Test
    fun buy_beforeTheOpeningBalance_asksForIt() {
        val vm = started(FakeStore())
        vm.openBuySellMenu()
        vm.chooseBuy()
        val state = vm.state.value
        assertEquals(BuyNotice.NEEDS_OPENING_BALANCE, state.buyNotice)
        assertFalse(state.buySellMenuOpen)
        assertEquals(Screen.TABS, state.screen)
        vm.openForm()
        assertEquals(null, vm.state.value.buyNotice)
        assertEquals(CashType.OPENING_BALANCE, vm.state.value.form.type)
    }

    @Test
    fun buy_withNoPortfolio_asksForOne_andReturnsToBuyAfterItIsCreated() {
        val vm = started(storeWithCash(portfolios = emptyList()), newId = ids())
        vm.openBuySellMenu()
        vm.chooseBuy()
        assertEquals(BuyNotice.NEEDS_PORTFOLIO, vm.state.value.buyNotice)
        vm.createPortfolioForBuy()
        assertEquals(null, vm.state.value.buyNotice)
        vm.onPortfolioNameChange("Main")
        vm.createPortfolio()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        assertEquals(null, state.portfolioDialog)
        assertEquals(state.portfolios.single().id, state.buyForm.portfolioId)
    }

    @Test
    fun cancellingThePortfolioDialog_doesNotReturnToBuyLater() {
        val vm = started(storeWithCash(portfolios = emptyList()), newId = ids())
        vm.openBuySellMenu()
        vm.chooseBuy()
        vm.createPortfolioForBuy()
        vm.cancelPortfolioDialog()
        vm.openPortfolioDialog()
        vm.onPortfolioNameChange("Main")
        vm.createPortfolio()
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    @Test
    fun sell_withNothingHeld_saysSo_andWithAHolding_pointsToALaterBuild() {
        val empty = started(storeWithCash())
        empty.openBuySellMenu()
        empty.chooseSell()
        assertEquals(BuyNotice.NOTHING_TO_SELL, empty.state.value.buyNotice)
        val (transaction, line) = boughtBefore()
        val store = storeWithCash().also {
            it.stored = it.stored + line
            it.storedTransactions = listOf(transaction)
        }
        val holding = started(store)
        holding.openBuySellMenu()
        holding.chooseSell()
        assertEquals(BuyNotice.SELL_LATER, holding.state.value.buyNotice)
        holding.dismissBuyNotice()
        assertEquals(null, holding.state.value.buyNotice)
    }

    @Test
    fun buy_opensOnTheFirstPortfolio_datedTodayInNewYork() {
        val vm = started(storeWithCash(portfolios = listOf(second.copy(createdSeq = 2L), main)))
        vm.openBuySellMenu()
        vm.chooseBuy()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        assertEquals("p1", state.buyForm.portfolioId)
        assertEquals("20261026", state.buyForm.dateText)
        assertEquals("", state.buyForm.tickerText)
    }

    @Test
    fun theDate_takesDigitsOnly_asTheCashFilters() {
        val vm = started(storeWithCash())
        vm.chooseBuy()
        vm.onBuyDateChange("202602011")
        assertEquals("20261026", vm.state.value.buyForm.dateText)
        vm.onBuyDateChange("2026-02")
        assertEquals("2026-02", vm.state.value.buyForm.dateText)
        assertEquals(DateRefusal.NOT_DIGITS, vm.state.value.buyRefusals.date)
        vm.onBuyDateChange("202602")
        assertEquals(null, vm.state.value.buyRefusals.date)
    }

    @Test
    fun review_aShortOrEmptyDate_isRefused() {
        val vm = started(storeWithCash())
        vm.chooseBuy()
        vm.fillBuy(date = "202602")
        vm.reviewBuy()
        assertEquals(DateRefusal.NOT_A_DATE, vm.state.value.buyRefusals.date)
        vm.onBuyDateChange("")
        vm.reviewBuy()
        assertEquals(DateRefusal.EMPTY, vm.state.value.buyRefusals.date)
        assertEquals(Screen.BUY, vm.state.value.screen)
    }

    @Test
    fun theTicker_isShownInCapitals_andThePortfolioCanBeChanged() {
        val vm = started(storeWithCash(portfolios = listOf(main, second)))
        vm.chooseBuy()
        vm.onBuyTickerChange("brk.b")
        vm.onBuyPortfolioChange("p2")
        assertEquals("BRK.B", vm.state.value.buyForm.tickerText)
        assertEquals("p2", vm.state.value.buyForm.portfolioId)
        vm.onBuyPortfolioChange("p9")
        assertEquals("p2", vm.state.value.buyForm.portfolioId)
    }

    @Test
    fun thePricePerShare_followsTheQuantityAndTotal() {
        val vm = started(storeWithCash())
        vm.chooseBuy()
        vm.fillBuy(quantity = "10", total = "1500.00")
        assertEquals(dollars(150), vm.state.value.buyPricePerShare)
        vm.onBuyQuantityChange("0")
        assertEquals(null, vm.state.value.buyPricePerShare)
        vm.onBuyQuantityChange("3")
        vm.onBuyTotalChange("1000")
        assertEquals(Fraction.of(BigInteger.valueOf(1_000), BigInteger.valueOf(3)), vm.state.value.buyPricePerShare)
    }

    @Test
    fun review_showsEveryFieldRefusalTogether_keepsTheText_andAsksNoSource() {
        val source = FakeSource()
        val vm = started(storeWithCash(), source = source)
        vm.chooseBuy()
        vm.fillBuy(ticker = "  ", date = "20260230", quantity = "1.123456789", total = "0")
        vm.reviewBuy()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        assertTrue(state.buyRefusals.tickerEmpty)
        assertEquals(DateRefusal.NOT_A_DATE, state.buyRefusals.date)
        assertEquals(QuantityError.TOO_MANY_DECIMALS, state.buyRefusals.quantity)
        assertEquals(AmountError.ZERO, state.buyRefusals.total)
        assertEquals("1.123456789", state.buyForm.quantityText)
        assertEquals(emptyList<String>(), source.asked)
    }

    @Test
    fun review_aBuyOverTheCash_isRefusedByTheRules_beforeAnyLookup() {
        val source = FakeSource(answer = Lookup.Found(aaplListing))
        val vm = started(storeWithCash(cents = 100_000L), source = source)
        vm.chooseBuy()
        vm.fillBuy(total = "1500.00")
        vm.reviewBuy()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        assertEquals(BuyRefusal.NegativeCash(LocalDate.of(2026, 2, 1), 50_000L), state.buyRefusals.rule)
        assertEquals(emptyList<String>(), source.asked)
    }

    @Test
    fun review_aNewTickerTheSourceAccepts_showsTheReview_andKeepsItVerified() {
        val source = FakeSource(answer = Lookup.Found(aaplListing))
        val store = storeWithCash()
        val vm = started(store, source = source)
        vm.chooseBuy()
        vm.fillBuy(ticker = "aapl")
        vm.reviewBuy()
        val state = vm.state.value
        assertEquals(Screen.BUY_REVIEW, state.screen)
        assertEquals(listOf("AAPL"), source.asked)
        assertEquals(listOf(aaplVerified), store.savedInstruments)
        assertEquals(listOf(aaplVerified), state.instruments)
        assertFalse(state.checking)
    }

    @Test
    fun review_aVerifiedTicker_needsNoLookup_evenOffline() {
        val source = FakeSource()
        val store = storeWithCash().also { it.storedInstruments = listOf(aaplVerified) }
        val vm = started(store, source = source, online = { false })
        vm.chooseBuy()
        vm.fillBuy()
        vm.reviewBuy()
        assertEquals(Screen.BUY_REVIEW, vm.state.value.screen)
        assertEquals(emptyList<String>(), source.asked)
    }

    @Test
    fun review_aNewTickerOffline_isRefused_withoutALookup() {
        val source = FakeSource(answer = Lookup.Found(aaplListing))
        val vm = started(storeWithCash(), source = source, online = { false })
        vm.chooseBuy()
        vm.fillBuy()
        vm.reviewBuy()
        assertEquals(Screen.BUY, vm.state.value.screen)
        assertEquals(CheckRefusal.Offline, vm.state.value.buyRefusals.check)
        assertEquals(emptyList<String>(), source.asked)
    }

    @Test
    fun review_aCheckThatFails_staysOnTheForm_withTheReason() {
        val cases =
            listOf(
                Lookup.NotFound to CheckRefusal.Ticker(TickerRefusal.NotFound),
                Lookup.Found(Listing("Common Stock", "OOTC", "USD")) to CheckRefusal.Ticker(TickerRefusal.NotUsExchange("OOTC")),
                Lookup.Unreadable to CheckRefusal.Unreadable,
                Lookup.KeyRefused(401) to CheckRefusal.KeyRefused(401),
                Lookup.TooManyCalls to CheckRefusal.TooManyCalls,
                Lookup.NoAnswer(503) to CheckRefusal.NoAnswer(503),
                Lookup.NoKey to CheckRefusal.NoKey,
            )
        for ((answer, expected) in cases) {
            val store = storeWithCash()
            val vm = started(store, source = FakeSource(answer = answer))
            vm.chooseBuy()
            vm.fillBuy(ticker = "AAPPL")
            vm.reviewBuy()
            val state = vm.state.value
            assertEquals(answer.toString(), Screen.BUY, state.screen)
            assertEquals(answer.toString(), expected, state.buyRefusals.check)
            assertEquals("AAPPL", state.buyForm.tickerText)
            assertEquals(emptyList<Instrument>(), store.savedInstruments)
        }
    }

    @Test
    fun typing_clearsTheCheckRefusal() {
        val vm = started(storeWithCash(), source = FakeSource(answer = Lookup.NotFound))
        vm.chooseBuy()
        vm.fillBuy(ticker = "AAPPL")
        vm.reviewBuy()
        vm.onBuyTickerChange("AAPL")
        assertEquals(null, vm.state.value.buyRefusals.check)
    }

    @Test
    fun whileTheCheckRuns_reviewAndTheTabsAreIgnored() {
        val gate = CompletableDeferred<Unit>()
        val source = FakeSource(answer = Lookup.Found(aaplListing), gate = gate)
        val vm = started(storeWithCash(), source = source)
        vm.chooseBuy()
        vm.fillBuy()
        vm.reviewBuy()
        assertTrue(vm.state.value.checking)
        vm.reviewBuy()
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.BUY, vm.state.value.screen)
        assertEquals(1, source.asked.size)
        gate.complete(Unit)
        assertEquals(Screen.BUY_REVIEW, vm.state.value.screen)
        assertFalse(vm.state.value.checking)
    }

    @Test
    fun backToEdit_keepsTheValues() {
        val vm = started(storeWithCash().also { it.storedInstruments = listOf(aaplVerified) })
        vm.chooseBuy()
        vm.fillBuy()
        vm.reviewBuy()
        vm.backToBuy()
        assertEquals(Screen.BUY, vm.state.value.screen)
        assertEquals("1500.00", vm.state.value.buyForm.totalText)
    }

    @Test
    fun confirm_savesTheBuyAndItsCashLineTogether_andShowsThemAtCost() {
        val store = storeWithCash().also { it.storedInstruments = listOf(aaplVerified) }
        val vm = started(store, newId = ids())
        vm.chooseBuy()
        vm.fillBuy(ticker = "AAPL", date = "20260201", quantity = "10", total = "1500.00")
        vm.reviewBuy()
        vm.confirmBuy()
        val (transaction, line) = store.savedBuys.single()
        assertEquals("AAPL", transaction.ticker)
        assertEquals("p1", transaction.portfolioId)
        assertEquals(BigDecimal("10"), transaction.quantity)
        assertEquals(150_000L, transaction.totalCents)
        assertEquals(-150_000L, line.amountCents)
        assertEquals(transaction.id, line.transactionId)
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(BuyForm(), state.buyForm)
        assertTrue(state.entries.contains(line))
        assertEquals(listOf(transaction), state.transactions)
        assertEquals(1, state.holdings.size)
        assertEquals(dollars(1_500), state.holdings.single().cost)
        assertEquals(1_000_000L, state.netWorthCents)
        assertEquals(dollars(1_500), state.portfolioValue("p1"))
    }

    @Test
    fun confirm_whenTheSaveFails_staysOnTheReview_andSavesNothing() {
        val store =
            FakeStore(stored = listOf(opening()), saveError = IllegalStateException("disk")).also {
                it.storedPortfolios = listOf(main)
                it.storedInstruments = listOf(aaplVerified)
            }
        val vm = started(store)
        vm.chooseBuy()
        vm.fillBuy()
        vm.reviewBuy()
        vm.confirmBuy()
        val state = vm.state.value
        assertEquals(Screen.BUY_REVIEW, state.screen)
        assertEquals(SaveRefusal.SAVE_FAILED, state.buyRefusals.save)
        assertEquals(emptyList<Transaction>(), state.transactions)
        assertFalse(state.saving)
    }

    @Test
    fun loading_aStoredBuy_countsItAtCostInNetWorth() {
        val (transaction, line) = boughtBefore()
        val store = storeWithCash().also {
            it.stored = it.stored + line
            it.storedTransactions = listOf(transaction)
        }
        val state = started(store).state.value
        assertEquals(1_000_000L, state.netWorthCents)
        assertEquals(listOf(transaction), state.transactions)
        assertEquals(dollars(1_000), state.portfolioValue("p1"))
        assertEquals(1, state.holdingsOf("p1").size)
    }

    @Test
    fun aTab_leavesTheBuyForm_andBuyReopensItWithTheTextKept() {
        val vm = started(storeWithCash())
        vm.chooseBuy()
        vm.fillBuy(ticker = "SPY")
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS, vm.state.value.screen)
        vm.chooseBuy()
        assertEquals(Screen.BUY, vm.state.value.screen)
        assertEquals("SPY", vm.state.value.buyForm.tickerText)
    }

    @Test
    fun anUnlock_keepsTheBuyForm() {
        val vm = started(storeWithCash())
        vm.chooseBuy()
        vm.onUnlocked()
        assertEquals(Screen.BUY, vm.state.value.screen)
    }

    @Test
    fun closeBuy_returnsToTheTab_keepingTheText() {
        val vm = started(storeWithCash())
        vm.chooseBuy()
        vm.fillBuy(ticker = "SPY")
        vm.closeBuy()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals("SPY", vm.state.value.buyForm.tickerText)
    }
}
