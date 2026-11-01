package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.ReversalRefusal
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReversalFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)

    private fun shares(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

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

    // Example D's buy (b1) and its sell of Jan 3 (s1); a buy that is not sold (b2); a 2 for 1 split on Apr 1 (x1).
    private val b1 = bought("b1", "2026-01-02", "10", 100_000L, 2L)
    private val s1 =
        Transaction(
            id = "s1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SELL,
            date = LocalDate.of(2026, 1, 3),
            enteredSeq = 3L,
            quantity = BigDecimal("10"),
            totalCents = 120_000L,
            allocations = listOf(SellAllocation("b1", shares(10))),
        )
    private val b2 = bought("b2", "2026-02-01", "10", 150_000L, 4L)
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

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    private var nextId = 0

    private fun store(saveError: Exception? = null): FakeStore {
        val transactions = listOf(b1, s1, b2, x1)
        val lines = transactions.filter { it.type != TransactionType.SPLIT }.map { lineOf(it) }
        val store = FakeStore(stored = listOf(opening()) + lines, saveError = saveError)
        store.storedPortfolios = listOf(main)
        store.storedTransactions = transactions
        return store
    }

    private fun onTransaction(
        id: String,
        store: FakeStore = store(),
    ): AppViewModel {
        val vm = started(store, newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        vm.openTransaction(id)
        return vm
    }

    @Test
    fun reverse_opensOnTheOriginalsDate_andTheEffectFollowsTheDate_withTheSplitsBetweenApplied() {
        val vm = onTransaction("b2")
        assertEquals(true, vm.state.value.transactionReverseOffered)
        vm.openTransactionReversal()
        assertEquals(Screen.REVERSE_TRANSACTION, vm.state.value.screen)
        assertEquals("20260201", vm.state.value.transactionReversalDate)
        assertEquals(ReversalEffect.OfBuy(shares(10), 150_000L), vm.state.value.transactionReversalEffect)
        vm.onTransactionReversalDateChange("20260501")
        assertEquals(ReversalEffect.OfBuy(shares(20), 150_000L), vm.state.value.transactionReversalEffect)
    }

    @Test
    fun confirm_savesTheReversalWithItsCashLine_andReturnsToTheOriginal_nowReversed() {
        val store = store()
        val vm = onTransaction("b2", store)
        vm.openTransactionReversal()
        vm.onTransactionReversalDateChange("20260501")
        vm.confirmTransactionReversal()
        val state = vm.state.value
        assertEquals(Screen.TRANSACTION, state.screen)
        assertEquals(b2, state.shownTransaction)
        val (reversal, line) = store.savedReversals.single()
        assertEquals(TransactionType.REVERSAL, reversal.type)
        assertEquals("b2", reversal.reversesId)
        assertEquals(150_000L, line?.amountCents)
        assertEquals(reversal, state.transactionReversal)
        assertEquals(false, state.transactionReverseOffered)
        assertEquals(Fraction.ZERO, state.shownHolding?.shares)
        assertEquals(TransactionReversalRefusals(), state.transactionReversalRefusals)
    }

    @Test
    fun exampleD_theBuysReversal_isRefused_andTheScreenStaysOpen_withTheDateKept() {
        val store = store()
        val vm = onTransaction("b1", store)
        vm.openTransactionReversal()
        vm.confirmTransactionReversal()
        val state = vm.state.value
        assertEquals(Screen.REVERSE_TRANSACTION, state.screen)
        assertEquals("20260102", state.transactionReversalDate)
        val jan3 = LocalDate.of(2026, 1, 3)
        assertEquals(ReversalRefusal.SellNotReversed(jan3, shares(-10), jan3), state.transactionReversalRefusals.rule)
        assertEquals(emptyList<Pair<Transaction, CashEntry?>>(), store.savedReversals)
    }

    @Test
    fun theDate_takesDigitsOnly_aDateThatIsNoDate_andOneBeforeTheOriginal_areRefused() {
        val vm = onTransaction("b2")
        vm.openTransactionReversal()
        vm.onTransactionReversalDateChange("2026-")
        assertEquals(DateRefusal.NOT_DIGITS, vm.state.value.transactionReversalRefusals.date)
        assertNull(vm.state.value.transactionReversalEffect)
        vm.onTransactionReversalDateChange("20261301")
        vm.confirmTransactionReversal()
        assertEquals(DateRefusal.NOT_A_DATE, vm.state.value.transactionReversalRefusals.date)
        vm.onTransactionReversalDateChange("20260131")
        vm.confirmTransactionReversal()
        assertEquals(ReversalRefusal.BeforeOriginal(LocalDate.of(2026, 2, 1)), vm.state.value.transactionReversalRefusals.rule)
        assertEquals(Screen.REVERSE_TRANSACTION, vm.state.value.screen)
    }

    @Test
    fun aSellsReversal_linksBothWays_cannotItselfBeReversed_andBackGoesOneStepAtATime() {
        val store = store()
        val vm = onTransaction("s1", store)
        vm.openTransactionReversal()
        vm.confirmTransactionReversal()
        assertEquals(-120_000L, store.savedReversals.single().second?.amountCents)
        val reversal = vm.state.value.transactionReversal ?: error("not reversed")
        vm.openTransactionLink()
        assertEquals(reversal, vm.state.value.shownTransaction)
        assertEquals(s1, vm.state.value.transactionOriginal)
        assertEquals(false, vm.state.value.transactionReverseOffered)
        vm.openTransactionReversal()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        vm.closeTransaction()
        assertEquals(s1, vm.state.value.shownTransaction)
        vm.closeTransaction()
        assertEquals(Screen.HOLDING, vm.state.value.screen)
    }

    @Test
    fun aSplitsReversal_hasNoCashLine_andPutsTheLotsBack() {
        val store = store()
        val vm = onTransaction("x1", store)
        vm.openTransactionReversal()
        assertEquals(ReversalEffect.OfSplit(SplitRatio(1, 2), shares(20), shares(10)), vm.state.value.transactionReversalEffect)
        vm.confirmTransactionReversal()
        assertNull(store.savedReversals.single().second)
        assertEquals(shares(10), vm.state.value.shownHolding?.shares)
    }

    @Test
    fun cancel_returnsToTheDetail_aBottomTabLeaves_andAFailedSave_staysWithNothingSaved() {
        val vm = onTransaction("b2")
        vm.openTransactionReversal()
        vm.cancelTransactionReversal()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        vm.openTransactionReversal()
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertEquals(emptyList<String>(), vm.state.value.transactionTrail)
        val failing = store(saveError = IllegalStateException("disk"))
        val vm2 = onTransaction("b2", failing)
        vm2.openTransactionReversal()
        vm2.confirmTransactionReversal()
        assertEquals(Screen.REVERSE_TRANSACTION, vm2.state.value.screen)
        assertEquals(SaveRefusal.SAVE_FAILED, vm2.state.value.transactionReversalRefusals.save)
        assertEquals(emptyList<Pair<Transaction, CashEntry?>>(), failing.savedReversals)
    }
}
