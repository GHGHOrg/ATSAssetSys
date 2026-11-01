package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SplitEffect
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailFlowTest {
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
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = TransactionType.BUY,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
    )

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    // Example A: lot 1 (b1, 10 for $1,000.00), lot 2 (b2, 10 for $1,500.00), the Mar 1 sell of 12 for $2,100.00 (s1).
    private val b1 = bought("b1", "2026-01-02", "10", 100_000L, 2L)
    private val b2 = bought("b2", "2026-02-01", "10", 150_000L, 3L)
    private val s1 =
        Transaction(
            id = "s1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SELL,
            date = LocalDate.of(2026, 3, 1),
            enteredSeq = 4L,
            quantity = BigDecimal("12"),
            totalCents = 210_000L,
            allocations = listOf(SellAllocation("b1", dollars(10)), SellAllocation("b2", dollars(2))),
        )

    // Then a 2 for 1 split on Apr 1 (lot 2 becomes 16 at $75.00), and a buy dated after today (2026-10-26).
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
    private val future = bought("b3", "2026-12-01", "1", 10_000L, 6L)

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    private fun onHolding(): AppViewModel {
        val transactions = listOf(b1, b2, s1, x1, future)
        val lines = transactions.filter { it.type != TransactionType.SPLIT }.map { lineOf(it) }
        val store = FakeStore(stored = listOf(opening()) + lines)
        store.storedPortfolios = listOf(main)
        store.storedTransactions = transactions
        val vm = started(store)
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        return vm
    }

    @Test
    fun aTransactionsRow_opensItsTransaction_andBackReturnsToTheHolding() {
        val vm = onHolding()
        vm.openTransaction("b2")
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals(b2, vm.state.value.shownTransaction)
        assertEquals("c-b2", vm.state.value.transactionCashLine?.id)
        vm.closeTransaction()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
        assertEquals("AAPL", vm.state.value.holdingTicker)
        assertNull(vm.state.value.transactionId)
    }

    @Test
    fun aBuy_showsWhatIsLeftOfItsLot_asOfToday_splitAdjusted_atZeroOnceSoldOut() {
        val vm = onHolding()
        vm.openTransaction("b2")
        val lot = vm.state.value.transactionLot
        assertEquals(dollars(16), lot?.remainingQuantity)
        assertEquals(Fraction.ofCents(7_500L), lot?.costPerShare)
        vm.closeTransaction()
        vm.openTransaction("b1")
        assertEquals(Fraction.ZERO, vm.state.value.transactionLot?.remainingQuantity)
    }

    @Test
    fun aBuyDatedAfterToday_hasNoLotYet() {
        val vm = onHolding()
        vm.openTransaction("b3")
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertNull(vm.state.value.transactionLot)
    }

    @Test
    fun aSell_showsTheLotsItUsed_andItsRealizedGain() {
        val vm = onHolding()
        vm.openTransaction("s1")
        val pieces = vm.state.value.transactionPieces
        assertEquals(listOf("b1", "b2"), pieces.map { it.lotId })
        assertEquals(listOf(dollars(1_000), dollars(300)), pieces.map { it.cost })
        assertEquals(listOf(dollars(1_750), dollars(350)), pieces.map { it.proceeds })
        assertEquals(dollars(800), pieces.fold(Fraction.ZERO) { sum, it -> sum + it.gain })
        assertEquals("c-s1", vm.state.value.transactionCashLine?.id)
    }

    @Test
    fun aSplit_showsTheSharesBeforeAndAfter_andHasNoCashLine() {
        val vm = onHolding()
        vm.openTransaction("x1")
        assertEquals(SplitEffect(dollars(8), dollars(16)), vm.state.value.transactionSplitEffect)
        assertNull(vm.state.value.transactionCashLine)
        vm.openCashLine()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
    }

    @Test
    fun aTradeLine_opensItsTransaction_andBackReturnsToTheCashTab() {
        val vm = onHolding()
        vm.selectTab(Tab.CASH)
        vm.openTransaction("s1")
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        vm.closeTransaction()
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(Tab.CASH, vm.state.value.tab)
    }

    @Test
    fun theCashLink_opensTheTradeLine_whoseLinkAndBack_returnToTheTransaction_withoutALoop() {
        val vm = onHolding()
        vm.openTransaction("b2")
        vm.openCashLine()
        assertEquals(Screen.DETAIL, vm.state.value.screen)
        assertEquals("c-b2", vm.state.value.detailId)
        assertEquals(b2, vm.state.value.detailTradeTransaction)
        vm.openLinkedTransaction()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals(b2, vm.state.value.shownTransaction)
        vm.openCashLine()
        vm.closeDetail()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        vm.closeTransaction()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
    }

    @Test
    fun anUnknownId_isIgnored_andABottomTab_leavesTheDetail() {
        val vm = onHolding()
        vm.openTransaction("zz")
        assertEquals(Screen.HOLDING, vm.state.value.screen)
        vm.openTransaction("b2")
        vm.openCashLine()
        vm.selectTab(Tab.OVERALL)
        val state = vm.state.value
        assertEquals(Screen.TABS, state.screen)
        assertNull(state.transactionId)
        assertEquals(false, state.detailFromTransaction)
        vm.selectTab(Tab.CASH)
        vm.openDetail("c-b2")
        vm.closeDetail()
        assertEquals(Screen.TABS, vm.state.value.screen)
    }
}
