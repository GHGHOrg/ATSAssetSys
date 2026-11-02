package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.BuyRefusal
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Listing
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.ReentryRefusal
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.ReversalRefusal
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Sells
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.SplitRefusal
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.prices.CheckRefusal
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
import java.math.BigInteger
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReenterFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)
    private val aaplVerified = Instrument("AAPL", AssetType.STOCK, verified = true)

    private fun shares(
        numerator: Long,
        denominator: Long = 1L,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

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
        totalCents: Long,
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
        totalCents = totalCents,
        allocations = listOf(SellAllocation(lotId, Fraction.of(BigDecimal(quantity)))),
    )

    // Example D's buy (b1) and its sell of Jan 3 (s1); a buy that is not sold (b2); a 2 for 1 split on Apr 1 (x1).
    private val b1 = bought("b1", "2026-01-02", "10", 100_000L, 2L)
    private val s1 = sold("s1", "2026-01-03", "10", 120_000L, 3L, "b1")
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

    // The reversal of s1, dated Jan 3, for Example G's second half.
    private val r1 =
        Transaction(
            id = "r1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.REVERSAL,
            date = LocalDate.of(2026, 1, 3),
            enteredSeq = 6L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            reversesId = "s1",
        )

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        when (transaction.type) {
            TransactionType.BUY -> -transaction.totalCents
            TransactionType.SELL -> transaction.totalCents
            else -> -120_000L
        },
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    private var nextId = 0

    private fun store(
        transactions: List<Transaction> = listOf(b1, s1, b2, x1),
        saveError: Exception? = null,
        instruments: List<Instrument> = listOf(aaplVerified),
    ): FakeStore {
        val lines = transactions.filter { it.type != TransactionType.SPLIT }.map { lineOf(it) }
        val store = FakeStore(stored = listOf(opening()) + lines, saveError = saveError)
        store.storedPortfolios = listOf(main)
        store.storedTransactions = transactions
        store.storedInstruments = instruments
        return store
    }

    private fun onTransaction(
        id: String,
        store: FakeStore = store(),
        ticker: String = "AAPL",
        source: FakeSource = FakeSource(),
        online: Boolean = true,
    ): AppViewModel {
        val vm = started(store, source = source, online = { online }, newId = { "id${++nextId}" })
        vm.openPortfolio("p1")
        vm.openHolding(ticker)
        vm.openTransaction(id)
        return vm
    }

    // 14b: a split is offered too (it was not in 14a).
    @Test
    fun reverseAndReenter_isOfferedOnABuySellOrSplit_notOnAReversal_orAReversedEntry() {
        val vm = onTransaction("b2")
        assertTrue(vm.state.value.transactionReenterOffered)
        val onSplit = onTransaction("x1")
        assertTrue(onSplit.state.value.transactionReenterOffered)
        onSplit.openReenter()
        assertEquals(Screen.SPLIT, onSplit.state.value.screen)
        val withReversal = onTransaction("s1", store(listOf(b1, s1, b2, x1, r1)))
        assertFalse(withReversal.state.value.transactionReenterOffered)
        withReversal.openTransactionLink()
        assertFalse(withReversal.state.value.transactionReenterOffered)
    }

    // --- 14b: a split ---

    // b2 (10 on Feb 1) and the 2 for 1 of Apr 1 (x1): 20 shares; s5 sells 20 on Apr 2.
    private val s5 =
        Transaction(
            id = "s5",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SELL,
            date = LocalDate.of(2026, 4, 2),
            enteredSeq = 6L,
            quantity = BigDecimal("20"),
            totalCents = 300_000L,
            allocations = listOf(SellAllocation("b2", shares(20))),
        )

    @Test
    fun onASplit_theSplitScreenOpensPrefilled_andBackLeavesNothingSaved() {
        val store = store(listOf(b2, x1))
        val vm = onTransaction("x1", store)
        vm.openReenter()
        val state = vm.state.value
        assertEquals(Screen.SPLIT, state.screen)
        assertEquals(SplitForm(portfolioId = "p1", ticker = "AAPL", dateText = "20260401", toText = "2", fromText = "1"), state.splitForm)
        assertEquals("x1", state.replacingId)
        vm.closeSplit()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals(x1, vm.state.value.shownTransaction)
        assertNull(vm.state.value.replacingId)
        assertEquals(SplitForm(), vm.state.value.splitForm)
        assertTrue(store.savedSplitPairs.isEmpty())
    }

    @Test
    fun aTwoForOneReenteredAsThreeForOne_showsBothParts_andConfirmSavesThePair_andReturnsToTheOriginal() {
        val store = store(listOf(b2, x1))
        val vm = onTransaction("x1", store)
        vm.openReenter()
        vm.onSplitToChange("3")
        vm.reviewSplit()
        val review = vm.state.value
        assertEquals(Screen.REPLACE_REVIEW, review.screen)
        assertEquals(ReversalEffect.OfSplit(SplitRatio(1, 2), shares(20), shares(10)), review.replaceReversalEffect)
        assertEquals(SplitRatio(3, 1), review.replaceSplitReview?.replacement?.split)
        assertNull(review.replaceCombinedCents)
        vm.confirmReplace()
        val state = vm.state.value
        assertEquals(Screen.TRANSACTION, state.screen)
        assertEquals(x1, state.shownTransaction)
        assertEquals("x1", state.transactionReversal?.reversesId)
        assertEquals(1, store.savedSplitPairs.size)
        assertEquals(shares(30), state.holdings.single().shares)
        assertEquals(SplitForm(), state.splitForm)
    }

    @Test
    fun aSplitPairThatLeavesALaterSellShort_keepsTheSplitScreen_withTheNewSplitsRule() {
        val store = store(listOf(b2, x1, s5))
        val vm = onTransaction("x1", store)
        vm.openReenter()
        vm.onSplitToChange("3")
        vm.onSplitFromChange("2")
        vm.reviewSplit()
        assertEquals(Screen.SPLIT, vm.state.value.screen)
        assertEquals(SplitRefusal.SellShort(LocalDate.of(2026, 4, 2)), vm.state.value.splitRefusals.rule)
        assertTrue(store.savedSplitPairs.isEmpty())
    }

    // --- 14b: a cash entry ---

    private val deposit500 = CashEntry("d1", LocalDate.of(2026, 4, 20), 50_000L, CashType.DEPOSIT, "Salary", 2L)
    private val reversalNote = "Reversal of the deposit of 2026-04-20 ET"

    private fun cashStore(
        vararg more: CashEntry,
        saveError: Exception? = null,
    ): FakeStore {
        val store = FakeStore(stored = listOf(opening(), deposit500) + more, saveError = saveError)
        store.storedPortfolios = listOf(main)
        return store
    }

    private fun onCashEntry(
        id: String,
        store: FakeStore,
    ): AppViewModel {
        val vm = started(store, newId = { "id${++nextId}" })
        vm.selectTab(Tab.CASH)
        vm.openDetail(id)
        return vm
    }

    @Test
    fun reverseAndReenter_isOfferedOnADirectCashEntry_notOnTheOpeningBalance_aTradeLine_aReversal_orAReversedEntry() {
        val trade = CashEntry("t1", LocalDate.of(2026, 4, 21), -10_000L, CashType.TRADE, "", 3L)
        val adjustment = CashEntry("a1", LocalDate.of(2026, 4, 22), 5_000L, CashType.ADJUSTMENT, "fix", 4L, reversesId = "d9")
        val store = cashStore(trade, adjustment, CashEntry("d9", LocalDate.of(2026, 4, 22), -5_000L, CashType.WITHDRAWAL, "", 5L))
        assertTrue(onCashEntry("d1", store).state.value.detailReenterOffered)
        assertFalse(onCashEntry("o1", store).state.value.detailReenterOffered)
        assertFalse(onCashEntry("t1", store).state.value.detailReenterOffered)
        assertFalse(onCashEntry("a1", store).state.value.detailReenterOffered)
        val reversed = onCashEntry("d9", store)
        assertFalse(reversed.state.value.detailReenterOffered)
        reversed.openReenterCash(reversalNote)
        assertEquals(Screen.DETAIL, reversed.state.value.screen)
    }

    @Test
    fun onACashEntry_theFormOpensPrefilled_andBackReturnsToTheDetail_withNothingSaved() {
        val store = cashStore()
        val vm = onCashEntry("d1", store)
        vm.openReenterCash(reversalNote)
        val state = vm.state.value
        assertEquals(Screen.FORM, state.screen)
        assertEquals(FormState(dateText = "2026-04-20", amountText = "500.00", noteText = "Salary", type = CashType.DEPOSIT), state.form)
        assertEquals("d1", state.replacingCashId)
        assertEquals(deposit500, state.replacedCashEntry)
        assertEquals("20260420", state.replaceReversalDate)
        vm.closeForm()
        assertEquals(Screen.DETAIL, vm.state.value.screen)
        assertEquals(deposit500, vm.state.value.detailEntry)
        assertNull(vm.state.value.replacingCashId)
        assertEquals(FormState(), vm.state.value.form)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun frameE_aDepositOf500ReenteredAs50_showsTheAdjustmentAndTheNewDeposit_combinedMinus450_andSavesBothInOneBatch() {
        val store = cashStore()
        val vm = onCashEntry("d1", store)
        vm.openReenterCash(reversalNote)
        vm.onAmountChange("50.00")
        vm.review()
        val review = vm.state.value
        assertEquals(Screen.REPLACE_REVIEW, review.screen)
        val pair = review.replaceCashReview ?: error("no pair")
        assertEquals(CashType.ADJUSTMENT, pair.reversal.type)
        assertEquals(-50_000L, pair.reversal.amountCents)
        assertEquals(reversalNote, pair.reversal.note)
        assertEquals(5_000L, pair.replacement.amountCents)
        assertEquals(-45_000L, review.replaceCombinedCents)
        vm.confirmReplace()
        val state = vm.state.value
        assertEquals(Screen.DETAIL, state.screen)
        assertEquals(deposit500, state.detailEntry)
        val batch = store.saved.single()
        assertEquals(2, batch.size)
        assertEquals("d1", batch.first().reversesId)
        assertEquals(batch.first(), state.detailReversedBy)
        assertEquals(1_005_000L, state.cashBalanceCents)
        assertEquals(FormState(), state.form)
    }

    // 14b phone check 3: part 2 showed the stand-in note "check" instead of the typed one.
    @Test
    fun theCombinedReview_showsTheTypedNote_andConfirmSavesTheSameNote() {
        val store = cashStore()
        val vm = onCashEntry("d1", store)
        vm.openReenterCash(reversalNote)
        vm.onAmountChange("50.00")
        vm.review()
        assertEquals("Salary", vm.state.value.replaceCashReview?.replacement?.note)
        vm.confirmReplace()
        assertEquals("Salary", store.saved.single().last().note)
    }

    @Test
    fun theCombinedReview_showsAnEmptyNoteAsEmpty() {
        val vm = onCashEntry("d1", cashStore())
        vm.openReenterCash(reversalNote)
        vm.onNoteChange("")
        vm.review()
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        assertEquals("", vm.state.value.replaceCashReview?.replacement?.note)
    }

    @Test
    fun anAdjustmentWithNoNote_stillShowsTheNoteRefusal_togetherWithTheCashShortfall() {
        val vm = onCashEntry("d1", cashStore())
        vm.openReenterCash(reversalNote)
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.onDirectionChange(CashDirection.OUT)
        vm.onNoteChange("")
        // $10,000 + $500 − $500 back − $10,000.01 = −$0.01 on Apr 20.
        vm.onAmountChange("10000.01")
        vm.review()
        assertEquals(Screen.FORM, vm.state.value.screen)
        assertTrue(vm.state.value.refusals.noteRequired)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 4, 20), 1L), vm.state.value.refusals.cash)
    }

    @Test
    fun bothOrNeither_aCashPairThatTakesCashBelowZero_keepsTheForm_withTheNewEntrysCashRule() {
        val store = cashStore()
        val vm = onCashEntry("d1", store)
        vm.openReenterCash(reversalNote)
        vm.onTypeChange(CashType.WITHDRAWAL)
        // $10,000 + $500 − $500 back − $10,000.01 = −$0.01 on Apr 20.
        vm.onAmountChange("10000.01")
        vm.review()
        assertEquals(Screen.FORM, vm.state.value.screen)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 4, 20), 1L), vm.state.value.refusals.cash)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun onACashPair_theFieldRulesShowTogether_andAReversalDateBeforeTheOriginalIsRefusedOnTheReview() {
        val vm = onCashEntry("d1", cashStore())
        vm.openReenterCash(reversalNote)
        vm.onTypeChange(CashType.ADJUSTMENT)
        vm.onNoteChange(" ")
        vm.onAmountChange("abc")
        vm.review()
        assertEquals(Screen.FORM, vm.state.value.screen)
        assertTrue(vm.state.value.refusals.noteRequired)
        assertEquals(AmountError.BAD_FORMAT, vm.state.value.refusals.amount)
        vm.onNoteChange("fix")
        vm.onAmountChange("50.00")
        vm.review()
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        vm.changeReplaceDate()
        vm.onReplaceDateChange("20260419")
        assertEquals(
            ReentryRefusal.OfCashReversal(CashEntryRefusal.BeforeOriginal(LocalDate.of(2026, 4, 20))),
            vm.state.value.replaceRefusals.rule,
        )
    }

    @Test
    fun aFailedCashPairSave_staysOnTheReview_withNothingSaved_andABottomTabLeaves() {
        val failing = cashStore(saveError = IllegalStateException("disk"))
        val vm = onCashEntry("d1", failing)
        vm.openReenterCash(reversalNote)
        vm.review()
        vm.confirmReplace()
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        assertEquals(SaveRefusal.SAVE_FAILED, vm.state.value.replaceRefusals.save)
        assertTrue(failing.saved.isEmpty())
        vm.selectTab(Tab.OVERALL)
        assertNull(vm.state.value.replacingCashId)
        assertEquals(FormState(), vm.state.value.form)
    }

    @Test
    fun onABuy_theBuyScreenOpensPrefilled_keepsTheOriginalForItsHints_andBackLeavesNothingSaved() {
        val store = store()
        val vm = onTransaction("b2", store)
        vm.openReenter()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        assertEquals(BuyForm(portfolioId = "p1", tickerText = "AAPL", dateText = "20260201", quantityText = "10", totalText = "1500.00"), state.buyForm)
        assertEquals("b2", state.replacingId)
        assertEquals("20260201", state.replaceReversalDate)
        vm.onBuyQuantityChange("100")
        assertEquals(b2, vm.state.value.replacedTransaction)
        vm.closeBuy()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals(b2, vm.state.value.shownTransaction)
        assertNull(vm.state.value.replacingId)
        assertEquals(BuyForm(), vm.state.value.buyForm)
        assertTrue(store.savedPairs.isEmpty())
    }

    @Test
    fun exampleG_aBuyReenteredWithMoreShares_showsTheCombinedReview_andConfirmSavesBoth_andReturnsToTheOriginal() {
        val store = store()
        val vm = onTransaction("b2", store)
        vm.openReenter()
        vm.onBuyQuantityChange("100")
        vm.reviewBuy()
        val review = vm.state.value
        assertEquals(Screen.REPLACE_REVIEW, review.screen)
        assertEquals(ReversalEffect.OfBuy(shares(10), 150_000L), review.replaceReversalEffect)
        assertEquals(BigDecimal("100"), review.replaceReview?.replacement?.quantity)
        assertEquals(0L, review.replaceCombinedCents)
        vm.confirmReplace()
        val state = vm.state.value
        assertEquals(Screen.TRANSACTION, state.screen)
        assertEquals(b2, state.shownTransaction)
        assertEquals("b2", state.transactionReversal?.reversesId)
        assertEquals(1, store.savedPairs.size)
        // The new lot of 100 bought on Feb 1, doubled by the Apr 1 split; b1's shares were sold.
        assertEquals(shares(200), state.shownHolding?.shares)
        assertNull(state.replacingId)
        assertEquals(BuyForm(), state.buyForm)
    }

    @Test
    fun bothOrNeither_aTotalThatTakesCashBelowZero_keepsTheBuyScreen_withTheNewBuysCashRule_andSavesNothing() {
        val store = store()
        val vm = onTransaction("b2", store)
        vm.openReenter()
        vm.onBuyTotalChange("10500.00")
        vm.reviewBuy()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        // Cash $8,700, plus $1,500 back from the reversal, less $10,500: −$300 on Feb 1.
        assertEquals(BuyRefusal.NegativeCash(LocalDate.of(2026, 2, 1), 30_000L), state.buyRefusals.rule)
        assertEquals("10500.00", state.buyForm.totalText)
        assertTrue(store.savedPairs.isEmpty())
    }

    @Test
    fun blockedByTheOriginal_whileTheSellStands_keepsTheBuyScreen_withReverseTheSellFirst() {
        val vm = onTransaction("b1")
        vm.openReenter()
        vm.onBuyQuantityChange("100")
        vm.reviewBuy()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        val jan3 = LocalDate.of(2026, 1, 3)
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.SellNotReversed(jan3, shares(-10), jan3)), state.replaceRefusals.rule)
    }

    @Test
    fun afterTheSellsReversal_theReversalDateRefusalOpensTheReview_confirmRefusesUntilChange_thenSavesWithTheNewBuyOnJan2() {
        val store = store(listOf(b1, s1, b2, x1, r1))
        val vm = onTransaction("b1", store)
        vm.openReenter()
        vm.onBuyQuantityChange("100")
        vm.reviewBuy()
        val jan3 = LocalDate.of(2026, 1, 3)
        val tooEarly = ReentryRefusal.OfReversal(ReversalRefusal.TooEarly(jan3, shares(-10), LocalDate.of(2026, 1, 2), jan3))
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        assertEquals(tooEarly, vm.state.value.replaceRefusals.rule)
        vm.confirmReplace()
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        assertTrue(store.savedPairs.isEmpty())
        vm.changeReplaceDate()
        assertTrue(vm.state.value.replaceDateEditing)
        vm.onReplaceDateChange("20260103")
        assertNull(vm.state.value.replaceRefusals.rule)
        vm.confirmReplace()
        val (reversal, _, replacement, _) = store.savedPairs.single()
        assertEquals(jan3, (reversal as Transaction).date)
        assertEquals(LocalDate.of(2026, 1, 2), (replacement as Transaction).date)
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals(b1, vm.state.value.shownTransaction)
    }

    // 14a risk 2: the review opens only when the pair passes at the date that works, so a second rule is not hidden.
    @Test
    fun aReversalDateRefusal_thatHidesASecondRule_keepsTheBuyScreen_withThatRule() {
        val vm = onTransaction("b1", store(listOf(b1, s1, b2, x1, r1)))
        vm.openReenter()
        vm.onBuyQuantityChange("100")
        vm.onBuyTotalChange("20000.00")
        vm.reviewBuy()
        val state = vm.state.value
        assertEquals(Screen.BUY, state.screen)
        // Opening $10,000, less b1's $1,000 and the new $20,000, both on Jan 2: −$11,000.
        assertEquals(BuyRefusal.NegativeCash(LocalDate.of(2026, 1, 2), 1_100_000L), state.buyRefusals.rule)
        assertNull(state.replaceRefusals.rule)
    }

    @Test
    fun onTheReview_changeTakesDigitsOnly_theEffectFollowsTheDate_andAnEarlierDateIsRefusedThere() {
        val vm = onTransaction("b2")
        vm.openReenter()
        vm.reviewBuy()
        vm.changeReplaceDate()
        vm.onReplaceDateChange("2026-")
        assertEquals(DateRefusal.NOT_DIGITS, vm.state.value.replaceRefusals.date)
        assertNull(vm.state.value.replaceReversalEffect)
        vm.onReplaceDateChange("20260501")
        assertEquals(ReversalEffect.OfBuy(shares(20), 150_000L), vm.state.value.replaceReversalEffect)
        vm.onReplaceDateChange("20260131")
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.BeforeOriginal(LocalDate.of(2026, 2, 1))), vm.state.value.replaceRefusals.rule)
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
    }

    // 14a Deploy finding 1: an unfinished reversal date must never leave Confirm without a visible reason.
    @Test
    fun anUnfinishedReversalDate_keptThroughBackToEdit_opensTheReviewWithTheDateField_andConfirmShowsWhy() {
        val store = store()
        val vm = onTransaction("b2", store)
        vm.openReenter()
        vm.reviewBuy()
        vm.changeReplaceDate()
        vm.onReplaceDateChange("2026")
        vm.backToReplaceEntry()
        vm.reviewBuy()
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        assertTrue(vm.state.value.replaceDateEditing)
        vm.confirmReplace()
        assertEquals(DateRefusal.NOT_A_DATE, vm.state.value.replaceRefusals.date)
        assertTrue(vm.state.value.replaceDateEditing)
        assertTrue(store.savedPairs.isEmpty())
    }

    @Test
    fun onASell_theSellScreenStartsFromTheOriginalPicks_withTheLotsAsAfterTheReversal_andShowsTheNewGain() {
        val store = store()
        val vm = onTransaction("s1", store)
        vm.openReenter()
        val state = vm.state.value
        assertEquals(Screen.SELL, state.screen)
        assertEquals(
            SellForm(portfolioId = "p1", ticker = "AAPL", dateText = "20260103", quantityText = "10", totalText = "1200.00", picks = mapOf("b1" to "10")),
            state.sellForm,
        )
        assertEquals(mapOf("b1" to "10"), state.replaceStartingPicks)
        assertEquals(shares(10), state.sellLotChoices.first { it.buyId == "b1" }.available)
        vm.onSellTotalChange("1300.00")
        vm.reviewSell()
        val review = vm.state.value
        assertEquals(Screen.REPLACE_REVIEW, review.screen)
        assertEquals(shares(300), Sells.realizedGain(review.replacePieces))
        assertEquals(10_000L, review.replaceCombinedCents)
        vm.confirmReplace()
        assertEquals(Screen.TRANSACTION, vm.state.value.screen)
        assertEquals(s1, vm.state.value.shownTransaction)
        assertEquals(1, store.savedPairs.size)
        assertEquals(SellForm(), vm.state.value.sellForm)
    }

    // 14a fix (phone check 2): as on the phone's 2026-10-31, a 2 for 1 entered after the trades that day. Re-entered with
    // nothing changed, the sell and the buy start with the split applied, pass Review, and leave the holding as it was.
    private val b10 = bought("b10", "2026-01-05", "10", 150_000L, 2L)
    private val b11 = bought("b11", "2026-01-05", "1", 15_000L, 3L)
    private val s13 = sold("s13", "2026-01-05", "3", 48_000L, 4L, "b10")
    private val x14 =
        Transaction(
            id = "x14",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SPLIT,
            date = LocalDate.of(2026, 1, 5),
            enteredSeq = 5L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            split = SplitRatio(2, 1),
        )

    @Test
    fun aSellWithASplitEnteredAfterItThatDay_startsWithTheSplitApplied_andUnchangedLeavesTheHoldingAsItWas() {
        val store = store(listOf(b10, b11, s13, x14))
        val vm = onTransaction("s13", store)
        assertEquals(shares(16), vm.state.value.shownHolding?.shares)
        vm.openReenter()
        assertEquals("6", vm.state.value.sellForm.quantityText)
        assertEquals(mapOf("b10" to "6"), vm.state.value.sellForm.picks)
        vm.reviewSell()
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        vm.confirmReplace()
        assertEquals(1, store.savedPairs.size)
        assertEquals(shares(16), vm.state.value.shownHolding?.shares)
    }

    @Test
    fun aBuyWithASplitEnteredAfterItThatDay_startsWithTheSplitApplied_andUnchangedLeavesTheHoldingAsItWas() {
        val store = store(listOf(b10, b11, s13, x14))
        val vm = onTransaction("b11", store)
        vm.openReenter()
        assertEquals("2", vm.state.value.buyForm.quantityText)
        vm.reviewBuy()
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        vm.confirmReplace()
        assertEquals(1, store.savedPairs.size)
        assertEquals(shares(16), vm.state.value.shownHolding?.shares)
    }

    @Test
    fun criterion60b_aSellAtZero_reenteredAt50Offline_needsNoTickerCheck_andTheHoldingStaysAtZero() {
        val oldco = bought("b9", "2026-01-05", "10", 20_000L, 2L, ticker = "OLDCO")
        val sale = sold("s9", "2026-03-10", "10", 0L, 3L, "b9", ticker = "OLDCO")
        val source = FakeSource(answer = Lookup.Found(Listing("Common Stock", "XNAS", "USD")))
        val store = store(listOf(oldco, sale), instruments = emptyList())
        val vm = onTransaction("s9", store, ticker = "OLDCO", source = source, online = false)
        vm.openReenter()
        assertTrue("OLDCO" in vm.state.value.sellTickers)
        vm.onSellTotalChange("50.00")
        vm.reviewSell()
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        assertEquals(shares(-150), Sells.realizedGain(vm.state.value.replacePieces))
        vm.confirmReplace()
        assertEquals(emptyList<String>(), source.asked)
        assertEquals(1, store.savedPairs.size)
        assertEquals(Fraction.ZERO, vm.state.value.shownHolding?.shares)
    }

    @Test
    fun theNewBuy_runsTheTickerCheck_onlineWithALookup_andOfflineIsRefusedWithoutOne() {
        val source = FakeSource(answer = Lookup.Found(Listing("Common Stock", "XNAS", "USD")))
        val vm = onTransaction("b2", store(instruments = emptyList()), source = source)
        vm.openReenter()
        vm.reviewBuy()
        assertEquals(listOf("AAPL"), source.asked)
        assertEquals(Screen.REPLACE_REVIEW, vm.state.value.screen)
        val offlineSource = FakeSource(answer = Lookup.Found(Listing("Common Stock", "XNAS", "USD")))
        val offline = onTransaction("b2", store(instruments = emptyList()), source = offlineSource, online = false)
        offline.openReenter()
        offline.reviewBuy()
        assertEquals(Screen.BUY, offline.state.value.screen)
        assertEquals(CheckRefusal.Offline, offline.state.value.buyRefusals.check)
        assertEquals(emptyList<String>(), offlineSource.asked)
    }

    @Test
    fun backToEdit_keepsTheValuesAndTheReversalDate_aBottomTabLeaves_andAFailedSave_staysWithNothingSaved() {
        val vm = onTransaction("b2", store(listOf(b1, s1, b2, x1)))
        vm.openReenter()
        vm.onBuyQuantityChange("100")
        vm.reviewBuy()
        vm.changeReplaceDate()
        vm.onReplaceDateChange("20260301")
        vm.backToReplaceEntry()
        assertEquals(Screen.BUY, vm.state.value.screen)
        assertEquals("100", vm.state.value.buyForm.quantityText)
        assertEquals("20260301", vm.state.value.replaceReversalDate)
        vm.selectTab(Tab.CASH)
        assertEquals(Screen.TABS, vm.state.value.screen)
        assertNull(vm.state.value.replacingId)
        assertEquals(BuyForm(), vm.state.value.buyForm)
        val failing = store(saveError = IllegalStateException("disk"))
        val vm2 = onTransaction("b2", failing)
        vm2.openReenter()
        vm2.reviewBuy()
        vm2.confirmReplace()
        assertEquals(Screen.REPLACE_REVIEW, vm2.state.value.screen)
        assertEquals(SaveRefusal.SAVE_FAILED, vm2.state.value.replaceRefusals.save)
        assertTrue(failing.savedPairs.isEmpty())
    }
}
