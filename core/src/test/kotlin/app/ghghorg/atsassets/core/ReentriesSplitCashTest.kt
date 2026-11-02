package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

// 14b: Reverse and re-enter of a split or a direct cash entry (spec 4.6; wireframe 10c frame E, marker 11).
class ReentriesSplitCashTest {
    private val portfolioP = Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L)

    private fun opening(cents: Long = 1_000_000L): CashEntry = CashEntry("e1", LocalDate.of(2026, 1, 1), cents, CashType.OPENING_BALANCE, "", 1L)

    private fun cash(
        id: String,
        type: CashType,
        date: String,
        cents: Long,
        seq: Long,
        note: String = "",
    ): CashEntry = CashEntry(id, LocalDate.parse(date), cents, type, note, seq)

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

    private fun sold(
        id: String,
        date: String,
        quantity: Fraction,
        totalCents: Long,
        seq: Long,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = TransactionType.SELL,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = Sells.shown(quantity),
        totalCents = totalCents,
        allocations = listOf(SellAllocation("b1", quantity)),
    )

    private fun splitOn(
        id: String,
        date: String,
        to: Int,
        from: Int,
        seq: Long,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = TransactionType.SPLIT,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal.ZERO,
        totalCents = 0L,
        split = SplitRatio(to, from),
    )

    private fun linesOf(transactions: List<Transaction>): List<CashEntry> = transactions
        .filter { it.type == TransactionType.BUY || it.type == TransactionType.SELL }
        .map {
            CashEntry(
                id = "c-${it.id}",
                date = it.date,
                amountCents = if (it.type == TransactionType.BUY) -it.totalCents else it.totalCents,
                type = CashType.TRADE,
                note = "",
                enteredSeq = it.enteredSeq,
                transactionId = it.id,
            )
        }

    private fun fraction(
        numerator: Long,
        denominator: Long = 1L,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    private val day = LocalDate.of(2026, 12, 31)

    private val reversalNote = "Reversal of the deposit of 2026-04-20 ET"

    private fun reenterCash(
        existing: List<CashEntry>,
        originalId: String,
        reversalDate: String,
        type: CashType,
        date: String,
        amountCents: Long,
        note: String = "",
        direction: CashDirection = CashDirection.IN,
        reversalNote: String = this.reversalNote,
    ): CashPairResult = Reentries.reenterCash(
        existing = existing,
        reversalId = "r1",
        originalId = originalId,
        reversalDate = LocalDate.parse(reversalDate),
        reversalNote = reversalNote,
        replacementId = "n1",
        type = type,
        date = LocalDate.parse(date),
        amountCents = amountCents,
        note = note,
        direction = direction,
    )

    private fun cashCreated(result: CashPairResult): CashPairResult.Created = result as? CashPairResult.Created ?: error("Refused: $result")

    private fun cashRefusal(result: CashPairResult): ReentryRefusal = (result as CashPairResult.Refused).reason

    private fun reenterSplit(
        transactions: List<Transaction>,
        originalId: String,
        reversalDate: String,
        date: String,
        to: Int,
        from: Int,
        cashEntries: List<CashEntry> = listOf(opening()) + linesOf(transactions),
    ): SplitPairResult = Reentries.reenterSplit(
        cashEntries = cashEntries,
        transactions = transactions,
        portfolios = listOf(portfolioP),
        reversalId = "r1",
        replacementId = "n1",
        originalId = originalId,
        reversalDate = LocalDate.parse(reversalDate),
        portfolioId = "p1",
        ticker = "AAPL",
        date = LocalDate.parse(date),
        ratio = SplitRatio(to, from),
    )

    private fun splitCreated(result: SplitPairResult): SplitPairResult.Created = result as? SplitPairResult.Created ?: error("Refused: $result")

    private fun splitRefusal(result: SplitPairResult): ReentryRefusal = (result as SplitPairResult.Refused).reason

    private fun lot(transactions: List<Transaction>): Lot = Holdings.lots(transactions, day).first { it.buyId == "b1" }

    // --- Cash entries ---

    private val deposit500 = cash("d1", CashType.DEPOSIT, "2026-04-20", 50_000L, 2L)

    @Test
    fun frameE_aDepositOf500_reenteredAs50_savesAnAdjustmentOfMinus500WithItsNote_andADepositOf50_combinedMinus450() {
        val existing = listOf(opening(), deposit500)
        val result = cashCreated(reenterCash(existing, "d1", "2026-04-20", CashType.DEPOSIT, "2026-04-20", 5_000L))
        val reversal = result.reversal
        assertEquals(CashType.ADJUSTMENT, reversal.type)
        assertEquals(-50_000L, reversal.amountCents)
        assertEquals("d1", reversal.reversesId)
        assertEquals(reversalNote, reversal.note)
        assertEquals(LocalDate.of(2026, 4, 20), reversal.date)
        assertEquals(3L, reversal.enteredSeq)
        val replacement = result.replacement
        assertEquals(CashType.DEPOSIT, replacement.type)
        assertEquals(5_000L, replacement.amountCents)
        assertEquals(4L, replacement.enteredSeq)
        assertEquals(null, replacement.reversesId)
        assertEquals(-45_000L, reversal.amountCents + replacement.amountCents)
        assertEquals(1_005_000L, CashRules.balanceOn(existing + reversal + replacement, day))
    }

    @Test
    fun aDepositReenteredAsAWithdrawal_changesItsType_andItsSign() {
        val existing = listOf(opening(), deposit500)
        val result = cashCreated(reenterCash(existing, "d1", "2026-04-20", CashType.WITHDRAWAL, "2026-04-20", 20_000L))
        assertEquals(CashType.WITHDRAWAL, result.replacement.type)
        assertEquals(-20_000L, result.replacement.amountCents)
        assertEquals(980_000L, CashRules.balanceOn(existing + result.reversal + result.replacement, day))
    }

    @Test
    fun bothOrNeither_aWithdrawalReenteredLarger_isRefusedAsTheReplacementsCash_namingTheDateAndShortfall() {
        val existing = listOf(opening(), cash("w1", CashType.WITHDRAWAL, "2026-04-20", -900_000L, 2L))
        val refused = cashRefusal(reenterCash(existing, "w1", "2026-04-20", CashType.WITHDRAWAL, "2026-04-20", 1_100_000L))
        // $10,000 − $9,000 + $9,000 back − $11,000 = −$1,000 on Apr 20.
        assertEquals(ReentryRefusal.OfCash(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 4, 20), 100_000L)), refused)
        assertFalse(refused.fixedOnReview)
    }

    // 14a decision 5: on a shared date the pair is one point in the replay.
    @Test
    fun onASharedDate_theReversalMayTakeCashBelowZero_beforeTheNewDepositPutsItBack() {
        val spend = cash("w2", CashType.WITHDRAWAL, "2026-04-20", -1_050_000L, 3L)
        val existing = listOf(opening(), deposit500, spend)
        val result = cashCreated(reenterCash(existing, "d1", "2026-04-20", CashType.DEPOSIT, "2026-04-20", 60_000L))
        assertEquals(10_000L, CashRules.balanceOn(existing + result.reversal + result.replacement, day))
    }

    @Test
    fun aReversalDatedBeforeTheNewEntry_isRefusedAsTheReversalsCash_whenCashDipsInBetween() {
        val spend = cash("w2", CashType.WITHDRAWAL, "2026-04-20", -1_050_000L, 3L)
        val existing = listOf(opening(), deposit500, spend)
        val refused = cashRefusal(reenterCash(existing, "d1", "2026-04-20", CashType.DEPOSIT, "2026-04-22", 60_000L))
        assertEquals(ReentryRefusal.OfCashReversal(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 4, 20), 50_000L)), refused)
    }

    @Test
    fun aCashReversalDatedBeforeItsOriginal_isAReviewRefusal() {
        val refused = cashRefusal(reenterCash(listOf(opening(), deposit500), "d1", "2026-04-19", CashType.DEPOSIT, "2026-04-20", 5_000L))
        assertEquals(ReentryRefusal.OfCashReversal(CashEntryRefusal.BeforeOriginal(LocalDate.of(2026, 4, 20))), refused)
        assertTrue(refused.fixedOnReview)
    }

    @Test
    fun theNewCashEntrysOwnRules_nameTheReplacement() {
        val existing = listOf(opening(), deposit500)
        assertEquals(
            ReentryRefusal.OfCash(CashEntryRefusal.NoteRequired),
            cashRefusal(reenterCash(existing, "d1", "2026-04-20", CashType.ADJUSTMENT, "2026-04-20", 5_000L, note = " ")),
        )
        assertEquals(
            ReentryRefusal.OfCash(CashEntryRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 1))),
            cashRefusal(reenterCash(existing, "d1", "2026-04-20", CashType.DEPOSIT, "2025-12-31", 5_000L)),
        )
    }

    @Test
    fun aCashPair_onTheOpeningBalance_aTradeLine_aReversal_anEntryAlreadyReversed_orNoEntry_isRefusedAsPart1() {
        val trade = cash("t1", CashType.TRADE, "2026-04-21", -10_000L, 3L)
        val first = cashCreated(reenterCash(listOf(opening(), deposit500), "d1", "2026-04-20", CashType.DEPOSIT, "2026-04-20", 5_000L))
        val existing = listOf(opening(), deposit500, trade, first.reversal, first.replacement)
        fun refused(id: String) = cashRefusal(reenterCash(existing, id, "2026-04-22", CashType.DEPOSIT, "2026-04-22", 5_000L))
        assertEquals(ReentryRefusal.OfCashReversal(CashEntryRefusal.NotReversible(NotReversibleReason.OPENING_BALANCE)), refused("e1"))
        assertEquals(ReentryRefusal.OfCashReversal(CashEntryRefusal.NotReversible(NotReversibleReason.TRADE)), refused("t1"))
        assertEquals(ReentryRefusal.OfCashReversal(CashEntryRefusal.NotReversible(NotReversibleReason.IS_REVERSAL)), refused("r1"))
        assertEquals(ReentryRefusal.OfCashReversal(CashEntryRefusal.NotReversible(NotReversibleReason.ALREADY_REVERSED)), refused("d1"))
        assertEquals(ReentryRefusal.OfCashReversal(CashEntryRefusal.OriginalNotFound), refused("zz"))
    }

    @Test
    fun reverseAndReenter_isOfferedOnADirectCashEntry_notReversed_andNotOnTheOpeningBalance_aTradeLine_orAReversal() {
        val trade = cash("t1", CashType.TRADE, "2026-04-21", -10_000L, 3L)
        val existing = listOf(opening(), deposit500, trade)
        assertTrue(Reentries.cashOffered(existing, deposit500))
        assertFalse(Reentries.cashOffered(existing, existing.first()))
        assertFalse(Reentries.cashOffered(existing, trade))
        val first = cashCreated(reenterCash(existing, "d1", "2026-04-20", CashType.DEPOSIT, "2026-04-20", 5_000L))
        val after = existing + first.reversal + first.replacement
        assertFalse(Reentries.cashOffered(after, deposit500))
        assertFalse(Reentries.cashOffered(after, first.reversal))
        assertTrue(Reentries.cashOffered(after, first.replacement))
    }

    // --- Splits ---

    private val tenFor1000 = bought("b1", "2026-01-02", "10", 100_000L, 2L)
    private val twoForOne = splitOn("x1", "2026-03-01", 2, 1, 3L)

    @Test
    fun aTwoForOneReenteredAsThreeForOne_leavesTheLotAt30_exactly_withTheEffectOfEachPart() {
        val transactions = listOf(tenFor1000, twoForOne)
        val result = splitCreated(reenterSplit(transactions, "x1", "2026-03-01", "2026-03-01", 3, 1))
        assertEquals(TransactionType.REVERSAL, result.reversal.type)
        assertEquals("x1", result.reversal.reversesId)
        assertEquals(4L, result.reversal.enteredSeq)
        assertEquals(TransactionType.SPLIT, result.replacement.type)
        assertEquals(SplitRatio(3, 1), result.replacement.split)
        assertEquals(5L, result.replacement.enteredSeq)
        val after = transactions + result.reversal + result.replacement
        assertEquals(fraction(30), lot(after).remainingQuantity)
        assertEquals(fraction(100, 3), lot(after).costPerShare)
        assertEquals(fraction(1_000), lot(after).cost)
        assertEquals(ReversalEffect.OfSplit(SplitRatio(1, 2), fraction(20), fraction(10)), Reversals.effect(after, result.reversal))
        assertEquals(SplitEffect(fraction(10), fraction(30)), Splits.effect(after, result.replacement))
    }

    // The reversal alone would leave the Apr 1 sell of 15 short (10 < 15); the pair is checked with both applied (4.6).
    @Test
    fun aSplitPair_isCheckedWithBothSplits_soTheReversalAloneMayLeaveALaterSellShort_andANewRatioThatDoesIsRefusedAsPart2() {
        val sale = sold("s1", "2026-04-01", fraction(15), 90_000L, 4L)
        val transactions = listOf(tenFor1000, twoForOne, sale)
        val threeForTwo = splitCreated(reenterSplit(transactions, "x1", "2026-03-01", "2026-03-01", 3, 2))
        assertEquals(Fraction.ZERO, lot(transactions + threeForTwo.reversal + threeForTwo.replacement).remainingQuantity)
        val refused = splitRefusal(reenterSplit(transactions, "x1", "2026-03-01", "2026-03-01", 4, 3))
        assertEquals(ReentryRefusal.OfSplit(SplitRefusal.SellShort(LocalDate.of(2026, 4, 1))), refused)
        assertFalse(refused.fixedOnReview)
    }

    // Rounding (CLAUDE.md pitfall from 12): a 1 for 3 leaves 10/3 (3.33333333, rounded down), sold in full; re-entered as
    // 1 for 2 the lot ends at 5 − 10/3 = 5/3 (1.66666667, rounded up); as 1 for 7 (10/7 < 10/3) the sell is left short.
    @Test
    fun aOneForThreeWhoseLotWasSoldInFull_reenteredAsOneForTwo_leavesExactly5Over3_andAsOneForSeven_isRefused() {
        val oneForThree = splitOn("x1", "2026-03-01", 1, 3, 3L)
        val sale = sold("s1", "2026-04-01", fraction(10, 3), 50_000L, 4L)
        val transactions = listOf(tenFor1000, oneForThree, sale)
        val unchanged = splitCreated(reenterSplit(transactions, "x1", "2026-03-01", "2026-03-01", 1, 3))
        assertEquals(Fraction.ZERO, lot(transactions + unchanged.reversal + unchanged.replacement).remainingQuantity)
        val oneForTwo = splitCreated(reenterSplit(transactions, "x1", "2026-03-01", "2026-03-01", 1, 2))
        val left = lot(transactions + oneForTwo.reversal + oneForTwo.replacement).remainingQuantity
        assertEquals(fraction(5, 3), left)
        assertEquals(BigDecimal("1.66666667"), Sells.shown(left))
        assertEquals(
            ReentryRefusal.OfSplit(SplitRefusal.SellShort(LocalDate.of(2026, 4, 1))),
            splitRefusal(reenterSplit(transactions, "x1", "2026-03-01", "2026-03-01", 1, 7)),
        )
    }

    @Test
    fun aSplitReversalDatedBeforeItsOriginal_isAReviewRefusal_andASplitAlreadyReversed_orNotASplit_isRefusedAsPart1() {
        val transactions = listOf(tenFor1000, twoForOne)
        val before = splitRefusal(reenterSplit(transactions, "x1", "2026-02-28", "2026-03-01", 3, 1))
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.BeforeOriginal(LocalDate.of(2026, 3, 1))), before)
        assertTrue(before.fixedOnReview)
        val first = splitCreated(reenterSplit(transactions, "x1", "2026-03-01", "2026-03-01", 3, 1))
        val after = transactions + first.reversal + first.replacement
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.NotReversible), splitRefusal(reenterSplit(after, "x1", "2026-03-02", "2026-03-02", 4, 1)))
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.NotFound), splitRefusal(reenterSplit(after, "b1", "2026-03-02", "2026-03-02", 4, 1)))
    }

    @Test
    fun theNewSplitsOwnRules_nameTheReplacement() {
        val transactions = listOf(tenFor1000, twoForOne)
        assertEquals(ReentryRefusal.OfSplit(SplitRefusal.RatioEqual), splitRefusal(reenterSplit(transactions, "x1", "2026-03-01", "2026-03-01", 2, 2)))
        assertEquals(
            ReentryRefusal.OfSplit(SplitRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 1))),
            splitRefusal(reenterSplit(transactions, "x1", "2026-03-01", "2025-12-31", 3, 1)),
        )
    }
}
