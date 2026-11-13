package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TradeSide
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 28 step 3: the Cash tab's ticker, Buy and Sell chips and Ticker menu in the screen state (view 7, wireframe 07; AC-49).
@OptIn(ExperimentalCoroutinesApi::class)
class CashTickerFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val main = Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L)
    private val other = Portfolio(id = "p2", name = "Portfolio Q", createdSeq = 2L)

    private fun bought(
        id: String,
        ticker: String,
        date: String,
        totalCents: Long,
        seq: Long,
        portfolioId: String = "p1",
    ) = Transaction(id, portfolioId, ticker, TransactionType.BUY, LocalDate.parse(date), seq, BigDecimal.TEN, totalCents)

    // Example A in Portfolio P (AC-49), and a buy of MSFT in Portfolio Q.
    private val a1 = bought("a1", "AAPL", "2026-01-02", 100_000L, 2L)
    private val a2 = bought("a2", "AAPL", "2026-02-01", 150_000L, 3L)
    private val aSell =
        Transaction(
            id = "as",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SELL,
            date = LocalDate.parse("2026-03-01"),
            enteredSeq = 4L,
            quantity = BigDecimal(12),
            totalCents = 210_000L,
            allocations = listOf(SellAllocation("a1", Fraction.of(BigDecimal.TEN)), SellAllocation("a2", Fraction.of(BigDecimal(2)))),
        )
    private val q1 = bought("q1", "MSFT", "2026-02-15", 30_000L, 5L, portfolioId = "p2")

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    private val deposit = CashEntry("dep", LocalDate.parse("2026-03-05"), 5_000L, CashType.DEPOSIT, "", 6L)

    /** Started offline so no refresh runs, on the Cash tab. */
    private fun onCash(): AppViewModel {
        val transactions = listOf(a1, a2, aSell, q1)
        val store = FakeStore(stored = listOf(opening(), deposit) + transactions.map { lineOf(it) })
        store.storedPortfolios = listOf(main, other)
        store.storedTransactions = transactions
        store.storedInstruments = listOf("AAPL", "MSFT").map { Instrument(it, AssetType.STOCK, verified = true) }
        val vm = started(store, online = { false })
        vm.selectTab(Tab.CASH)
        return vm
    }

    private fun AppViewModel.shown(): List<String> = state.value.cashFilteredRows.map { it.entry.id }

    private fun AppViewModel.pick(ticker: String?) {
        openCashTickerMenu()
        pickCashTicker(ticker)
    }

    // View 7: the menu opens on the Cash tab and offers each ticker with a trade line, every portfolio's, alphabetically.
    @Test
    fun theMenu_opensOnTheCashTab_withEveryTickerWithATradeLine() {
        val vm = onCash()
        vm.openCashTickerMenu()
        assertTrue(vm.state.value.cashTickerMenuOpen)
        assertEquals(listOf("AAPL", "MSFT"), vm.state.value.cashTickers)
    }

    // Only on the Cash tab.
    @Test
    fun theMenu_doesNotOpenOnAnotherTab() {
        val vm = onCash()
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openCashTickerMenu()
        assertFalse(vm.state.value.cashTickerMenuOpen)
    }

    // AC-49: ticker AAPL with All: Example A's three trade lines, no opening balance, no deposit, not Q's MSFT; the real
    // balances; the menu closes on the pick.
    @Test
    fun pickingATicker_showsOnlyItsTradeLines_andClosesTheMenu() {
        val vm = onCash()
        vm.pick("AAPL")
        assertFalse(vm.state.value.cashTickerMenuOpen)
        assertEquals("AAPL", vm.state.value.cashFilterForm.ticker)
        assertEquals(listOf("c-as", "c-a2", "c-a1"), vm.shown())
        assertEquals(listOf(930_000L, 750_000L, 900_000L), vm.state.value.cashFilteredRows.map { it.balanceAfterCents })
        assertTrue(vm.state.value.filterActive)
    }

    // AC-49: a ticker picked while Deposit is on sets All.
    @Test
    fun pickingATicker_whileADirectChipIsOn_setsAll() {
        val vm = onCash()
        vm.onFilterTypeChange(CashType.DEPOSIT)
        vm.pick("AAPL")
        assertNull(vm.state.value.cashFilterForm.type)
        assertNull(vm.state.value.cashFilterForm.trade)
    }

    // AC-49: with a ticker the direct chips change nothing; Sell shows only the Mar 1 sell, still at $9,300.00 here
    // (Example A's $9,600.00 less Q's MSFT buy).
    @Test
    fun withATicker_aDirectChipIsIgnored_andSellShowsOnlyTheSell() {
        val vm = onCash()
        vm.pick("AAPL")
        vm.onFilterTypeChange(CashType.DEPOSIT)
        assertNull(vm.state.value.cashFilterForm.type)
        vm.onFilterTradeChange(TradeSide.SELL)
        assertEquals(TradeSide.SELL, vm.state.value.cashFilterForm.trade)
        assertEquals(listOf("c-as"), vm.shown())
        assertEquals(listOf(930_000L), vm.state.value.cashFilteredRows.map { it.balanceAfterCents })
    }

    // One chip at a time: Buy, Sell and the direct chips replace each other; All clears both.
    @Test
    fun oneChipAtATime() {
        val vm = onCash()
        vm.onFilterTradeChange(TradeSide.SELL)
        vm.onFilterTypeChange(CashType.DEPOSIT)
        assertEquals(CashType.DEPOSIT, vm.state.value.cashFilterForm.type)
        assertNull(vm.state.value.cashFilterForm.trade)
        vm.onFilterTradeChange(TradeSide.BUY)
        assertNull(vm.state.value.cashFilterForm.type)
        assertEquals(TradeSide.BUY, vm.state.value.cashFilterForm.trade)
        assertEquals(listOf("c-q1", "c-a2", "c-a1"), vm.shown())
        vm.onFilterTypeChange(null)
        assertNull(vm.state.value.cashFilterForm.type)
        assertNull(vm.state.value.cashFilterForm.trade)
    }

    // AC-49: "All tickers" enables the direct chips again and keeps the chip that is on.
    @Test
    fun allTickers_enablesTheDirectChipsAgain_andKeepsTheChip() {
        val vm = onCash()
        vm.pick("AAPL")
        vm.onFilterTradeChange(TradeSide.SELL)
        vm.pick(null)
        assertNull(vm.state.value.cashFilterForm.ticker)
        assertEquals(TradeSide.SELL, vm.state.value.cashFilterForm.trade)
        vm.onFilterTypeChange(CashType.DEPOSIT)
        assertEquals(CashType.DEPOSIT, vm.state.value.cashFilterForm.type)
        assertEquals(listOf("dep"), vm.shown())
    }

    // A ticker not offered is ignored, and a pick needs the menu open.
    @Test
    fun aTickerNotOffered_orAPickWithoutTheMenu_isIgnored() {
        val vm = onCash()
        vm.openCashTickerMenu()
        vm.pickCashTicker("OLDCO")
        assertNull(vm.state.value.cashFilterForm.ticker)
        assertTrue(vm.state.value.cashTickerMenuOpen)
        vm.closeCashTickerMenu()
        vm.pickCashTicker("AAPL")
        assertNull(vm.state.value.cashFilterForm.ticker)
    }

    // AC-49: Clear filters resets the dates, the ticker and the chip.
    @Test
    fun clearFilters_resetsTheDatesTheTickerAndTheChip() {
        val vm = onCash()
        vm.onFilterFromChange("20260201")
        vm.pick("AAPL")
        vm.onFilterTradeChange(TradeSide.BUY)
        vm.clearFilters()
        assertEquals(CashFilterForm(), vm.state.value.cashFilterForm)
        assertFalse(vm.state.value.filterActive)
    }

    // Back closes the menu; a tab closes it too, and the filters stay while the app runs (06 decision 4).
    @Test
    fun backOrATab_closesTheMenu_andTheFiltersStay() {
        val vm = onCash()
        vm.pick("AAPL")
        vm.openCashTickerMenu()
        vm.closeCashTickerMenu()
        assertFalse(vm.state.value.cashTickerMenuOpen)
        vm.openCashTickerMenu()
        vm.selectTab(Tab.OVERALL)
        assertFalse(vm.state.value.cashTickerMenuOpen)
        vm.selectTab(Tab.CASH)
        assertFalse(vm.state.value.cashTickerMenuOpen)
        assertEquals("AAPL", vm.state.value.cashFilterForm.ticker)
    }

    // An unlock shows Overall (02b decision 9), so the menu closes with the Cash tab; the filters stay.
    @Test
    fun anUnlock_closesTheMenu() {
        val vm = onCash()
        vm.pick("AAPL")
        vm.openCashTickerMenu()
        vm.onUnlocked()
        assertEquals(Tab.OVERALL, vm.state.value.tab)
        assertFalse(vm.state.value.cashTickerMenuOpen)
        assertEquals("AAPL", vm.state.value.cashFilterForm.ticker)
    }

    // 28 step 4 (CR2): the screen's model in the state follows the ticker and the chip: the rows, the tickers, the
    // direct chips' state, and the same model as a fresh build.
    @Test
    fun theScreensModel_followsTheTickerAndTheChip() {
        val vm = onCash()
        vm.pick("AAPL")
        vm.onFilterTradeChange(TradeSide.SELL)
        val model = vm.state.value.cashList
        assertEquals(CashList.of(vm.state.value.entries, vm.state.value.transactions, vm.state.value.portfolios, vm.state.value.cashFilterForm), model)
        assertEquals(listOf("c-as"), model.rows.map { it.key })
        assertEquals(listOf("AAPL", "MSFT"), model.tickers)
        assertEquals("AAPL", model.ticker)
        assertFalse(model.directChipsEnabled)
        assertTrue(model.filterActive)
    }

    // The menu's flag is not an input of the kept Cash list (15d): opening and closing it only reads the list.
    @Test
    fun theMenu_doesNotRebuildTheCashList() {
        val vm = onCash()
        val before = vm.state.value.cashList
        vm.openCashTickerMenu()
        vm.closeCashTickerMenu()
        assertTrue(vm.state.value.cashTickers.isNotEmpty())
        assertSame(before, vm.state.value.cashList)
    }
}
