package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class ReentriesTest {
    private val portfolioP = Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L)

    private fun opening(cents: Long = 1_000_000L): CashEntry = CashEntry("e1", LocalDate.of(2026, 1, 1), cents, CashType.OPENING_BALANCE, "", 1L)

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
        allocations: List<SellAllocation>,
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
        allocations = allocations,
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

    /** The trade line of a buy (cash out) or a sell (cash in); none for a split. */
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

    private var nextId = 0

    private fun ids(): ReentryIds {
        val n = ++nextId
        return ReentryIds(reversalId = "r$n", reversalCashId = "cr$n", replacementId = "n$n", replacementCashId = "cn$n")
    }

    private fun reenterBuy(
        transactions: List<Transaction>,
        originalId: String,
        reversalDate: String,
        date: String,
        quantity: String,
        totalCents: Long,
        cashEntries: List<CashEntry> = listOf(opening()) + linesOf(transactions),
    ): ReentryResult = Reentries.reenterBuy(
        cashEntries = cashEntries,
        transactions = transactions,
        portfolios = listOf(portfolioP),
        ids = ids(),
        originalId = originalId,
        reversalDate = LocalDate.parse(reversalDate),
        portfolioId = "p1",
        ticker = "AAPL",
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
    )

    private fun reenterSell(
        transactions: List<Transaction>,
        originalId: String,
        reversalDate: String,
        date: String,
        quantity: String,
        totalCents: Long,
        picks: List<Pick>,
        cashEntries: List<CashEntry> = listOf(opening()) + linesOf(transactions),
        ticker: String = "AAPL",
    ): ReentryResult = Reentries.reenterSell(
        cashEntries = cashEntries,
        transactions = transactions,
        portfolios = listOf(portfolioP),
        ids = ids(),
        originalId = originalId,
        reversalDate = LocalDate.parse(reversalDate),
        portfolioId = "p1",
        ticker = ticker,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        picks = picks,
    )

    private fun created(result: ReentryResult): ReentryResult.Created = result as? ReentryResult.Created ?: error("Refused: $result")

    private fun refusal(result: ReentryResult): ReentryRefusal = (result as ReentryResult.Refused).reason

    private val day = LocalDate.of(2026, 12, 31)

    private fun lot(
        transactions: List<Transaction>,
        buyId: String,
    ): Lot = Holdings.lots(transactions, day).first { it.buyId == buyId }

    private fun saved(
        transactions: List<Transaction>,
        result: ReentryResult.Created,
    ): List<Transaction> = transactions + result.reversal + result.replacement

    private fun cashAfter(
        cash: List<CashEntry>,
        result: ReentryResult.Created,
    ): List<CashEntry> = cash + result.reversalCashLine + result.replacementCashLine

    private val tenFor1000 = bought("b1", "2026-01-02", "10", 100_000L, 2L)

    @Test
    fun exampleG_theBuyReenteredAs100Shares_savesAReversalAndANewBuy_threeRecords_cash9000_andTheFirstLotAtZero() {
        val transactions = listOf(tenFor1000)
        val cash = listOf(opening()) + linesOf(transactions)
        val result = created(reenterBuy(transactions, "b1", "2026-01-02", "2026-01-02", "100", 100_000L, cash))
        assertEquals(TransactionType.REVERSAL, result.reversal.type)
        assertEquals("b1", result.reversal.reversesId)
        assertEquals(LocalDate.of(2026, 1, 2), result.reversal.date)
        assertEquals(100_000L, result.reversalCashLine.amountCents)
        assertEquals(result.reversal.id, result.reversalCashLine.transactionId)
        assertEquals(TransactionType.BUY, result.replacement.type)
        assertEquals(LocalDate.of(2026, 1, 2), result.replacement.date)
        assertEquals(BigDecimal("100"), result.replacement.quantity)
        assertEquals(-100_000L, result.replacementCashLine.amountCents)
        val after = saved(transactions, result)
        assertEquals(3, after.size)
        assertEquals(Fraction.ZERO, lot(after, "b1").remainingQuantity)
        assertEquals(fraction(100), lot(after, result.replacement.id).remainingQuantity)
        assertEquals(fraction(10), lot(after, result.replacement.id).costPerShare)
        assertEquals(900_000L, CashRules.balanceOn(cashAfter(cash, result), day))
    }

    @Test
    fun theNewEntry_takesTheNextEntryOrderAfterTheReversal_andEachCashLineSharesItsEntrysOrder() {
        val result = created(reenterBuy(listOf(tenFor1000), "b1", "2026-01-02", "2026-01-02", "100", 100_000L))
        assertEquals(3L, result.reversal.enteredSeq)
        assertEquals(3L, result.reversalCashLine.enteredSeq)
        assertEquals(4L, result.replacement.enteredSeq)
        assertEquals(4L, result.replacementCashLine.enteredSeq)
        assertEquals(result.replacement.id, result.replacementCashLine.transactionId)
    }

    @Test
    fun exampleG_bothOrNeither_aNewTotalOf10500_isRefusedAsTheReplacementsCash_namingJan2And500() {
        val refused = refusal(reenterBuy(listOf(tenFor1000), "b1", "2026-01-02", "2026-01-02", "10", 1_050_000L))
        assertEquals(ReentryRefusal.OfBuy(BuyRefusal.NegativeCash(LocalDate.of(2026, 1, 2), 50_000L)), refused)
        assertFalse(refused.fixedOnReview)
    }

    @Test
    fun exampleG_blockedByTheOriginal_whileTheSellStands_saysReverseTheSellFirst_andIsNotFixedOnTheReview() {
        val sale = sold("s1", "2026-01-03", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val jan3 = LocalDate.of(2026, 1, 3)
        val refused = refusal(reenterBuy(listOf(tenFor1000, sale), "b1", "2026-01-02", "2026-01-02", "100", 100_000L))
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.SellNotReversed(jan3, fraction(-10), jan3)), refused)
        assertFalse(refused.fixedOnReview)
    }

    @Test
    fun exampleG_afterTheSellsReversal_theReversalDatedJan2_isAReviewRefusalNamingJan3_andDatedJan3_withTheNewBuyOnJan2_isSaved() {
        val sale = sold("s1", "2026-01-03", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val sellReversal =
            Reversals.reverse(listOf(opening()) + linesOf(listOf(tenFor1000, sale)), listOf(tenFor1000, sale), "rs", "crs", "s1", LocalDate.of(2026, 1, 3))
                as ReversalResult.Created
        val transactions = listOf(tenFor1000, sale, sellReversal.transaction)
        val cash = listOf(opening()) + linesOf(listOf(tenFor1000, sale)) + listOfNotNull(sellReversal.cashLine)
        val jan3 = LocalDate.of(2026, 1, 3)
        val refused = refusal(reenterBuy(transactions, "b1", "2026-01-02", "2026-01-02", "100", 100_000L, cash))
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.TooEarly(jan3, fraction(-10), LocalDate.of(2026, 1, 2), jan3)), refused)
        assertTrue(refused.fixedOnReview)
        // The new buy's shares do not cover the original lot: lots are checked one by one (4.1, 4.6).
        val result = created(reenterBuy(transactions, "b1", "2026-01-03", "2026-01-02", "100", 100_000L, cash))
        assertEquals(jan3, result.reversal.date)
        assertEquals(LocalDate.of(2026, 1, 2), result.replacement.date)
        val after = saved(transactions, result)
        assertEquals(Fraction.ZERO, lot(after, "b1").remainingQuantity)
        assertEquals(fraction(100), lot(after, result.replacement.id).remainingQuantity)
        assertEquals(900_000L, CashRules.balanceOn(cashAfter(cash, result), day))
    }

    @Test
    fun aReversalDatedBeforeItsOriginal_isAReviewRefusal() {
        val refused = refusal(reenterBuy(listOf(tenFor1000), "b1", "2026-01-01", "2026-01-02", "100", 100_000L))
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.BeforeOriginal(LocalDate.of(2026, 1, 2))), refused)
        assertTrue(refused.fixedOnReview)
    }

    @Test
    fun exampleG_theSellReenteredAt2200_startsFromTheOriginalPicks_cash9700_cost1300_realized900() {
        // Example A: cash $9,600 after the Mar 1 sell of 12 for $2,100 (lot 1 x 10, lot 2 x 2).
        val b2 = bought("b2", "2026-02-01", "10", 150_000L, 3L)
        val sale = sold("s1", "2026-03-01", "12", 210_000L, 4L, listOf(SellAllocation("b1", fraction(10)), SellAllocation("b2", fraction(2))))
        val transactions = listOf(tenFor1000, b2, sale)
        val cash = listOf(opening()) + linesOf(transactions)
        assertEquals(960_000L, CashRules.balanceOn(cash, day))
        val reversal = Reversals.draft(cash, transactions, sale, "r0", sale.date)
        val picks = Reentries.startingPicks(transactions + reversal, sale, sale.date, reversal.enteredSeq + 1)
        assertEquals(listOf(Pick("b1", BigDecimal("10")), Pick("b2", BigDecimal("2"))), picks)
        val result = created(reenterSell(transactions, "s1", "2026-03-01", "2026-03-01", "12", 220_000L, picks, cash))
        assertEquals(-210_000L, result.reversalCashLine.amountCents)
        assertEquals(220_000L, result.replacementCashLine.amountCents)
        assertEquals(970_000L, CashRules.balanceOn(cashAfter(cash, result), day))
        val after = saved(transactions, result)
        val pieces = Sells.pieces(after, result.replacement)
        assertEquals(fraction(1_300), pieces.fold(Fraction.ZERO) { sum, piece -> sum + piece.cost })
        assertEquals(fraction(900), Sells.realizedGain(pieces))
        assertEquals(Fraction.ZERO, lot(after, "b1").remainingQuantity)
        assertEquals(fraction(8), lot(after, "b2").remainingQuantity)
    }

    @Test
    fun theSellsLotList_isAsItWillBeAfterTheReversal_soTheOriginalsLotsAreAvailableAgain() {
        val sale = sold("s1", "2026-03-01", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val transactions = listOf(tenFor1000, sale)
        val cash = listOf(opening()) + linesOf(transactions)
        assertEquals(emptyList<LotChoice>(), Sells.lotChoices(transactions, "p1", "AAPL", sale.date, sellSeq = 4L))
        val reversal = Reversals.draft(cash, transactions, sale, "r0", sale.date)
        val choice = Sells.lotChoices(transactions + reversal, "p1", "AAPL", sale.date, reversal.enteredSeq + 1).single()
        assertEquals("b1", choice.buyId)
        assertEquals(fraction(10), choice.available)
    }

    @Test
    fun theStartingPicks_carryTheSplitsBetweenTheOriginalSellAndTheNewOne() {
        val sale = sold("s1", "2026-03-01", "4", 60_000L, 3L, listOf(SellAllocation("b1", fraction(4))))
        val split = splitOn("x1", "2026-03-01", 3, 1, 4L)
        val transactions = listOf(tenFor1000, sale, split)
        val reversal = Reversals.draft(listOf(opening()) + linesOf(transactions), transactions, sale, "r0", sale.date)
        assertEquals(listOf(Pick("b1", BigDecimal("12"))), Reentries.startingPicks(transactions + reversal, sale, sale.date, reversal.enteredSeq + 1))
    }

    // 14a fix (phone check 2): the new entry goes after a split entered later on the original's date, so its first
    // quantity carries that split, as its picks do.
    @Test
    fun theStartingQuantity_ofABuyWithASplitEnteredAfterItThatDay_carriesTheSplit() {
        val one = bought("b1", "2026-01-02", "1", 15_000L, 2L)
        val split = splitOn("x1", "2026-01-02", 2, 1, 3L)
        val transactions = listOf(one, split)
        val reversal = Reversals.draft(listOf(opening()) + linesOf(transactions), transactions, one, "r0", one.date)
        assertEquals(BigDecimal("2"), Reentries.startingQuantity(transactions + reversal, one, one.date, reversal.enteredSeq + 1))
    }

    @Test
    fun theStartingQuantity_after1For3_isRoundedTo8Places_andWithNoSplitBetween_keepsTheOriginalText() {
        val one = bought("b1", "2026-01-02", "1", 15_000L, 2L)
        val oneForThree = splitOn("x1", "2026-01-02", 1, 3, 3L)
        val withSplit = listOf(one, oneForThree)
        val r1 = Reversals.draft(listOf(opening()) + linesOf(withSplit), withSplit, one, "r0", one.date)
        assertEquals(BigDecimal("0.33333333"), Reentries.startingQuantity(withSplit + r1, one, one.date, r1.enteredSeq + 1))
        val typed = bought("b2", "2026-01-02", "2.50", 25_000L, 2L)
        val r2 = Reversals.draft(listOf(opening()) + linesOf(listOf(typed)), listOf(typed), typed, "r0", typed.date)
        val kept = Reentries.startingQuantity(listOf(typed, r2), typed, typed.date, r2.enteredSeq + 1)
        assertEquals("2.50", kept.toPlainString())
    }

    // Rounding (CLAUDE.md pitfall from 12): 1/7 rounds down to 0.14285714 and 3/7 rounds up to 0.42857143.
    @Test
    fun aSellAllOfSevenths_reenteredWithANewTotal_takesTheExactRemaindersAgain_andLeavesBothLotsAtZero() {
        val one = bought("b1", "2026-01-02", "1", 10_000L, 2L)
        val three = bought("b2", "2026-01-03", "3", 30_000L, 3L)
        val oneForSeven = splitOn("x1", "2026-02-01", 1, 7, 4L)
        val before = listOf(one, three, oneForSeven)
        val picks = Sells.sellAll(Sells.lotChoices(before, "p1", "AAPL", LocalDate.of(2026, 3, 1), sellSeq = 5L))
        assertEquals(listOf(Pick("b1", BigDecimal("0.14285714")), Pick("b2", BigDecimal("0.42857143"))), picks)
        val sale =
            (
                Sells.sell(
                    cashEntries = listOf(opening()) + linesOf(before),
                    transactions = before,
                    portfolios = listOf(portfolioP),
                    transactionId = "s1",
                    cashEntryId = "c-s1",
                    portfolioId = "p1",
                    ticker = "AAPL",
                    date = LocalDate.of(2026, 3, 1),
                    quantity = BigDecimal("0.57142857"),
                    totalCents = 50_000L,
                    picks = picks,
                ) as SellResult.Created
                ).transaction
        val transactions = before + sale
        val cash = listOf(opening()) + linesOf(transactions)
        val reversal = Reversals.draft(cash, transactions, sale, "r0", sale.date)
        val starting = Reentries.startingPicks(transactions + reversal, sale, sale.date, reversal.enteredSeq + 1)
        assertEquals(picks, starting)
        val result = created(reenterSell(transactions, "s1", "2026-03-01", "2026-03-01", "0.57142857", 55_000L, starting, cash))
        assertEquals(listOf(SellAllocation("b1", fraction(1, 7)), SellAllocation("b2", fraction(3, 7))), result.replacement.allocations)
        val after = saved(transactions, result)
        assertEquals(listOf(Fraction.ZERO, Fraction.ZERO), Holdings.lots(after, day).map { it.remainingQuantity })
    }

    // Rounding: 2/3 rounds up to 0.66666667; a smaller new sell leaves the exact rest of the lot.
    @Test
    fun aSellOfTwoThirds_reenteredForLess_leavesTheExactRestOfTheLot_andTheRestCanBeSoldLater() {
        val two = bought("b1", "2026-01-02", "2", 20_000L, 2L)
        val oneForThree = splitOn("x1", "2026-02-01", 1, 3, 3L)
        val sale = sold("s1", "2026-03-01", "0.66666667", 30_000L, 4L, listOf(SellAllocation("b1", fraction(2, 3))))
        val transactions = listOf(two, oneForThree, sale)
        val cash = listOf(opening()) + linesOf(transactions)
        val result =
            created(reenterSell(transactions, "s1", "2026-03-01", "2026-03-01", "0.33333333", 15_000L, listOf(Pick("b1", BigDecimal("0.33333333"))), cash))
        val after = saved(transactions, result)
        val rest = fraction(2, 3) - Fraction.of(BigDecimal("0.33333333"))
        assertEquals(rest, lot(after, "b1").remainingQuantity)
        val choice = Sells.lotChoices(after, "p1", "AAPL", LocalDate.of(2026, 4, 1), sellSeq = 99L).single()
        assertEquals(rest, choice.available)
        assertEquals(BigDecimal("0.33333334"), Sells.shown(choice.available))
    }

    @Test
    fun aSellPair_isRefusedForTheNewSellsLotRules_namingTheNewSell() {
        val b2 = bought("b2", "2026-03-15", "10", 150_000L, 4L)
        val sale = sold("s1", "2026-03-01", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val transactions = listOf(tenFor1000, sale, b2)
        val refused = refusal(reenterSell(transactions, "s1", "2026-03-01", "2026-03-01", "10", 120_000L, listOf(Pick("b2", BigDecimal("10")))))
        assertEquals(ReentryRefusal.OfSell(SellRefusal.PickNotAvailable("b2", LotUnavailable.BOUGHT_AFTER_SELL_DATE)), refused)
        assertFalse(refused.fixedOnReview)
    }

    // 14a decision 6: on a shared date the pair is one point in the replay, judged only after the new entry.
    @Test
    fun aSellPair_onASharedDate_isJudgedOnlyAfterBothEntries_soTheReversalMayTakeCashBelowZeroBeforeTheNewSell() {
        // Cash is $0 after the Jan 3 SPY buy spends the sell's $1,200; the reversal takes $1,200 out, the new sell puts $1,300 in.
        val sale = sold("s1", "2026-01-03", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val spy = bought("b2", "2026-01-03", "1", 120_000L, 4L, ticker = "SPY")
        val transactions = listOf(tenFor1000, sale, spy)
        val cash = listOf(opening(100_000L)) + linesOf(transactions)
        val result = created(reenterSell(transactions, "s1", "2026-01-03", "2026-01-03", "10", 130_000L, listOf(Pick("b1", BigDecimal("10"))), cash))
        assertEquals(10_000L, CashRules.balanceOn(cashAfter(cash, result), day))
    }

    @Test
    fun aSellPair_onASharedDate_isRefusedAsTheNewSellsCash_whenCashIsStillNegativeAfterBothEntries() {
        val sale = sold("s1", "2026-01-03", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val spy = bought("b2", "2026-01-03", "1", 120_000L, 4L, ticker = "SPY")
        val transactions = listOf(tenFor1000, sale, spy)
        val cash = listOf(opening(100_000L)) + linesOf(transactions)
        val refused = refusal(reenterSell(transactions, "s1", "2026-01-03", "2026-01-03", "10", 110_000L, listOf(Pick("b1", BigDecimal("10"))), cash))
        assertEquals(ReentryRefusal.OfSell(SellRefusal.NegativeCash(LocalDate.of(2026, 1, 3), 10_000L)), refused)
    }

    @Test
    fun aSellPair_whoseReversalIsDatedBeforeTheNewSell_isRefusedAsTheReversalsCash_whenCashDipsInBetween() {
        val sale = sold("s1", "2026-01-03", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val spy = bought("b2", "2026-01-04", "1", 120_000L, 4L, ticker = "SPY")
        val transactions = listOf(tenFor1000, sale, spy)
        val cash = listOf(opening(100_000L)) + linesOf(transactions)
        // Reversed on Jan 4 after the SPY buy, the new sell on Jan 5: cash is −$1,200 on Jan 4.
        val refused = refusal(reenterSell(transactions, "s1", "2026-01-04", "2026-01-05", "10", 130_000L, listOf(Pick("b1", BigDecimal("10"))), cash))
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.NegativeCash(LocalDate.of(2026, 1, 4), 120_000L)), refused)
    }

    @Test
    fun criterion60b_aSellAtZero_reenteredAt50_changesTheRealizedLossFromMinus200ToMinus150_andTheHoldingStaysAtZero() {
        val oldco = bought("b9", "2026-01-05", "10", 20_000L, 2L, ticker = "OLDCO")
        val sale = sold("s9", "2026-03-10", "10", 0L, 3L, listOf(SellAllocation("b9", fraction(10))), ticker = "OLDCO")
        val transactions = listOf(oldco, sale)
        val cash = listOf(opening()) + linesOf(transactions)
        assertEquals(fraction(-200), Sells.realizedGain(Sells.pieces(transactions, sale)))
        val result =
            created(
                reenterSell(transactions, "s9", "2026-03-10", "2026-03-10", "10", 5_000L, listOf(Pick("b9", BigDecimal("10"))), cash, ticker = "OLDCO"),
            )
        val after = saved(transactions, result)
        assertEquals(fraction(-150), Sells.realizedGain(Sells.pieces(after, result.replacement)))
        assertEquals(Fraction.ZERO, Holdings.holdings(after, day).single().shares)
    }

    @Test
    fun aPair_onAReversal_aReversedEntry_aSplit_aCashLine_orTheWrongKind_isRefused() {
        val split = splitOn("x1", "2026-03-01", 2, 1, 3L)
        val first = created(reenterBuy(listOf(tenFor1000), "b1", "2026-01-02", "2026-01-02", "100", 100_000L))
        val transactions = saved(listOf(tenFor1000), first)
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.NotReversible), refusal(reenterBuy(transactions, "b1", "2026-01-02", "2026-01-02", "5", 1_000L)))
        assertEquals(
            ReentryRefusal.OfReversal(ReversalRefusal.NotFound),
            refusal(reenterBuy(transactions, first.reversal.id, "2026-01-02", "2026-01-02", "5", 1_000L)),
        )
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.NotFound), refusal(reenterBuy(listOf(tenFor1000, split), "x1", "2026-03-01", "2026-03-01", "5", 1_000L)))
        assertEquals(ReentryRefusal.OfReversal(ReversalRefusal.NotFound), refusal(reenterBuy(listOf(tenFor1000), "c-b1", "2026-01-02", "2026-01-02", "5", 1_000L)))
        assertEquals(
            ReentryRefusal.OfReversal(ReversalRefusal.NotFound),
            refusal(reenterSell(listOf(tenFor1000), "b1", "2026-01-02", "2026-01-02", "5", 1_000L, listOf(Pick("b1", BigDecimal("5"))))),
        )
    }

    @Test
    fun reverseAndReenter_isOffered_onABuyOrSell_notReversed_andNotOnASplitOrAReversal() {
        val sale = sold("s1", "2026-03-01", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val split = splitOn("x1", "2026-02-01", 2, 1, 4L)
        val transactions = listOf(tenFor1000, sale, split)
        assertTrue(Reentries.offered(transactions, tenFor1000))
        assertTrue(Reentries.offered(transactions, sale))
        assertFalse(Reentries.offered(transactions, split))
        val result = created(reenterSell(listOf(tenFor1000, sale), "s1", "2026-03-01", "2026-03-01", "10", 130_000L, listOf(Pick("b1", BigDecimal("10")))))
        val after = saved(listOf(tenFor1000, sale), result)
        assertFalse(Reentries.offered(after, sale))
        assertFalse(Reentries.offered(after, result.reversal))
        assertTrue(Reentries.offered(after, result.replacement))
    }
}
