package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.DeletionCheck
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 29 step 4: deleting a portfolio in the screen state (4.7, wireframe 10d frames D to F; decisions 1 and 2; AC-28, AC-55).
@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioDeleteFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val p = Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L)
    private val q = Portfolio(id = "p2", name = "Portfolio Q", createdSeq = 2L)

    private fun bought(
        id: String,
        portfolioId: String,
        ticker: String,
        date: String,
        cents: Long,
        seq: Long,
    ) = Transaction(id, portfolioId, ticker, TransactionType.BUY, LocalDate.parse(date), seq, BigDecimal.TEN, cents)

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    // Example A in Portfolio P (AC-55): two buys and the Mar 1 sell; Portfolio Q holds MSFT.
    private val a1 = bought("a1", "p1", "AAPL", "2026-01-02", 100_000L, 2L)
    private val a2 = bought("a2", "p1", "AAPL", "2026-02-01", 150_000L, 3L)
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
    private val q1 = bought("q1", "p2", "MSFT", "2026-02-15", 30_000L, 5L)

    private fun store(
        transactions: List<Transaction> = listOf(a1, a2, aSell, q1),
        extra: List<CashEntry> = emptyList(),
        portfolios: List<Portfolio> = listOf(p, q),
    ): FakeStore {
        val store = FakeStore(stored = listOf(opening()) + transactions.map { lineOf(it) } + extra)
        store.storedPortfolios = portfolios
        store.storedTransactions = transactions
        store.storedInstruments = listOf("AAPL", "MSFT").map { Instrument(it, AssetType.STOCK, verified = true) }
        store.storedHidden = listOf(HiddenHolding("p1", "AAPL"))
        store.storedOverrides = listOf(ClassOverride("p1", "AAPL", ClassList.SECTOR, "Technology"), ClassOverride("p2", "MSFT", ClassList.SECTOR, "Technology"))
        return store
    }

    /** Started offline so no refresh runs; [id]'s screen opened from the Portfolios tab, then its menu. */
    private fun onMenu(
        store: FakeStore = store(),
        id: String = "p1",
    ): AppViewModel {
        val vm = started(store, online = { false })
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio(id)
        vm.openPortfolioMenu()
        return vm
    }

    // Wireframe 10d frame D, AC-55: Delete closes the menu and opens the dialog with Example A's summary.
    @Test
    fun delete_opensTheDialog_withTheSummary() {
        val vm = onMenu()
        vm.openDelete()
        val dialog = assertNotNullDialog(vm)
        assertFalse(vm.state.value.portfolioMenuOpen)
        assertEquals(p, dialog.summary.portfolio)
        assertEquals(3, dialog.summary.transactionCount)
        assertEquals(3, dialog.summary.tradeLineCount)
        assertEquals(Fraction.ofCents(80_000L), dialog.summary.realizedGain)
        assertEquals(930_000L, dialog.summary.balanceNowCents)
        assertEquals(970_000L, dialog.summary.balanceAfterCents)
        assertEquals("", dialog.typedName)
    }

    private fun assertNotNullDialog(vm: AppViewModel): DeleteDialog {
        val dialog = vm.state.value.deleteDialog
        assertNotNull("the Delete dialog is open", dialog)
        return dialog!!
    }

    // AC-55: Delete portfolio does nothing until the name is typed, case and spaces ignored.
    @Test
    fun deletePortfolio_needsTheTypedName() {
        val store = store()
        val vm = onMenu(store)
        vm.openDelete()
        vm.confirmDelete()
        vm.onDeleteNameChange("Portfolio")
        vm.confirmDelete()
        assertTrue(store.deletions.isEmpty())
        assertNotNull(vm.state.value.deleteDialog)
        vm.onDeleteNameChange(" portfolio  p ")
        assertEquals(" portfolio  p ", vm.state.value.deleteDialog?.typedName)
        vm.confirmDelete()
        assertEquals(1, store.deletions.size)
    }

    // 4.7 and decision 1: the portfolio, its transactions, their lines, its own hidden row and override go; Q's stay. The
    // app shows the Portfolios tab.
    @Test
    fun deletePortfolio_removesItAndWhatIsItsOwn_andShowsThePortfoliosTab() {
        val store = store()
        val vm = onMenu(store)
        vm.openDelete()
        vm.onDeleteNameChange("Portfolio P")
        vm.confirmDelete()
        val removal = store.deletions.single()
        assertEquals("p1", removal.portfolioId)
        assertEquals(setOf("a1", "a2", "as"), removal.transactionIds)
        assertEquals(setOf("c-a1", "c-a2", "c-as"), removal.cashEntryIds)
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertEquals(Tab.PORTFOLIOS, state.tab)
        assertNull(state.portfolioId)
        assertNull(state.deleteDialog)
        assertFalse(state.saving)
        assertEquals(listOf(q), state.portfolios)
        assertEquals(listOf(q1), state.transactions)
        assertEquals(setOf("o1", "c-q1"), state.entries.map { it.id }.toSet())
        assertTrue(state.hidden.isEmpty())
        assertEquals(listOf(ClassOverride("p2", "MSFT", ClassList.SECTOR, "Technology")), state.overrides)
        // The kept Cash list follows: its balance is the summary's "after", $9,700.00.
        assertEquals("$9,700.00", state.cashList.balance)
    }

    // AC-55: deleting the last portfolio leaves none ("No portfolios yet" on the Portfolios tab).
    @Test
    fun deletingTheLastPortfolio_leavesNone() {
        val store = store(transactions = emptyList(), portfolios = listOf(p))
        val vm = onMenu(store)
        vm.openDelete()
        vm.onDeleteNameChange("Portfolio P")
        vm.confirmDelete()
        assertTrue(vm.state.value.portfolios.isEmpty())
        assertEquals(Tab.PORTFOLIOS, vm.state.value.tab)
    }

    // Cancel and Back close the dialog; nothing is removed.
    @Test
    fun cancelOrBack_closesTheDialog_andRemovesNothing() {
        val store = store()
        val vm = onMenu(store)
        vm.openDelete()
        vm.onDeleteNameChange("Portfolio P")
        vm.cancelDelete()
        assertNull(vm.state.value.deleteDialog)
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
        vm.openPortfolioMenu()
        vm.openDelete()
        vm.closePortfolio()
        assertNull(vm.state.value.deleteDialog)
        assertTrue(store.deletions.isEmpty())
        assertEquals(2, vm.state.value.portfolios.size)
    }

    // Example E (AC-55): Delete opens no dialog; the blocked message names Feb 10 and $200.00; Close closes it.
    private val qBuy = bought("qb", "p2", "MSFT", "2026-01-05", 40_000L, 2L)
    private val qSell =
        Transaction(
            id = "qs",
            portfolioId = "p2",
            ticker = "MSFT",
            type = TransactionType.SELL,
            date = LocalDate.parse("2026-02-01"),
            enteredSeq = 3L,
            quantity = BigDecimal.TEN,
            totalCents = 90_000L,
            allocations = listOf(SellAllocation("qb", Fraction.of(BigDecimal.TEN))),
        )

    private fun exampleE(withdrawalDate: String = "2026-02-10"): FakeStore {
        val withdrawal = CashEntry("w", LocalDate.parse(withdrawalDate), -1_020_000L, CashType.WITHDRAWAL, "", 4L)
        return store(transactions = listOf(qBuy, qSell), extra = listOf(withdrawal))
    }

    @Test
    fun exampleE_opensNoDialog_butTheBlockedMessage_andCloseClosesIt() {
        val store = exampleE()
        val vm = onMenu(store, "p2")
        vm.openDelete()
        assertNull(vm.state.value.deleteDialog)
        assertFalse(vm.state.value.portfolioMenuOpen)
        assertEquals(DeletionCheck.Blocked(LocalDate.parse("2026-02-10"), 20_000L), vm.state.value.deleteBlocked)
        vm.closeDeleteBlocked()
        assertNull(vm.state.value.deleteBlocked)
        assertEquals(Screen.PORTFOLIO, vm.state.value.screen)
        assertTrue(store.deletions.isEmpty())
    }

    // 4.7: negative from the opening balance's date: the "cannot be deleted" message.
    @Test
    fun negativeFromTheOpeningDate_showsCannotDelete() {
        val vm = onMenu(exampleE("2026-01-01"), "p2")
        vm.openDelete()
        assertNull(vm.state.value.deleteDialog)
        assertTrue(vm.state.value.deleteBlocked is DeletionCheck.CannotDelete)
    }

    // A failed removal keeps the dialog with "not saved" and changes nothing in the state.
    @Test
    fun aFailedRemoval_keepsTheDialog_andChangesNothing() {
        val store = store()
        store.deleteError = IllegalStateException("disk")
        val vm = onMenu(store)
        vm.openDelete()
        vm.onDeleteNameChange("Portfolio P")
        vm.confirmDelete()
        val state = vm.state.value
        assertEquals(SaveRefusal.SAVE_FAILED, state.deleteDialog?.save)
        assertFalse(state.saving)
        assertEquals(Screen.PORTFOLIO, state.screen)
        assertEquals(2, state.portfolios.size)
        assertEquals(4, state.transactions.size)
    }

    // Decision 2: a Cash ticker whose last trade line went with the portfolio goes back to All tickers.
    @Test
    fun aCashTickerWithNoLineLeft_goesBackToAllTickers() {
        val vm = started(store(), online = { false })
        vm.selectTab(Tab.CASH)
        vm.openCashTickerMenu()
        vm.pickCashTicker("MSFT")
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p2")
        vm.openPortfolioMenu()
        vm.openDelete()
        vm.onDeleteNameChange("Portfolio Q")
        vm.confirmDelete()
        assertNull(vm.state.value.cashFilterForm.ticker)
    }

    // Decision 2: a Cash ticker that still has a line elsewhere stays picked.
    @Test
    fun aCashTickerWithALineLeft_staysPicked() {
        val vm = started(store(), online = { false })
        vm.selectTab(Tab.CASH)
        vm.openCashTickerMenu()
        vm.pickCashTicker("AAPL")
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p2")
        vm.openPortfolioMenu()
        vm.openDelete()
        vm.onDeleteNameChange("Portfolio Q")
        vm.confirmDelete()
        assertEquals("AAPL", vm.state.value.cashFilterForm.ticker)
    }

    // A tab closes the dialog and the message.
    @Test
    fun aTab_closesTheDialogAndTheMessage() {
        val vm = onMenu()
        vm.openDelete()
        vm.selectTab(Tab.CASH)
        assertNull(vm.state.value.deleteDialog)
        val blocked = onMenu(exampleE(), "p2")
        blocked.openDelete()
        blocked.selectTab(Tab.CASH)
        assertNull(blocked.state.value.deleteBlocked)
    }
}
