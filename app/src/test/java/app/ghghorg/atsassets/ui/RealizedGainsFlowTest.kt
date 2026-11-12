package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
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
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 26 step 3: Realized gains in the screen state (view 5, wireframe 05; decisions 1 to 3; AC-11, AC-23, AC-47).
@OptIn(ExperimentalCoroutinesApi::class)
class RealizedGainsFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val main = Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L)

    private fun bought(
        id: String,
        ticker: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
    ) = Transaction(id, "p1", ticker, TransactionType.BUY, LocalDate.parse(date), seq, BigDecimal(quantity), totalCents)

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

    // The sample file's sales: Example A's Mar 1 AAPL sell (+$800) and the hidden OLDCO's Mar 10 sell for $0 (-$200).
    private val a1 = bought("a1", "AAPL", "2026-01-02", "10", 100_000L, 2L)
    private val a2 = bought("a2", "AAPL", "2026-02-01", "10", 150_000L, 3L)
    private val aSell = sold("as", "AAPL", "2026-03-01", 210_000L, 4L, "a1" to "10", "a2" to "2")
    private val o1 = bought("o1", "OLDCO", "2026-01-03", "5", 20_000L, 5L)
    private val oSell = sold("os", "OLDCO", "2026-03-10", 0L, 6L, "o1" to "5")
    private val aapl = "p1" to "AAPL"
    private val oldco = "p1" to "OLDCO"

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    private fun store(): FakeStore {
        val transactions = listOf(a1, a2, aSell, o1, oSell)
        val store = FakeStore(stored = listOf(opening()) + transactions.filter { it.totalCents != 0L }.map { lineOf(it) })
        store.storedPortfolios = listOf(main)
        store.storedTransactions = transactions
        store.storedInstruments = listOf("AAPL", "OLDCO").map { Instrument(it, AssetType.STOCK, verified = true) }
        store.storedHidden = listOf(HiddenHolding("p1", "OLDCO"))
        return store
    }

    /** Started on More, offline so no refresh runs, then Realized gains opened. */
    private fun onGains(store: FakeStore = store()): AppViewModel {
        val vm = started(store, online = { false })
        vm.selectTab(Tab.MORE)
        vm.openRealizedGains()
        return vm
    }

    private fun AppViewModel.tickers(): List<String> = state.value.realizedGains.holdings.map { it.ticker }

    private fun dollars(cents: Long): Fraction = Fraction.ofCents(cents)

    // AC-47: all dates at first; AAPL then the hidden OLDCO, total +$600 from two sales; "<" returns to More.
    @Test
    fun opensFromMore_onAllDates_withTheHiddenHoldingListed_andClosesToMore() {
        val vm = onGains()
        assertEquals(Screen.REALIZED_GAINS, vm.state.value.screen)
        assertEquals(listOf("AAPL", "OLDCO"), vm.tickers())
        assertEquals(dollars(60_000L), vm.state.value.realizedGains.total)
        assertEquals(2, vm.state.value.realizedGains.count)
        vm.closeRealizedGains()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.MORE, vm.state.value.tab)
    }

    // Only from More, not from another tab.
    @Test
    fun opensOnlyFromMore() {
        val vm = started(store(), online = { false })
        vm.selectTab(Tab.OVERALL)
        vm.openRealizedGains()
        assertEquals(Screen.TABS, vm.state.value.screen)
    }

    // AC-23 and AC-47: Mar 1 to Mar 1 shows the AAPL sale alone; Apr 1 to Apr 30 none; Clear dates shows all again.
    @Test
    fun theDatesFilter_andClearDatesShowsAllAgain() {
        val vm = onGains()
        vm.onGainsFromChange("20260301")
        vm.onGainsToChange("20260301")
        assertEquals(listOf("AAPL"), vm.tickers())
        assertEquals(dollars(80_000L), vm.state.value.realizedGains.total)
        vm.onGainsFromChange("20260401")
        vm.onGainsToChange("20260430")
        assertEquals(emptyList<String>(), vm.tickers())
        assertEquals(0, vm.state.value.realizedGains.count)
        vm.clearGainsDates()
        assertEquals("" to "", vm.state.value.gainsFromText to vm.state.value.gainsToText)
        assertEquals(listOf("AAPL", "OLDCO"), vm.tickers())
    }

    // As the Cash list's fields: a ninth digit is not accepted; other characters are kept.
    @Test
    fun aNinthDigitIsNotAccepted_otherCharactersAreKept() {
        val vm = onGains()
        vm.onGainsFromChange("20260301")
        vm.onGainsFromChange("202603011")
        assertEquals("20260301", vm.state.value.gainsFromText)
        vm.onGainsToChange("2026-03")
        assertEquals("2026-03", vm.state.value.gainsToText)
    }

    // Decision 3: an end that is not a date, or half typed, counts as open, so the list still shows.
    @Test
    fun anEndThatIsNotADate_countsAsOpen() {
        val vm = onGains()
        vm.onGainsFromChange("20260230")
        assertEquals(listOf("AAPL", "OLDCO"), vm.tickers())
        vm.onGainsFromChange("202603")
        vm.onGainsToChange("20260305")
        assertEquals(listOf("AAPL"), vm.tickers())
        vm.onGainsToChange("2026x")
        assertEquals(listOf("AAPL", "OLDCO"), vm.tickers())
    }

    // View 5: a row expands and collapses; a holding not listed is ignored.
    @Test
    fun aHoldingRowExpandsAndCollapses_anUnlistedOneIsIgnored() {
        val vm = onGains()
        vm.toggleGainsHolding("p1", "AAPL")
        assertEquals(setOf(aapl), vm.state.value.gainsExpanded)
        vm.toggleGainsHolding("p1", "OLDCO")
        assertEquals(setOf(aapl, oldco), vm.state.value.gainsExpanded)
        vm.toggleGainsHolding("p1", "AAPL")
        assertEquals(setOf(oldco), vm.state.value.gainsExpanded)
        vm.toggleGainsHolding("p1", "MSFT")
        assertEquals(setOf(oldco), vm.state.value.gainsExpanded)
    }

    // Decisions 1 and 2: a sale opens the sell's own screen; Back returns to Realized gains with its dates and rows.
    @Test
    fun aSaleOpensTheSell_andBackReturnsAsItWas() {
        val vm = onGains()
        vm.onGainsFromChange("20260301")
        vm.toggleGainsHolding("p1", "AAPL")
        vm.openTransaction("as")
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals("as", vm.state.value.transactionId)
        vm.closeTransaction()
        assertEquals(Screen.REALIZED_GAINS, vm.state.value.screen)
        assertEquals("20260301", vm.state.value.gainsFromText)
        assertEquals(setOf(aapl), vm.state.value.gainsExpanded)
    }

    // View 5 and 4.6: a sale reversed from its own screen is gone on coming back, from the total and the count too.
    @Test
    fun aSaleReversedFromItsScreen_isGoneOnReturn() {
        val store = store()
        val vm = onGains(store)
        vm.toggleGainsHolding("p1", "AAPL")
        vm.openTransaction("as")
        vm.openTransactionReversal()
        vm.confirmTransactionReversal()
        assertEquals(1, store.savedReversals.size)
        vm.closeTransaction()
        assertEquals(Screen.REALIZED_GAINS, vm.state.value.screen)
        assertEquals(listOf("OLDCO"), vm.tickers())
        assertEquals(dollars(-20_000L), vm.state.value.realizedGains.total)
        assertEquals(1, vm.state.value.realizedGains.count)
    }

    // Decision 2: "<" to More, or a tab, ends the dates and the expanded rows; opening again starts on all dates.
    @Test
    fun leavingByBackOrATab_resetsTheDatesAndRows() {
        val vm = onGains()
        vm.onGainsFromChange("20260301")
        vm.toggleGainsHolding("p1", "AAPL")
        vm.closeRealizedGains()
        vm.openRealizedGains()
        assertEquals("" to "", vm.state.value.gainsFromText to vm.state.value.gainsToText)
        assertEquals(emptySet<Pair<String, String>>(), vm.state.value.gainsExpanded)
        vm.onGainsToChange("20260301")
        vm.toggleGainsHolding("p1", "AAPL")
        vm.selectTab(Tab.CASH)
        vm.selectTab(Tab.MORE)
        vm.openRealizedGains()
        assertEquals("" to "", vm.state.value.gainsFromText to vm.state.value.gainsToText)
        assertEquals(emptySet<Pair<String, String>>(), vm.state.value.gainsExpanded)
    }

    // CR2's wiring test: the screen draws from the state's model, built from the kept report, which follows each action.
    @Test
    fun theScreenDrawsFromTheStatesModel() {
        val vm = onGains()
        val state = vm.state.value
        assertEquals(RealizedGainsScreens.of(state.realizedGains, state.portfolios, "", "", emptySet()), state.realizedGainsScreen)
        assertEquals("+$600.00", state.realizedGainsScreen.total.text)
        vm.onGainsFromChange("20260301")
        vm.onGainsToChange("20260301")
        vm.toggleGainsHolding("p1", "AAPL")
        val shown = vm.state.value.realizedGainsScreen
        assertEquals(GainsCountLine.Between(1, "2026-03-01", "2026-03-01"), shown.countLine)
        assertEquals(listOf("as"), shown.holdings.single().sales.map { it.sellId })
    }

    // A tab from the sell opened here also ends Realized gains' dates and rows (decision 2).
    @Test
    fun aTabFromTheSellOpenedHere_resetsThemToo() {
        val vm = onGains()
        vm.onGainsFromChange("20260301")
        vm.toggleGainsHolding("p1", "AAPL")
        vm.openTransaction("as")
        vm.selectTab(Tab.MORE)
        assertEquals("" to "", vm.state.value.gainsFromText to vm.state.value.gainsToText)
        assertEquals(emptySet<Pair<String, String>>(), vm.state.value.gainsExpanded)
    }
}
