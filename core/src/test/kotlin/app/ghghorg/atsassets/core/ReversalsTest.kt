package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class ReversalsTest {
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
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
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

    private fun reverse(
        transactions: List<Transaction>,
        originalId: String,
        date: String,
        cashEntries: List<CashEntry> = listOf(opening()) + linesOf(transactions),
    ): ReversalResult = Reversals.reverse(
        cashEntries = cashEntries,
        transactions = transactions,
        transactionId = "r${++nextId}",
        cashEntryId = "cr$nextId",
        originalId = originalId,
        date = LocalDate.parse(date),
    )

    private fun created(result: ReversalResult): ReversalResult.Created = result as? ReversalResult.Created ?: error("Refused: $result")

    private fun refusal(result: ReversalResult): ReversalRefusal = (result as ReversalResult.Refused).reason

    private val day = LocalDate.of(2026, 12, 31)

    private fun lot(
        transactions: List<Transaction>,
        buyId: String,
    ): Lot = Holdings.lots(transactions, day).first { it.buyId == buyId }

    private val tenFor1000 = bought("b1", "2026-01-02", "10", 100_000L, 2L)

    @Test
    fun exampleF_aBuysReversalAfterATwoForOne_removes20Shares_returns1000_andLeavesTheLotAtZero() {
        val split = splitOn("x1", "2026-03-01", 2, 1, 3L)
        val transactions = listOf(tenFor1000, split)
        val result = created(reverse(transactions, "b1", "2026-04-01"))
        val reversal = result.transaction
        assertEquals(TransactionType.REVERSAL, reversal.type)
        assertEquals("b1", reversal.reversesId)
        assertEquals("p1", reversal.portfolioId)
        assertEquals("AAPL", reversal.ticker)
        assertEquals(LocalDate.of(2026, 4, 1), reversal.date)
        assertEquals(4L, reversal.enteredSeq)
        val line = result.cashLine ?: error("no cash line")
        assertEquals(100_000L, line.amountCents)
        assertEquals(CashType.TRADE, line.type)
        assertEquals(reversal.id, line.transactionId)
        assertEquals(LocalDate.of(2026, 4, 1), line.date)
        val after = transactions + reversal
        assertEquals(ReversalEffect.OfBuy(sharesRemoved = fraction(20), cashCents = 100_000L), Reversals.effect(after, reversal))
        assertEquals(Fraction.ZERO, lot(after, "b1").remainingQuantity)
        assertEquals(Fraction.ZERO, lot(after, "b1").cost)
        val cash = listOf(opening()) + linesOf(transactions) + line
        assertEquals(fraction(10_000), Valuation.netWorth(cash, after, day))
    }

    @Test
    fun exampleD_theBuysReversal_isRefusedWhileTheSellStands_namingTheDateTheSharesAndThatSell() {
        val sale = sold("s1", "2026-01-03", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val transactions = listOf(tenFor1000, sale)
        val jan3 = LocalDate.of(2026, 1, 3)
        assertEquals(ReversalRefusal.SellNotReversed(jan3, fraction(-10), jan3), refusal(reverse(transactions, "b1", "2026-01-02")))
        assertEquals(
            ReversalRefusal.SellNotReversed(LocalDate.of(2026, 1, 5), fraction(-10), jan3),
            refusal(reverse(transactions, "b1", "2026-01-05")),
        )
    }

    @Test
    fun exampleD_afterTheSellsReversal_theBuysReversalDatedJan2_isRefusedNamingJan3_andDatedJan3_isSaved() {
        val sale = sold("s1", "2026-01-03", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val sellReversal = created(reverse(listOf(tenFor1000, sale), "s1", "2026-01-03"))
        val transactions = listOf(tenFor1000, sale, sellReversal.transaction)
        val cash = listOf(opening()) + linesOf(listOf(tenFor1000, sale)) + listOfNotNull(sellReversal.cashLine)
        val jan3 = LocalDate.of(2026, 1, 3)
        assertEquals(
            ReversalRefusal.TooEarly(jan3, fraction(-10), LocalDate.of(2026, 1, 2), jan3),
            refusal(reverse(transactions, "b1", "2026-01-02", cash)),
        )
        val buyReversal = created(reverse(transactions, "b1", "2026-01-03", cash))
        val after = transactions + buyReversal.transaction
        assertEquals(Fraction.ZERO, lot(after, "b1").remainingQuantity)
        assertEquals(fraction(10_000), Valuation.netWorth(cash + listOfNotNull(buyReversal.cashLine), after, day))
    }

    @Test
    fun whenSeveralSellsTookFromTheLot_theRefusalNamesTheEarliestNotYetReversed_thenTheLatestReversalDate() {
        val first = sold("s1", "2026-01-03", "4", 50_000L, 3L, listOf(SellAllocation("b1", fraction(4))))
        val second = sold("s2", "2026-01-05", "6", 70_000L, 4L, listOf(SellAllocation("b1", fraction(6))))
        var transactions = listOf(tenFor1000, first, second)
        var cash = listOf(opening()) + linesOf(transactions)
        val jan3 = LocalDate.of(2026, 1, 3)
        assertEquals(ReversalRefusal.SellNotReversed(jan3, fraction(-4), jan3), refusal(reverse(transactions, "b1", "2026-01-02", cash)))
        val r1 = created(reverse(transactions, "s1", "2026-01-10", cash))
        transactions = transactions + r1.transaction
        cash = cash + listOfNotNull(r1.cashLine)
        // s1 is reversed on Jan 10, so dated Jan 2 the lot still goes to −4 at s1 on Jan 3; s2 is the earliest not reversed.
        assertEquals(
            ReversalRefusal.SellNotReversed(jan3, fraction(-4), LocalDate.of(2026, 1, 5)),
            refusal(reverse(transactions, "b1", "2026-01-02", cash)),
        )
        val r2 = created(reverse(transactions, "s2", "2026-01-07", cash))
        transactions = transactions + r2.transaction
        cash = cash + listOfNotNull(r2.cashLine)
        assertEquals(
            ReversalRefusal.TooEarly(jan3, fraction(-4), LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 10)),
            refusal(reverse(transactions, "b1", "2026-01-02", cash)),
        )
        assertTrue(reverse(transactions, "b1", "2026-01-10", cash) is ReversalResult.Created)
    }

    @Test
    fun aSellsReversal_putsItsExactLotsBack_withTheSplitsSince_takesTheProceedsOut_andRemovesItsGain() {
        // Example A, then a 2 for 1 split on Apr 1; the Mar 1 sell is reversed on May 1.
        val b2 = bought("b2", "2026-02-01", "10", 150_000L, 3L)
        val sale = sold("s1", "2026-03-01", "12", 210_000L, 4L, listOf(SellAllocation("b1", fraction(10)), SellAllocation("b2", fraction(2))))
        val split = splitOn("x1", "2026-04-01", 2, 1, 5L)
        val transactions = listOf(tenFor1000, b2, sale, split)
        val result = created(reverse(transactions, "s1", "2026-05-01"))
        assertEquals(-210_000L, result.cashLine?.amountCents)
        val after = transactions + result.transaction
        assertEquals(
            ReversalEffect.OfSell(
                lots =
                listOf(
                    RestoredLot("b1", LocalDate.of(2026, 1, 2), fraction(20)),
                    RestoredLot("b2", LocalDate.of(2026, 2, 1), fraction(4)),
                ),
                cashCents = -210_000L,
                realizedRemoved = fraction(800),
            ),
            Reversals.effect(after, result.transaction),
        )
        assertEquals(fraction(20), lot(after, "b1").remainingQuantity)
        assertEquals(fraction(1_000), lot(after, "b1").cost)
        assertEquals(fraction(20), lot(after, "b2").remainingQuantity)
        assertEquals(fraction(75), lot(after, "b2").costPerShare)
    }

    @Test
    fun aSellsReversal_isRefusedWhenCashWouldBeNegative() {
        val sale = sold("s1", "2026-01-03", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val spy = bought("b2", "2026-01-04", "1", 120_000L, 4L, ticker = "SPY")
        val transactions = listOf(tenFor1000, sale, spy)
        val cash = listOf(opening(100_000L)) + linesOf(transactions)
        assertEquals(ReversalRefusal.NegativeCash(LocalDate.of(2026, 1, 4), 120_000L), refusal(reverse(transactions, "s1", "2026-01-03", cash)))
    }

    @Test
    fun criterion59a_reversingTheOneForThree_givesExactly10Shares_at100PerShare_withNoCashLine() {
        val split = splitOn("x1", "2026-03-01", 1, 3, 3L)
        val transactions = listOf(tenFor1000, split)
        val result = created(reverse(transactions, "x1", "2026-04-01"))
        assertNull(result.cashLine)
        val after = transactions + result.transaction
        assertEquals(fraction(10), lot(after, "b1").remainingQuantity)
        assertEquals(fraction(100), lot(after, "b1").costPerShare)
        assertEquals(fraction(1_000), lot(after, "b1").cost)
        assertEquals(
            ReversalEffect.OfSplit(inverse = SplitRatio(3, 1), before = fraction(10, 3), after = fraction(10)),
            Reversals.effect(after, result.transaction),
        )
    }

    @Test
    fun aSplitsReversal_thatLeavesALaterSellShort_isRefused_namingThatSellsDate() {
        val split = splitOn("x1", "2026-03-01", 2, 1, 3L)
        val sale = sold("s1", "2026-04-01", "15", 90_000L, 4L, listOf(SellAllocation("b1", fraction(15))))
        val transactions = listOf(tenFor1000, split, sale)
        assertEquals(ReversalRefusal.SellShort(LocalDate.of(2026, 4, 1)), refusal(reverse(transactions, "x1", "2026-03-01")))
        // Dated after the sell, it leaves the 5 shares that are left, halved.
        val later = created(reverse(transactions, "x1", "2026-05-01"))
        assertEquals(fraction(5, 2), lot(transactions + later.transaction, "b1").remainingQuantity)
    }

    @Test
    fun aReversal_datedBeforeItsOriginal_ofAReversal_aSecondOne_orOfACashLine_isRefused() {
        val split = splitOn("x1", "2026-03-01", 2, 1, 3L)
        val transactions = listOf(tenFor1000, split)
        assertEquals(ReversalRefusal.BeforeOriginal(LocalDate.of(2026, 3, 1)), refusal(reverse(transactions, "x1", "2026-02-28")))
        val first = created(reverse(transactions, "x1", "2026-03-01")).transaction
        assertEquals(ReversalRefusal.NotReversible, refusal(reverse(transactions + first, "x1", "2026-03-02")))
        assertEquals(ReversalRefusal.NotReversible, refusal(reverse(transactions + first, first.id, "2026-03-02")))
        assertEquals(ReversalRefusal.NotFound, refusal(reverse(transactions, "c-b1", "2026-03-02")))
        assertEquals(ReversalRefusal.NotFound, refusal(reverse(transactions, "zz", "2026-03-02")))
    }

    @Test
    fun theReversalsDefault_onTheOriginalsDate_comesAfterItInEntryOrder() {
        val result = created(reverse(listOf(tenFor1000), "b1", "2026-01-02"))
        assertEquals(LocalDate.of(2026, 1, 2), result.transaction.date)
        assertEquals(3L, result.transaction.enteredSeq)
        assertEquals(Fraction.ZERO, lot(listOf(tenFor1000, result.transaction), "b1").remainingQuantity)
    }

    @Test
    fun aBuysReversal_beforeALaterSplit_removesTheQuantityAsTraded_andTheSplitAppliesToNothing() {
        val split = splitOn("x1", "2026-03-01", 2, 1, 3L)
        val transactions = listOf(tenFor1000, split)
        val result = created(reverse(transactions, "b1", "2026-02-01"))
        val after = transactions + result.transaction
        assertEquals(ReversalEffect.OfBuy(sharesRemoved = fraction(10), cashCents = 100_000L), Reversals.effect(after, result.transaction))
        assertEquals(Fraction.ZERO, lot(after, "b1").remainingQuantity)
    }

    @Test
    fun afterASellsReversal_itsLotCanBePickedAgain_butABackdatedSellSeesTheLeastItHoldsOnItsDateOrLater() {
        val sale = sold("s1", "2026-03-01", "10", 120_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val reversal = created(reverse(listOf(tenFor1000, sale), "s1", "2026-04-01")).transaction
        val transactions = listOf(tenFor1000, sale, reversal)
        // On Feb 1 the lot holds 10, but it falls to 0 on Mar 1 before the reversal puts it back.
        assertEquals(emptyList<LotChoice>(), Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 2, 1), sellSeq = 5L))
        val may = Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 5, 1), sellSeq = 5L).single()
        assertEquals(fraction(10), may.available)
        val again =
            Sells.sell(
                cashEntries = listOf(opening()) + linesOf(listOf(tenFor1000, sale)),
                transactions = transactions,
                portfolios = listOf(portfolioP),
                transactionId = "s2",
                cashEntryId = "c-s2",
                portfolioId = "p1",
                ticker = "AAPL",
                date = LocalDate.of(2026, 5, 1),
                quantity = BigDecimal("10"),
                totalCents = 130_000L,
                picks = listOf(Pick("b1", BigDecimal("10"))),
            )
        assertTrue(again is SellResult.Created)
    }

    // Rounding (CLAUDE.md pitfall from 12): remainders that round up and down, reversed back to exact figures.
    @Test
    fun aSellAll_ofLotsLeftBy1For3And1For7_reversed_thenTheSplitsReversed_givesTheExactOriginalQuantities() {
        val five = bought("b1", "2026-01-02", "5", 50_000L, 2L)
        val three = bought("b2", "2026-01-03", "3", 30_000L, 3L)
        val oneForThree = splitOn("x1", "2026-02-01", 1, 3, 4L)
        val oneForSeven = splitOn("x2", "2026-02-02", 1, 7, 5L)
        var transactions = listOf(five, three, oneForThree, oneForSeven)
        // 5/21 (0.23809524, rounded up) and 1/7 (0.14285714, rounded down).
        val picks = Sells.sellAll(Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 3, 1), sellSeq = 6L))
        assertEquals(listOf(Pick("b1", BigDecimal("0.23809524")), Pick("b2", BigDecimal("0.14285714"))), picks)
        val sale =
            (
                Sells.sell(
                    cashEntries = listOf(opening()) + linesOf(transactions),
                    transactions = transactions,
                    portfolios = listOf(portfolioP),
                    transactionId = "s1",
                    cashEntryId = "c-s1",
                    portfolioId = "p1",
                    ticker = "AAPL",
                    date = LocalDate.of(2026, 3, 1),
                    quantity = BigDecimal("0.38095238"),
                    totalCents = 90_000L,
                    picks = picks,
                ) as SellResult.Created
                ).transaction
        transactions = transactions + sale
        assertEquals(listOf(Fraction.ZERO, Fraction.ZERO), Holdings.lots(transactions, day).map { it.remainingQuantity })
        listOf("s1" to "2026-04-01", "x2" to "2026-04-02", "x1" to "2026-04-03").forEach { (id, date) ->
            transactions = transactions + created(reverse(transactions, id, date)).transaction
        }
        assertEquals(listOf(fraction(5), fraction(3)), Holdings.lots(transactions, day).map { it.remainingQuantity })
        assertEquals(listOf(fraction(100), fraction(100)), Holdings.lots(transactions, day).map { it.costPerShare })
    }

    @Test
    fun reverseIsOffered_onABuySellOrSplit_notReversed_andNeverOnAReversal() {
        val first = created(reverse(listOf(tenFor1000), "b1", "2026-01-02")).transaction
        val transactions = listOf(tenFor1000, first)
        assertEquals(false, Reversals.offered(transactions, tenFor1000))
        assertEquals(false, Reversals.offered(transactions, first))
        assertEquals(first, Reversals.reversalOf(transactions, "b1"))
        assertEquals(tenFor1000, Reversals.originalOf(transactions, first))
        assertEquals(true, Reversals.offered(listOf(tenFor1000), tenFor1000))
    }
}
