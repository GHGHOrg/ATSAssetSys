package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class SellsTest {
    private val portfolioP = Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L)
    private val portfolioQ = Portfolio(id = "p2", name = "Portfolio Q", createdSeq = 2L)

    private fun opening(cents: Long = 1_000_000L): CashEntry = CashEntry("e1", LocalDate.of(2026, 1, 1), cents, CashType.OPENING_BALANCE, "", 1L)

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

    /** The trade line of a buy (cash out) or a sell (cash in), as K1 makes it. */
    private fun line(transaction: Transaction): CashEntry = CashEntry(
        id = "c-${transaction.id}",
        date = transaction.date,
        amountCents = if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        type = CashType.TRADE,
        note = "",
        enteredSeq = transaction.enteredSeq,
        transactionId = transaction.id,
    )

    private fun fraction(
        numerator: Long,
        denominator: Long = 1L,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    private fun pick(
        lotId: String,
        quantity: String,
    ): Pick = Pick(lotId, BigDecimal(quantity))

    /** Example A's two buys: lot 1 (b1) and lot 2 (b2). */
    private val exampleA = listOf(bought("b1", "2026-01-02", "10", 100_000L, 2L), bought("b2", "2026-02-01", "10", 150_000L, 3L))

    private fun cashOf(transactions: List<Transaction>): List<CashEntry> = listOf(opening()) + transactions.map { line(it) }

    private fun sell(
        transactions: List<Transaction>,
        date: String,
        quantity: String,
        totalCents: Long,
        picks: List<Pick>,
        cashEntries: List<CashEntry> = cashOf(transactions),
        portfolioId: String = "p1",
        ticker: String = "AAPL",
    ): SellResult = Sells.sell(
        cashEntries = cashEntries,
        transactions = transactions,
        portfolios = listOf(portfolioP, portfolioQ),
        transactionId = "s-new",
        cashEntryId = "c-new",
        portfolioId = portfolioId,
        ticker = ticker,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        picks = picks,
    )

    private fun created(result: SellResult): SellResult.Created = result as SellResult.Created

    private fun refusal(result: SellResult): SellRefusal = (result as SellResult.Refused).reason

    @Test
    fun exampleA_aSellOf12_picking10And2_costs1300_gains800_andLeavesLot2With8() {
        val result = created(sell(exampleA, "2026-03-01", "12", 210_000L, listOf(pick("b1", "10"), pick("b2", "2"))))
        val sell = result.transaction
        assertEquals(TransactionType.SELL, sell.type)
        assertEquals("s-new", sell.id)
        assertEquals("AAPL", sell.ticker)
        assertEquals(BigDecimal("12"), sell.quantity)
        assertEquals(210_000L, sell.totalCents)
        assertEquals(4L, sell.enteredSeq)
        assertEquals(listOf(SellAllocation("b1", fraction(10)), SellAllocation("b2", fraction(2))), sell.allocations)
        val cashLine = result.cashLine
        assertEquals(CashType.TRADE, cashLine.type)
        assertEquals(210_000L, cashLine.amountCents)
        assertEquals("s-new", cashLine.transactionId)
        assertEquals(4L, cashLine.enteredSeq)
        val after = exampleA + sell
        assertEquals(960_000L, CashRules.balanceOn(cashOf(exampleA) + cashLine, LocalDate.of(2026, 3, 1)))
        val pieces = Sells.pieces(after, sell)
        assertEquals(listOf(fraction(1_000), fraction(300)), pieces.map { it.cost })
        assertEquals(fraction(1_300), pieces.fold(Fraction.ZERO) { sum, piece -> sum + piece.cost })
        assertEquals(fraction(800), Sells.realizedGain(pieces))
        val lots = Holdings.lotsOf(after, "p1", "AAPL", LocalDate.of(2026, 3, 1))
        assertEquals(listOf("b2"), lots.map { it.buyId })
        assertEquals(fraction(8), lots[0].remainingQuantity)
        assertEquals(fraction(1_200), lots[0].cost)
    }

    @Test
    fun lotChoices_areOldestFirst_withWhatIsLeftAndTheCostPerShare() {
        val choices = Sells.lotChoices(exampleA, "p1", "AAPL", LocalDate.of(2026, 3, 1), sellSeq = 4L)
        assertEquals(listOf("b1", "b2"), choices.map { it.buyId })
        assertEquals(listOf(fraction(10), fraction(10)), choices.map { it.available })
        assertEquals(listOf(fraction(100), fraction(150)), choices.map { it.costPerShare })
        assertEquals(listOf<LotUnavailable?>(null, null), choices.map { it.unavailable })
    }

    @Test
    fun lotChoices_aLotBoughtAfterTheSellDate_staysInItsPlace_greyedOut() {
        val choices = Sells.lotChoices(exampleA, "p1", "AAPL", LocalDate.of(2026, 1, 15), sellSeq = 4L)
        assertEquals(listOf("b1", "b2"), choices.map { it.buyId })
        assertEquals(listOf(null, LotUnavailable.BOUGHT_AFTER_SELL_DATE), choices.map { it.unavailable })
    }

    @Test
    fun lotChoices_aSameDateLotEnteredAfterTheSell_isGreyedOut() {
        val choices = Sells.lotChoices(exampleA, "p1", "AAPL", LocalDate.of(2026, 2, 1), sellSeq = 3L)
        assertEquals(listOf(null, LotUnavailable.ENTERED_AFTER_SELL), choices.map { it.unavailable })
    }

    @Test
    fun lotChoices_leaveOutOtherHoldings_andLotsWithNothingLeft() {
        val transactions =
            exampleA +
                bought("x1", "2026-01-02", "5", 10_000L, 4L, ticker = "SPY") +
                bought("x2", "2026-01-02", "5", 10_000L, 5L, portfolioId = "p2") +
                sold("s1", "2026-02-15", "10", 120_000L, 6L, listOf(SellAllocation("b1", fraction(10))))
        val choices = Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 3, 1), sellSeq = 7L)
        assertEquals(listOf("b2"), choices.map { it.buyId })
    }

    @Test
    fun criterion60d_aBackdatedSell_doesNotOfferALotSoldLater_andRefusesAPickOfIt() {
        val transactions = exampleA + sold("s1", "2026-03-01", "10", 120_000L, 4L, listOf(SellAllocation("b1", fraction(10))))
        val choices = Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 2, 15), sellSeq = 5L)
        assertEquals(listOf("b2"), choices.map { it.buyId })
        assertEquals(fraction(10), choices[0].available)
        assertEquals(
            SellRefusal.PickOverRemainder("b1", Fraction.ZERO),
            refusal(sell(transactions, "2026-02-15", "5", 60_000L, listOf(pick("b1", "5")))),
        )
        assertEquals(
            listOf(SellAllocation("b2", fraction(5))),
            created(sell(transactions, "2026-02-15", "5", 60_000L, listOf(pick("b2", "5")))).transaction.allocations,
        )
    }

    @Test
    fun sell_aPickOfALotBoughtAfterTheSellDate_isRefused() {
        assertEquals(
            SellRefusal.PickNotAvailable("b2", LotUnavailable.BOUGHT_AFTER_SELL_DATE),
            refusal(sell(exampleA, "2026-01-15", "2", 30_000L, listOf(pick("b2", "2")))),
        )
    }

    @Test
    fun sell_aPickOfAnotherHoldingsLot_orAnUnknownLot_orTheSameLotTwice_isRefused() {
        val transactions = exampleA + bought("x1", "2026-01-02", "5", 10_000L, 4L, portfolioId = "p2")
        assertEquals(SellRefusal.PickNotALot("x1"), refusal(sell(transactions, "2026-03-01", "2", 30_000L, listOf(pick("x1", "2")))))
        assertEquals(SellRefusal.PickNotALot("zz"), refusal(sell(transactions, "2026-03-01", "2", 30_000L, listOf(pick("zz", "2")))))
        assertEquals(
            SellRefusal.PickNotALot("b1"),
            refusal(sell(transactions, "2026-03-01", "2", 30_000L, listOf(pick("b1", "1"), pick("b1", "1")))),
        )
    }

    @Test
    fun sell_aPickOverTheLotsRemainder_isRefused_andAPickOfZeroToo() {
        assertEquals(
            SellRefusal.PickOverRemainder("b1", fraction(10)),
            refusal(sell(exampleA, "2026-03-01", "11", 30_000L, listOf(pick("b1", "11")))),
        )
        assertEquals(
            SellRefusal.PickNotPositive("b2"),
            refusal(sell(exampleA, "2026-03-01", "1", 30_000L, listOf(pick("b1", "1"), pick("b2", "0")))),
        )
    }

    @Test
    fun sell_aPickEqualToTheRemainderAt8Places_takesTheExactRemainder() {
        // A hand-made lot of 10/3 shares: 20/3 of b1's 10 were taken by an earlier sell (a split makes this real in 12).
        val transactions = exampleA + sold("s1", "2026-02-15", "6.66666667", 100_000L, 4L, listOf(SellAllocation("b1", fraction(20, 3))))
        val choices = Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 3, 1), sellSeq = 5L)
        assertEquals(fraction(10, 3), choices[0].available)
        assertEquals(
            SellRefusal.PickOverRemainder("b1", fraction(10, 3)),
            refusal(sell(transactions, "2026-03-01", "3.33333334", 40_000L, listOf(pick("b1", "3.33333334")))),
        )
        val sell = created(sell(transactions, "2026-03-01", "3.33333333", 40_000L, listOf(pick("b1", "3.33333333")))).transaction
        assertEquals(BigDecimal("3.33333333"), sell.quantity)
        assertEquals(listOf(SellAllocation("b1", fraction(10, 3))), sell.allocations)
        val lots = Holdings.lots(transactions + sell, LocalDate.of(2026, 3, 1))
        assertEquals(Fraction.ZERO, lots.first { it.buyId == "b1" }.remainingQuantity)
        val partial = created(sell(transactions, "2026-03-01", "3.3333333", 40_000L, listOf(pick("b1", "3.3333333")))).transaction
        assertEquals(listOf(SellAllocation("b1", Fraction.of(BigDecimal("3.3333333")))), partial.allocations)
    }

    @Test
    fun sell_picksThatDoNotAddUp_areRefused_namingBothNumbers() {
        assertEquals(
            SellRefusal.PicksDoNotAddUp(BigDecimal("10"), BigDecimal("12")),
            refusal(sell(exampleA, "2026-03-01", "12", 210_000L, listOf(pick("b1", "10")))),
        )
        assertEquals(
            SellRefusal.PicksDoNotAddUp(BigDecimal.ZERO, BigDecimal("12")),
            refusal(sell(exampleA, "2026-03-01", "12", 210_000L, emptyList())),
        )
    }

    @Test
    fun sell_moreThanThePortfolioHolds_isRefused_withWhatItHoldsOnThatDate() {
        assertEquals(
            SellRefusal.MoreThanHeld(LocalDate.of(2026, 3, 1), fraction(20), BigDecimal("21")),
            refusal(sell(exampleA, "2026-03-01", "21", 210_000L, listOf(pick("b1", "10"), pick("b2", "10")))),
        )
        assertEquals(
            SellRefusal.MoreThanHeld(LocalDate.of(2026, 1, 15), fraction(10), BigDecimal("12")),
            refusal(sell(exampleA, "2026-01-15", "12", 210_000L, listOf(pick("b1", "10")))),
        )
    }

    @Test
    fun sell_aTotalOfZero_isAllowed_aNegativeTotalOrAQuantityOfZeroIsNot() {
        val loss = created(sell(exampleA, "2026-03-01", "10", 0L, listOf(pick("b1", "10"))))
        assertEquals(0L, loss.cashLine.amountCents)
        assertEquals(fraction(-1_000), Sells.realizedGain(Sells.pieces(exampleA + loss.transaction, loss.transaction)))
        assertEquals(SellRefusal.TotalNegative, refusal(sell(exampleA, "2026-03-01", "10", -1L, listOf(pick("b1", "10")))))
        assertEquals(SellRefusal.QuantityNotPositive, refusal(sell(exampleA, "2026-03-01", "0", 100L, emptyList())))
    }

    @Test
    fun sell_theFirstRuleThatFails_decides() {
        assertEquals(
            SellRefusal.NoOpeningBalance,
            refusal(sell(exampleA, "2026-03-01", "1", 100L, listOf(pick("b1", "1")), cashEntries = emptyList())),
        )
        assertEquals(
            SellRefusal.PortfolioNotFound,
            refusal(sell(exampleA, "2026-03-01", "1", 100L, listOf(pick("b1", "1")), portfolioId = "p9")),
        )
        assertEquals(SellRefusal.TickerEmpty, refusal(sell(exampleA, "2026-03-01", "1", 100L, listOf(pick("b1", "1")), ticker = " ")))
        assertEquals(
            SellRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 1)),
            refusal(sell(exampleA, "2025-12-31", "1", 100L, listOf(pick("b1", "1")))),
        )
    }

    @Test
    fun aFutureDatedSell_countsFromItsDateOnly() {
        val sale = created(sell(exampleA, "2026-07-01", "10", 120_000L, listOf(pick("b1", "10"))))
        val transactions = exampleA + sale.transaction
        val cash = cashOf(exampleA) + sale.cashLine
        val june = LocalDate.of(2026, 6, 1)
        val july = LocalDate.of(2026, 7, 1)
        assertEquals(fraction(20), Holdings.holdings(transactions, june)[0].shares)
        assertEquals(fraction(10), Holdings.holdings(transactions, july)[0].shares)
        assertEquals(fraction(10_000), Valuation.netWorth(cash, transactions, june))
        assertEquals(fraction(10_200), Valuation.netWorth(cash, transactions, july))
    }

    @Test
    fun criterion59b_threeLotsSoldFor100_splitTheProceedsAndGainsByLargestRemainder() {
        val transactions =
            listOf(
                bought("b1", "2026-01-02", "1", 1_000L, 2L),
                bought("b2", "2026-01-03", "1", 1_000L, 3L),
                bought("b3", "2026-01-04", "1", 1_000L, 4L),
            )
        val picks = Sells.sellAll(Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 2, 1), sellSeq = 5L))
        val sell = created(sell(transactions, "2026-02-01", "3", 10_000L, picks)).transaction
        val pieces = Sells.pieces(transactions + sell, sell)
        assertEquals(listOf(3_334L, 3_333L, 3_333L), Money.partsInCents(pieces.map { it.proceeds }))
        assertEquals(listOf(2_334L, 2_333L, 2_333L), Money.partsInCents(pieces.map { it.gain }))
        assertEquals(fraction(70), Sells.realizedGain(pieces))
    }

    @Test
    fun criterion59d_sellAll_fillsTheQuantityWithTheSumOfThePicks_andEmptiesEveryLot() {
        // Two hand-made lots of 10/3 each (the split of 59(d) comes in 12).
        val transactions =
            exampleA +
                sold(
                    "s1",
                    "2026-02-15",
                    "13.33333334",
                    100_000L,
                    4L,
                    listOf(SellAllocation("b1", fraction(20, 3)), SellAllocation("b2", fraction(20, 3))),
                )
        val picks = Sells.sellAll(Sells.lotChoices(transactions, "p1", "AAPL", LocalDate.of(2026, 3, 1), sellSeq = 5L))
        assertEquals(listOf(pick("b1", "3.33333333"), pick("b2", "3.33333333")), picks)
        val quantity = picks.fold(BigDecimal.ZERO) { sum, it -> sum + it.quantity }
        assertEquals(BigDecimal("6.66666666"), quantity)
        val sell = created(sell(transactions, "2026-03-01", quantity.toPlainString(), 80_000L, picks)).transaction
        val lots = Holdings.lots(transactions + sell, LocalDate.of(2026, 3, 1))
        assertEquals(listOf(Fraction.ZERO, Fraction.ZERO), lots.map { it.remainingQuantity })
        assertEquals(Fraction.ZERO, Holdings.holdings(transactions + sell, LocalDate.of(2026, 3, 1))[0].shares)
    }

    @Test
    fun sellAll_picksOnlyTheLotsThatPassTheLotRules_wholeNumbersWithoutDecimals() {
        val picks = Sells.sellAll(Sells.lotChoices(exampleA, "p1", "AAPL", LocalDate.of(2026, 1, 15), sellSeq = 4L))
        assertEquals(listOf(pick("b1", "10")), picks)
        assertEquals("10", picks[0].quantity.toPlainString())
    }

    @Test
    fun pieces_areInLotOrder_whateverTheOrderOfThePicks() {
        val sell = created(sell(exampleA, "2026-03-01", "12", 210_000L, listOf(pick("b2", "2"), pick("b1", "10")))).transaction
        assertEquals(listOf("b1", "b2"), sell.allocations.map { it.lotId })
        val pieces = Sells.pieces(exampleA + sell, sell)
        assertEquals(listOf(fraction(1_750), fraction(350)), pieces.map { it.proceeds })
        assertNull(Sells.lotChoices(exampleA + sell, "p1", "AAPL", LocalDate.of(2026, 3, 2), sellSeq = 5L)[0].unavailable)
    }
}
