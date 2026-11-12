package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.HistoryChip
import app.ghghorg.atsassets.core.HistoryRow
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 27 step 3: Transaction history in the screen state (view 8, wireframe 08; decisions 1 to 3; AC-10, AC-50).
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryFlowTest {
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
        quantity: String,
        totalCents: Long,
        seq: Long,
        portfolioId: String = "p1",
    ) = Transaction(id, portfolioId, ticker, TransactionType.BUY, LocalDate.parse(date), seq, BigDecimal(quantity), totalCents)

    private fun sold(
        id: String,
        ticker: String,
        date: String,
        totalCents: Long,
        seq: Long,
        vararg picks: Pair<String, String>,
    ) = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.SELL,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + BigDecimal(pick.second) },
        totalCents = totalCents,
        allocations = picks.map { SellAllocation(it.first, Fraction.of(BigDecimal(it.second))) },
    )

    // Example A in Portfolio P, the hidden OLDCO sold for $0, and a buy in Portfolio Q.
    private val a1 = bought("a1", "AAPL", "2026-01-02", "10", 100_000L, 2L)
    private val a2 = bought("a2", "AAPL", "2026-02-01", "10", 150_000L, 3L)
    private val aSell = sold("as", "AAPL", "2026-03-01", 210_000L, 4L, "a1" to "10", "a2" to "2")
    private val o1 = bought("o1", "OLDCO", "2026-01-03", "5", 20_000L, 5L)
    private val oSell = sold("os", "OLDCO", "2026-03-10", 0L, 6L, "o1" to "5")
    private val q1 = bought("q1", "MSFT", "2026-02-15", "1", 30_000L, 7L, portfolioId = "p2")

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    private val deposit = CashEntry("dep", LocalDate.parse("2026-03-01"), 5_000L, CashType.DEPOSIT, "", 8L)

    private fun store(): FakeStore {
        val transactions = listOf(a1, a2, aSell, o1, oSell, q1)
        val store = FakeStore(stored = listOf(opening(), deposit) + transactions.map { lineOf(it) })
        store.storedPortfolios = listOf(main, other)
        store.storedTransactions = transactions
        store.storedInstruments = listOf("AAPL", "OLDCO", "MSFT").map { Instrument(it, AssetType.STOCK, verified = true) }
        store.storedHidden = listOf(HiddenHolding("p1", "OLDCO"))
        return store
    }

    /** Started offline so no refresh runs, Portfolio P opened from the Portfolios tab, then its history. */
    private fun onHistory(store: FakeStore = store()): AppViewModel {
        val vm = started(store, online = { false })
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openTransactionHistory()
        return vm
    }

    private fun AppViewModel.rowIds(): List<String> = state.value.historyRows.map {
        when (it) {
            is HistoryRow.Entry -> it.transaction.id
            is HistoryRow.Cash -> it.line.id
        }
    }

    private fun AppViewModel.filters() = with(state.value) { listOf(historyFromText, historyToText, historyTicker, historyChip) }

    private val cleared = listOf("", "", null, HistoryChip.ALL)

    // View 8 and AC-50: opened from the portfolio, all its entries newest first, the hidden OLDCO's too; "<" returns.
    @Test
    fun opensFromThePortfolio_withAllItsEntries_andClosesToIt() {
        val vm = onHistory()
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
        assertEquals(listOf("os", "as", "a2", "o1", "a1"), vm.rowIds())
        assertEquals(listOf("AAPL", "OLDCO"), vm.state.value.historyTickers)
        vm.closeTransactionHistory()
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
        assertEquals("p1", vm.state.value.portfolioId)
    }

    // Only from a portfolio's screen.
    @Test
    fun opensOnlyFromAPortfolio() {
        val vm = started(store(), online = { false })
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openTransactionHistory()
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    // AC-10: the Cash chip lists the portfolio's trade lines only: not the deposit, the opening balance or Q's line.
    @Test
    fun theCashChip_listsOnlyThisPortfoliosTradeLines() {
        val vm = onHistory()
        vm.selectHistoryChip(HistoryChip.CASH)
        assertEquals(listOf("c-os", "c-as", "c-a2", "c-o1", "c-a1"), vm.rowIds())
    }

    // View 8: the ticker, the range and the chip combine; a ninth digit is not accepted; a field not a date counts as open.
    @Test
    fun theFiltersCombine() {
        val vm = onHistory()
        vm.openHistoryTickerMenu()
        assertTrue(vm.state.value.historyTickerMenuOpen)
        vm.pickHistoryTicker("AAPL")
        assertFalse(vm.state.value.historyTickerMenuOpen)
        assertEquals(listOf("as", "a2", "a1"), vm.rowIds())
        vm.selectHistoryChip(HistoryChip.BUY)
        assertEquals(listOf("a2", "a1"), vm.rowIds())
        vm.onHistoryFromChange("20260201")
        vm.onHistoryFromChange("202602011")
        assertEquals("20260201", vm.state.value.historyFromText)
        assertEquals(listOf("a2"), vm.rowIds())
        vm.onHistoryToChange("20260230")
        assertEquals(listOf("a2"), vm.rowIds())
        vm.onHistoryFromChange("20260401")
        assertEquals(emptyList<String>(), vm.rowIds())
        vm.openHistoryTickerMenu()
        vm.pickHistoryTicker(null)
        vm.selectHistoryChip(HistoryChip.ALL)
        vm.onHistoryFromChange("")
        vm.onHistoryToChange("")
        assertEquals(listOf("os", "as", "a2", "o1", "a1"), vm.rowIds())
    }

    // Decision 3: a ticker not offered is ignored; Back closes the menu first, then the screen.
    @Test
    fun theTickerMenu_ignoresAnUnknownTicker_andBackClosesItFirst() {
        val vm = onHistory()
        vm.openHistoryTickerMenu()
        vm.pickHistoryTicker("MSFT")
        assertNull(vm.state.value.historyTicker)
        assertTrue(vm.state.value.historyTickerMenuOpen)
        vm.closeTransactionHistory()
        assertFalse(vm.state.value.historyTickerMenuOpen)
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
        vm.closeTransactionHistory()
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
    }

    // View 8 and decision 2: a row opens its transaction; Back returns to the history with its filters.
    @Test
    fun aRowOpensItsTransaction_andBackReturnsAsItWas() {
        val vm = onHistory()
        vm.openHistoryTickerMenu()
        vm.pickHistoryTicker("AAPL")
        vm.selectHistoryChip(HistoryChip.SELL)
        vm.openTransaction("as")
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals("as", vm.state.value.transactionId)
        vm.closeTransaction()
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
        assertEquals(listOf("", "", "AAPL", HistoryChip.SELL), vm.filters())
    }

    // View 8: a cash-effect line opens its trade line; its Back, and its link's transaction's Back, return to the history.
    @Test
    fun aCashLine_andItsLinkedTransaction_returnToTheHistory() {
        val vm = onHistory()
        vm.openHistoryCashLine("a2")
        assertEquals(Screen.DETAIL, vm.state.value.screen)
        assertEquals("c-a2", vm.state.value.detailId)
        vm.closeDetail()
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
        vm.openHistoryCashLine("a2")
        vm.openLinkedTransaction()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals("a2", vm.state.value.transactionId)
        vm.closeTransaction()
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
        vm.openHistoryCashLine("q1")
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
    }

    // Risk 4: the Cash tab's own path is unchanged: a trade line's transaction still returns to the Cash tab.
    @Test
    fun theCashTabsPath_staysAsItWas() {
        val vm = started(store(), online = { false })
        vm.selectTab(Tab.CASH)
        vm.openDetail("c-a2")
        vm.openLinkedTransaction()
        vm.closeTransaction()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.CASH, vm.state.value.tab)
    }

    // 4.6 and AC-50: a reversal made from a transaction opened here shows on return, under Sell, the pair linked, and
    // its own cash line under Cash.
    @Test
    fun aReversalMadeFromHere_showsOnReturn() {
        val store = store()
        val vm = onHistory(store)
        vm.openTransaction("as")
        vm.openTransactionReversal()
        vm.confirmTransactionReversal()
        assertEquals(1, store.savedReversals.size)
        vm.closeTransaction()
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
        vm.selectHistoryChip(HistoryChip.SELL)
        val rows = vm.state.value.historyRows.map { it as HistoryRow.Entry }
        val reversal = rows.first { it.transaction.type == TransactionType.REVERSAL }
        assertEquals("as", reversal.original?.id)
        assertEquals(-210_000L, reversal.cashLine?.amountCents)
        assertEquals(reversal.transaction.id, rows.single { it.transaction.id == "as" }.reversal?.id)
        // Dated the sell's date (Mar 1) and entered after it: after OLDCO's Mar 10 line, before the sell's.
        vm.selectHistoryChip(HistoryChip.CASH)
        assertEquals(listOf("c-os", reversal.cashLine?.id, "c-as", "c-a2", "c-o1", "c-a1"), vm.rowIds())
    }

    // Decision 2: "<" to the portfolio, or a tab, ends the filters; opening again starts on all.
    @Test
    fun leavingByBackOrATab_resetsTheFilters() {
        val vm = onHistory()
        vm.onHistoryFromChange("20260201")
        vm.selectHistoryChip(HistoryChip.SELL)
        vm.closeTransactionHistory()
        vm.openTransactionHistory()
        assertEquals(cleared, vm.filters())
        vm.openHistoryTickerMenu()
        vm.pickHistoryTicker("AAPL")
        vm.selectTab(Tab.CASH)
        assertEquals(cleared, vm.filters())
        assertNull(vm.state.value.historyPortfolioId)
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openTransactionHistory()
        assertEquals(cleared, vm.filters())
    }

    // CR2's wiring test: the screen draws from the state's model, which follows the filters and the menu.
    @Test
    fun theScreenDrawsFromTheStatesModel() {
        val vm = onHistory()
        val state = vm.state.value
        val expected =
            TransactionHistories.of(state.historyRows, "Portfolio P", "", "", null, listOf("AAPL", "OLDCO"), HistoryChip.ALL, menuOpen = false)
        assertEquals(expected, state.transactionHistoryScreen)
        vm.openHistoryTickerMenu()
        assertTrue(vm.state.value.transactionHistoryScreen.menuOpen)
        vm.pickHistoryTicker("AAPL")
        vm.selectHistoryChip(HistoryChip.CASH)
        val shown = vm.state.value.transactionHistoryScreen
        assertEquals("AAPL" to HistoryChip.CASH, shown.ticker to shown.chip)
        assertEquals(listOf("c-as", "c-a2", "c-a1"), shown.rows.map { it.key })
    }

    // Decision 1: a sale on Realized gains opens its portfolio's history with its ticker and Sell; Back returns as it was.
    @Test
    fun aSaleOnRealizedGains_opensTheHistory_andBackReturnsAsItWas() {
        val vm = started(store(), online = { false })
        vm.selectTab(Tab.MORE)
        vm.openRealizedGains()
        vm.onGainsFromChange("20260301")
        vm.toggleGainsHolding("p1", "AAPL")
        vm.openGainsSale("as")
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
        assertEquals("p1", vm.state.value.historyPortfolioId)
        assertEquals(listOf("", "", "AAPL", HistoryChip.SELL), vm.filters())
        assertEquals(listOf("as"), vm.rowIds())
        vm.openTransaction("as")
        vm.closeTransaction()
        assertEquals(Screen.TRANSACTION_HISTORY, vm.state.value.screen)
        vm.closeTransactionHistory()
        assertEquals(Screen.REALIZED_GAINS, vm.state.value.screen)
        assertEquals("20260301", vm.state.value.gainsFromText)
        assertEquals(setOf("p1" to "AAPL"), vm.state.value.gainsExpanded)
        assertNull(vm.state.value.historyPortfolioId)
        vm.openGainsSale("a1")
        assertEquals(Screen.REALIZED_GAINS, vm.state.value.screen)
    }
}
