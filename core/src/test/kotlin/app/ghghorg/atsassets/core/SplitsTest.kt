package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class SplitsTest {
    private val portfolioP = Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L)
    private val portfolioQ = Portfolio(id = "p2", name = "Portfolio Q", createdSeq = 2L)

    private val opening = CashEntry("e1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)

    private fun bought(
        id: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        ticker: String = "AAPL",
        portfolioId: String = "p1",
    ): Transaction = Transaction(
        id = id,
        portfolioId = portfolioId,
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

    private fun fraction(
        numerator: Long,
        denominator: Long = 1L,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    private fun split(
        transactions: List<Transaction>,
        date: String,
        to: Int,
        from: Int,
        ticker: String = "AAPL",
        portfolioId: String = "p1",
        cashEntries: List<CashEntry> = listOf(opening),
    ): SplitResult = Splits.split(
        cashEntries = cashEntries,
        transactions = transactions,
        portfolios = listOf(portfolioP, portfolioQ),
        transactionId = "t-split",
        portfolioId = portfolioId,
        ticker = ticker,
        date = LocalDate.parse(date),
        ratio = SplitRatio(to, from),
    )

    private fun created(result: SplitResult): Transaction = (result as SplitResult.Created).transaction

    private fun refusal(result: SplitResult): SplitRefusal = (result as SplitResult.Refused).reason

    private fun sell(
        transactions: List<Transaction>,
        date: String,
        quantity: String,
        totalCents: Long,
        picks: List<Pick>,
    ): SellResult = Sells.sell(
        cashEntries = listOf(opening),
        transactions = transactions,
        portfolios = listOf(portfolioP, portfolioQ),
        transactionId = "s-new",
        cashEntryId = "c-new",
        portfolioId = "p1",
        ticker = "AAPL",
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        picks = picks,
    )

    private val day = LocalDate.of(2026, 12, 31)

    private val tenFor1000 = bought("b1", "2026-01-02", "10", 100_000L, 2L)

    @Test
    fun exampleB_twoForOne_doublesTheShares_keepsTheCost_andAForgottenEarlierBuyCountsSplit() {
        val split = created(split(listOf(tenFor1000), "2026-03-01", 2, 1))
        assertEquals(TransactionType.SPLIT, split.type)
        assertEquals(SplitRatio(2, 1), split.split)
        assertEquals(3L, split.enteredSeq)
        val lot = Holdings.lots(listOf(tenFor1000, split), day).single()
        assertEquals(fraction(20), lot.remainingQuantity)
        assertEquals(fraction(1_000), lot.cost)
        assertEquals(fraction(50), lot.costPerShare)
        // The forgotten Feb 15 buy, typed as traded (5 for $500) and entered after the split.
        val forgotten = bought("b2", "2026-02-15", "5", 50_000L, 4L)
        val lots = Holdings.lots(listOf(tenFor1000, split, forgotten), day)
        assertEquals(listOf(fraction(20), fraction(10)), lots.map { it.remainingQuantity })
        assertEquals(fraction(50), lots[1].costPerShare)
        assertEquals(fraction(500), lots[1].cost)
        val holding = Holdings.holdings(listOf(tenFor1000, split, forgotten), day).single()
        assertEquals(fraction(30), holding.shares)
        assertEquals(fraction(1_500), holding.cost)
    }

    @Test
    fun aBuyOnTheSplitDate_enteredAfterIt_staysAsTraded_andOneEnteredBefore_isSplit() {
        val before = bought("b2", "2026-03-01", "4", 40_000L, 3L)
        val split = created(split(listOf(tenFor1000, before), "2026-03-01", 2, 1))
        val after = bought("b3", "2026-03-01", "5", 50_000L, 5L)
        val lots = Holdings.lots(listOf(tenFor1000, before, split, after), day)
        assertEquals(listOf(fraction(20), fraction(8), fraction(5)), lots.map { it.remainingQuantity })
        assertEquals(fraction(100), lots[2].costPerShare)
    }

    @Test
    fun aSplit_touchesOnlyItsOwnPortfolioAndTicker_andLotsBoughtBeforeIt() {
        val spy = bought("b2", "2026-01-02", "10", 100_000L, 3L, ticker = "SPY")
        val inQ = bought("b3", "2026-01-02", "10", 100_000L, 4L, portfolioId = "p2")
        val later = bought("b4", "2026-04-01", "10", 100_000L, 5L)
        val transactions = listOf(tenFor1000, spy, inQ, later)
        val split = created(split(transactions, "2026-03-01", 3, 1))
        val lots = Holdings.lots(transactions + split, day).associate { it.buyId to it.remainingQuantity }
        assertEquals(mapOf("b1" to fraction(30), "b2" to fraction(10), "b3" to fraction(10), "b4" to fraction(10)), lots)
    }

    @Test
    fun aSplitDatedLater_countsFromItsDateOnly() {
        val split = created(split(listOf(tenFor1000), "2026-03-01", 2, 1))
        assertEquals(fraction(10), Holdings.lots(listOf(tenFor1000, split), LocalDate.of(2026, 2, 28)).single().remainingQuantity)
        assertEquals(fraction(20), Holdings.lots(listOf(tenFor1000, split), LocalDate.of(2026, 3, 1)).single().remainingQuantity)
    }

    @Test
    fun criterion59a_oneForThree_onTenFor1000_gives10Over3Shares_shownTo8Places_cost1000_perShareExactly300() {
        val split = created(split(listOf(tenFor1000), "2026-03-01", 1, 3))
        val lot = Holdings.lots(listOf(tenFor1000, split), day).single()
        assertEquals(fraction(10, 3), lot.remainingQuantity)
        assertEquals(BigDecimal("3.33333333"), Sells.shown(lot.remainingQuantity))
        assertEquals(fraction(1_000), lot.cost)
        assertEquals(fraction(300), lot.costPerShare)
    }

    @Test
    fun criterion59c_aPickOf3_33333333_takesTheExact10Over3_leavesExactlyZero_andLoses600For400() {
        val split = created(split(listOf(tenFor1000), "2026-03-01", 1, 3))
        val transactions = listOf(tenFor1000, split)
        val choice = Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 4, 1), sellSeq = 4L).single()
        assertEquals(fraction(10, 3), choice.available)
        assertEquals(fraction(300), choice.costPerShare)
        val sale = sell(transactions, "2026-04-01", "3.33333333", 40_000L, listOf(Pick("b1", BigDecimal("3.33333333"))))
        val sell = (sale as SellResult.Created).transaction
        assertEquals(listOf(SellAllocation("b1", fraction(10, 3))), sell.allocations)
        assertEquals(Fraction.ZERO, Holdings.holdings(transactions + sell, day).single().shares)
        assertEquals(fraction(-600), Sells.realizedGain(Sells.pieces(transactions + sell, sell)))
    }

    @Test
    fun criterion59d_withTheSplit_sellAllOnTwoSuchLots_sells6_66666666_andEmptiesBoth() {
        val second = bought("b2", "2026-01-03", "10", 100_000L, 3L)
        val split = created(split(listOf(tenFor1000, second), "2026-03-01", 1, 3))
        val transactions = listOf(tenFor1000, second, split)
        val picks = Sells.sellAll(Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 4, 1), sellSeq = 5L))
        val quantity = picks.fold(BigDecimal.ZERO) { sum, it -> sum + it.quantity }
        assertEquals(BigDecimal("6.66666666"), quantity)
        val sell = (sell(transactions, "2026-04-01", quantity.toPlainString(), 80_000L, picks) as SellResult.Created).transaction
        assertEquals(listOf(Fraction.ZERO, Fraction.ZERO), Holdings.lots(transactions + sell, day).map { it.remainingQuantity })
    }

    @Test
    fun aSellAfterASplit_isInPostSplitShares_andItsPiecesCostThePostSplitPrice() {
        val split = created(split(listOf(tenFor1000), "2026-03-01", 2, 1))
        val transactions = listOf(tenFor1000, split)
        val sale = sell(transactions, "2026-04-01", "15", 90_000L, listOf(Pick("b1", BigDecimal("15"))))
        val sell = (sale as SellResult.Created).transaction
        assertEquals(listOf(SellAllocation("b1", fraction(15))), sell.allocations)
        assertEquals(fraction(5), Holdings.lots(transactions + sell, day).single().remainingQuantity)
        assertEquals(fraction(250), Holdings.lots(transactions + sell, day).single().cost)
        val piece = Sells.pieces(transactions + sell, sell).single()
        assertEquals(fraction(750), piece.cost)
        assertEquals(fraction(150), piece.gain)
    }

    @Test
    fun aSellOfMoreThanThePostSplitHolding_isRefused_withTheHeldFigureInPostSplitShares() {
        val split = created(split(listOf(tenFor1000), "2026-03-01", 2, 1))
        val result = sell(listOf(tenFor1000, split), "2026-04-01", "21", 90_000L, listOf(Pick("b1", BigDecimal("21"))))
        assertEquals(SellRefusal.MoreThanHeld(LocalDate.of(2026, 4, 1), fraction(20), BigDecimal("21")), (result as SellResult.Refused).reason)
    }

    @Test
    fun aBackdatedSellBeforeASplit_offersTheLotInTheUnitsOfItsOwnDate() {
        val split = created(split(listOf(tenFor1000), "2026-03-01", 2, 1))
        val april = sold("s1", "2026-04-01", "5", 40_000L, 4L, listOf(SellAllocation("b1", fraction(5))))
        val transactions = listOf(tenFor1000, split, april)
        // On Feb 1 the lot holds 10 shares; the 5 post-split shares sold in April are 2.5 of them.
        val choice = Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 2, 1), sellSeq = 5L).single()
        assertEquals(fraction(15, 2), choice.available)
        assertEquals(fraction(100), choice.costPerShare)
        val over = sell(transactions, "2026-02-01", "8", 80_000L, listOf(Pick("b1", BigDecimal("8"))))
        assertEquals(SellRefusal.MoreThanHeld(LocalDate.of(2026, 2, 1), fraction(15, 2), BigDecimal("8")), (over as SellResult.Refused).reason)
        val fits = sell(transactions, "2026-02-01", "7.5", 75_000L, listOf(Pick("b1", BigDecimal("7.5"))))
        val feb = (fits as SellResult.Created).transaction
        assertEquals(Fraction.ZERO, Holdings.lots(transactions + feb, day).single().remainingQuantity)
    }

    @Test
    fun aReverseSplit_thatLeavesALaterSellTakingMoreThanItsLotHolds_isRefused_namingThatSellsDate() {
        val april = sold("s1", "2026-04-01", "5", 40_000L, 3L, listOf(SellAllocation("b1", fraction(5))))
        val transactions = listOf(tenFor1000, april)
        assertEquals(SplitRefusal.SellShort(LocalDate.of(2026, 4, 1)), refusal(split(transactions, "2026-03-01", 1, 10)))
        // 1 for 2 leaves exactly the 5 the sell took.
        val split = created(split(transactions, "2026-03-01", 1, 2))
        assertEquals(Fraction.ZERO, Holdings.lots(transactions + split, day).single().remainingQuantity)
        // A reverse split dated after the sell changes nothing it took.
        val later = created(split(transactions, "2026-05-01", 1, 10))
        assertEquals(fraction(1, 2), Holdings.lots(transactions + later, day).single().remainingQuantity)
    }

    @Test
    fun theRatio_isTwoWholeNumbersFrom1To1000000_thatAreNotEqual() {
        val buys = listOf(tenFor1000)
        assertEquals(SplitRefusal.RatioOutOfRange, refusal(split(buys, "2026-03-01", 0, 1)))
        assertEquals(SplitRefusal.RatioOutOfRange, refusal(split(buys, "2026-03-01", 2, 0)))
        assertEquals(SplitRefusal.RatioOutOfRange, refusal(split(buys, "2026-03-01", 1_000_001, 1)))
        assertEquals(SplitRefusal.RatioOutOfRange, refusal(split(buys, "2026-03-01", -2, 1)))
        assertEquals(SplitRefusal.RatioEqual, refusal(split(buys, "2026-03-01", 2, 2)))
        assertEquals(fraction(10_000_000), Holdings.lots(buys + created(split(buys, "2026-03-01", 1_000_000, 1)), day).single().remainingQuantity)
    }

    @Test
    fun theTicker_mustHaveABuyInThatPortfolio_andTheOtherRulesComeInOrder() {
        val buys = listOf(tenFor1000)
        assertEquals(SplitRefusal.NoOpeningBalance, refusal(split(buys, "2026-03-01", 2, 1, cashEntries = emptyList())))
        assertEquals(SplitRefusal.PortfolioNotFound, refusal(split(buys, "2026-03-01", 2, 1, portfolioId = "p9")))
        assertEquals(SplitRefusal.TickerNotBought, refusal(split(buys, "2026-03-01", 2, 1, ticker = "SPY")))
        assertEquals(SplitRefusal.TickerNotBought, refusal(split(buys, "2026-03-01", 2, 1, portfolioId = "p2")))
        assertEquals(SplitRefusal.TickerNotBought, refusal(split(buys, "2026-03-01", 0, 1, ticker = " ")))
        assertEquals(
            SplitRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 1)),
            refusal(split(buys, "2025-12-31", 2, 1)),
        )
        assertEquals("AAPL", created(split(buys, "2026-03-01", 2, 1, ticker = " aapl ")).ticker)
    }

    @Test
    fun theTickers_offered_areThoseWithABuyInThePortfolio_inTickerOrder() {
        val transactions =
            listOf(
                bought("b1", "2026-01-02", "1", 100L, 2L, ticker = "SPY"),
                bought("b2", "2026-01-02", "1", 100L, 3L, ticker = "AAPL"),
                bought("b3", "2026-01-02", "1", 100L, 4L, ticker = "MSFT", portfolioId = "p2"),
                bought("b4", "2026-01-03", "1", 100L, 5L, ticker = "SPY"),
            )
        assertEquals(listOf("AAPL", "SPY"), Splits.tickers(transactions, "p1"))
        assertEquals(listOf("MSFT"), Splits.tickers(transactions, "p2"))
    }

    @Test
    fun theReview_showsTheSharesHeldAtThatPoint_beforeAndAfter() {
        val sameDaySell = sold("s1", "2026-03-01", "4", 40_000L, 3L, listOf(SellAllocation("b1", fraction(4))))
        val transactions = listOf(tenFor1000, sameDaySell)
        val split = created(split(transactions, "2026-03-01", 2, 1))
        assertEquals(SplitEffect(fraction(6), fraction(12)), Splits.effect(transactions + split, split))
        val exampleB = created(split(listOf(tenFor1000), "2026-03-01", 2, 1))
        assertEquals(SplitEffect(fraction(10), fraction(20)), Splits.effect(listOf(tenFor1000, exampleB), exampleB))
    }

    @Test
    fun aSplitAtZeroShares_isAllowed_soldOutOrBeforeTheFirstBuy() {
        val soldOut = sold("s1", "2026-02-01", "10", 100_000L, 3L, listOf(SellAllocation("b1", fraction(10))))
        val afterSale = created(split(listOf(tenFor1000, soldOut), "2026-03-01", 2, 1))
        assertEquals(SplitEffect(Fraction.ZERO, Fraction.ZERO), Splits.effect(listOf(tenFor1000, soldOut, afterSale), afterSale))
        val early = created(split(listOf(tenFor1000), "2026-01-01", 2, 1))
        assertEquals(SplitEffect(Fraction.ZERO, Fraction.ZERO), Splits.effect(listOf(tenFor1000, early), early))
        assertEquals(fraction(10), Holdings.lots(listOf(tenFor1000, early), day).single().remainingQuantity)
    }

    @Test
    fun aSplit_hasNoCashEffect_andLeavesNetWorthAtCost() {
        val cash = listOf(opening, CashEntry("c-b1", LocalDate.of(2026, 1, 2), -100_000L, CashType.TRADE, "", 2L, transactionId = "b1"))
        val before = Valuation.netWorth(cash, listOf(tenFor1000), day)
        val split = created(split(listOf(tenFor1000), "2026-03-01", 1, 3, cashEntries = cash))
        assertEquals(before, Valuation.netWorth(cash, listOf(tenFor1000, split), day))
        assertNull(split.reversesId)
        assertEquals(0L, split.totalCents)
    }
}
