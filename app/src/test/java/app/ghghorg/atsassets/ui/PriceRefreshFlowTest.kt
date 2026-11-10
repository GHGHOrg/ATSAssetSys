package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Quote
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.UnreadableDataException
import app.ghghorg.atsassets.core.Valuation
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
import java.time.Instant
import java.time.LocalDate

// 16a step 5: the price refresh in the screen state (decisions 2 and 3; spec 7, 4.8, section 6 error and loading states).
@OptIn(ExperimentalCoroutinesApi::class)
class PriceRefreshFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val at = Instant.ofEpochSecond(1_793_304_000L)
    private val old = Price(BigDecimal("150"), Instant.ofEpochSecond(1_793_200_000L))

    private fun bought(
        id: String,
        ticker: String,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.BUY,
        date = LocalDate.of(2026, 2, 1),
        enteredSeq = 2L,
        quantity = BigDecimal("8"),
        totalCents = 120_000L,
    )

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    /** $9,600 cash and 8 AAPL at a cost of $1,200 (Example A after its sell). Which tickers: PriceRefreshTest. */
    private fun store(): FakeStore = FakeStore(
        stored = listOf(CashEntry("o1", LocalDate.of(2026, 1, 1), 960_000L, CashType.OPENING_BALANCE, "", 1L)),
    ).apply {
        storedTransactions = listOf(bought("b2", "AAPL"))
        storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, true, old))
    }

    @Test
    fun theLoad_refreshesOnline_andEveryFigureUsesTheNewPrice() {
        val store = store()
        val source = FakeSource().apply { quotes = mapOf("AAPL" to Quote.Priced(BigDecimal("160"), at)) }
        val vm = started(store, source = source)
        val state = vm.state.value
        assertEquals(listOf("AAPL"), source.quoted)
        assertEquals(listOf(mapOf("AAPL" to Price(BigDecimal("160"), at)) to listOf<String>()), store.savedPrices)
        assertEquals(Price(BigDecimal("160"), at), state.instruments.single { it.ticker == "AAPL" }.price)
        assertEquals(dollars(10_880), state.netWorth)
        assertFalse(state.refreshing)
        assertFalse(state.refreshFailed)
        assertKeptFieldsFresh(state, "after a refresh")
    }

    @Test
    fun theLoad_offline_asksNothing_keepsTheLastPricesMarkedStale_andSaysTheRefreshFailed() {
        val store = store()
        val source = FakeSource()
        val vm = started(store, source = source, online = { false })
        val state = vm.state.value
        assertEquals(emptyList<String>(), source.quoted)
        assertEquals(listOf(emptyMap<String, Price>() to listOf("AAPL")), store.savedPrices)
        assertEquals(old.copy(stale = true), state.instruments.single { it.ticker == "AAPL" }.price)
        assertEquals(dollars(10_800), state.netWorth)
        assertTrue(state.refreshFailed)
        assertFalse(state.refreshing)
    }

    @Test
    fun whileARefreshRuns_itIsUpdating_andASecondRequestIsIgnored() {
        val source = FakeSource().apply { quoteGate = CompletableDeferred() }
        val vm = started(store(), source = source)
        assertTrue(vm.state.value.refreshing)
        vm.onPullToRefresh()
        vm.onUnlocked()
        assertEquals(listOf("AAPL"), source.quoted)
        source.quoteGate!!.complete(Unit)
        assertFalse(vm.state.value.refreshing)
    }

    @Test
    fun anUnlock_andAPull_eachRefreshAgain() {
        val source = FakeSource()
        val vm = started(store(), source = source)
        vm.onUnlocked()
        vm.onPullToRefresh()
        assertEquals(listOf("AAPL", "AAPL", "AAPL"), source.quoted)
    }

    @Test
    fun comingBackOnline_refreshesOnlyAfterARefreshFailed() {
        var online = false
        val source = FakeSource().apply { quotes = mapOf("AAPL" to Quote.Priced(BigDecimal("160"), at)) }
        val vm = started(store(), source = source, online = { online })
        assertTrue(vm.state.value.refreshFailed)
        online = true
        vm.onOnline()
        assertEquals(listOf("AAPL"), source.quoted)
        assertFalse(vm.state.value.refreshFailed)
        assertEquals(dollars(10_880), vm.state.value.netWorth)
        vm.onOnline()
        assertEquals(listOf("AAPL"), source.quoted)
    }

    @Test
    fun anUnreadableStoreOnThePriceSaveAtStart_showsTheUnreadableScreen() {
        // 22a (open item 54, your input): this save on its own; the tests of other saves no longer reach it.
        val store = store().apply { pricesSaveError = UnreadableDataException("unreadable") }
        val vm = started(store, source = FakeSource().apply { quotes = mapOf("AAPL" to Quote.Priced(BigDecimal("160"), at)) })
        assertEquals(Screen.UNREADABLE, vm.state.value.screen)
        assertFalse(vm.state.value.refreshing)
    }

    @Test
    fun aSourceFailure_keepsTheLastPriceStale_andSaysTheRefreshFailed() {
        val source = FakeSource().apply { quotes = mapOf("AAPL" to Quote.TooManyCalls) }
        val vm = started(store(), source = source)
        assertEquals(old.copy(stale = true), vm.state.value.instruments.single { it.ticker == "AAPL" }.price)
        assertTrue(vm.state.value.refreshFailed)
    }

    @Test
    fun holdingDetail_drawsItsHeaderAndLotsFromTheRefreshedPrice() {
        val store = store().apply { storedPortfolios = listOf(Portfolio("p1", "Portfolio P", 1L)) }
        val source = FakeSource().apply { quotes = mapOf("AAPL" to Quote.Priced(BigDecimal("160"), at)) }
        val vm = started(store, source = source)
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        val state = vm.state.value
        val shown = state.shownHolding!!
        assertEquals(HoldingDetail.header(shown, "Portfolio P", Price(BigDecimal("160"), at), false, false), state.holdingHeader)
        assertEquals("$1,280.00", state.holdingHeader!!.value)
        assertEquals(HoldingDetail.lots(state.holdingLots, Price(BigDecimal("160"), at)), state.holdingLotRows)
        source.quoteGate = CompletableDeferred()
        vm.onPullToRefresh()
        assertTrue(vm.state.value.holdingHeader!!.refreshing)
    }

    @Test
    fun thePortfolioScreens_useTheRefreshedPrice_soNetWorthIsCashPlusThePortfolios() {
        val store = store().apply { storedPortfolios = listOf(Portfolio("p1", "Portfolio P", 1L)) }
        val source = FakeSource().apply { quotes = mapOf("AAPL" to Quote.Priced(BigDecimal("160"), at)) }
        val state = started(store, source = source).state.value
        val prices = mapOf("AAPL" to Price(BigDecimal("160"), at))
        assertEquals(PortfolioFigures.of(state.holdingsOf("p1"), prices), state.portfolioFigures("p1"))
        assertEquals(PortfolioFigures.rows(state.holdingsOf("p1"), prices), state.portfolioHoldingRows("p1"))
        assertEquals("$1,280.00", state.portfolioFigures("p1").value)
        // Net worth is the cash plus the portfolio's holding at that price, exactly (4.8).
        val value = Valuation.valued(state.holdingsOf("p1").single(), prices["AAPL"]).value
        assertEquals(dollars(1_280), value)
        assertEquals(state.netWorth, dollars(9_600) + value)
    }

    // 16b: the portfolio screens' age line, stale flag and refresh state follow the refresh (AC-42).
    @Test
    fun thePortfolioScreens_showTheRefreshState_andTheStalePrice() {
        val store = store().apply { storedPortfolios = listOf(Portfolio("p1", "Portfolio P", 1L)) }
        val source = FakeSource().apply { quoteGate = CompletableDeferred() }
        val vm = started(store, source = source)
        assertEquals(PriceRefreshModel(refreshing = true, failed = false), vm.state.value.priceRefresh)
        source.quoteGate!!.complete(Unit)
        // The source has no quote for AAPL: its last price stays, marked stale, and the refresh did not fail.
        val state = vm.state.value
        assertEquals(PriceRefreshModel(refreshing = false, failed = false), state.priceRefresh)
        assertEquals(PricesAsOfModel(HoldingDetail.asOf(old.time), someStale = true), state.portfolioFigures("p1").prices)
        assertEquals(HoldingDetail.asOf(old.time), state.portfolioHoldingRows("p1").single().staleAsOf)
        val offline = started(store(), source = FakeSource(), online = { false }).state.value
        assertEquals(PriceRefreshModel(refreshing = false, failed = true), offline.priceRefresh)
    }

    // 16c: Overall draws net worth, its age line and the portfolio rows from the refreshed prices (view 1, AC-41).
    @Test
    fun overall_showsNetWorthAtTheRefreshedPrice_withItsAgeLine_andThePortfolioRow() {
        val store = store().apply { storedPortfolios = listOf(Portfolio("p1", "Portfolio P", 1L)) }
        val source = FakeSource().apply { quotes = mapOf("AAPL" to Quote.Priced(BigDecimal("160"), at)) }
        val state = started(store, source = source).state.value
        val overall = state.overall
        assertEquals("$10,880.00", overall.netWorth)
        assertEquals(PricesAsOfModel(HoldingDetail.asOf(at), someStale = false), overall.prices)
        assertEquals(listOf(OverallPortfolioRowModel("p1", "Portfolio P", state.portfolioFigures("p1"))), overall.portfolios)
        assertEquals(PriceRefreshModel(refreshing = false, failed = false), overall.refresh)
    }

    // 17: Net Worth (Total) shows the same net worth as Overall, its age line, and the composition adding up to it (AC-34, AC-44).
    @Test
    fun netWorthTotal_showsOverallsNetWorth_andTheCompositionAtTheRefreshedPrice() {
        val store = store().apply { storedPortfolios = listOf(Portfolio("p1", "Portfolio P", 1L)) }
        val source = FakeSource().apply { quotes = mapOf("AAPL" to Quote.Priced(BigDecimal("160"), at)) }
        val state = started(store, source = source).state.value
        val total = state.netWorthTotal
        assertEquals(state.overall.netWorth, total.netWorth)
        assertEquals(state.overall.prices, total.prices)
        assertEquals(
            CompositionModel("$9,600.00", "9,600.00", listOf(CompositionRowModel("p1", "Portfolio P", "$1,280.00", "1,280.00"))),
            total.composition,
        )
        assertEquals(state.priceRefresh, total.refresh)
        assertEquals(More.of(), state.more)
    }

    @Test
    fun nothingRefreshes_beforeTheRecordsAreLoaded() {
        val source = FakeSource()
        val vm = AppViewModel(storeProvider = { store() }, clock = clockAt("2026-10-26T12:00:00Z"), newId = { "c1" }, priceSource = source, isOnline = { true })
        vm.onUnlocked()
        vm.onPullToRefresh()
        vm.onOnline()
        assertEquals(emptyList<String>(), source.quoted)
    }
}
