package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class HoldingsTest {
    private val today = LocalDate.of(2026, 6, 1)

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

    private fun dollars(value: Long): Fraction = Fraction.of(BigInteger.valueOf(value), BigInteger.ONE)

    private fun shares(text: String): Fraction = Fraction.of(BigDecimal(text))

    @Test
    fun aBuy_makesOneLot_withItsQuantityAndCost() {
        val lots = Holdings.lots(listOf(bought("b1", "2026-01-02", "10", 100_000L, 2L)), today)
        assertEquals(1, lots.size)
        val lot = lots[0]
        assertEquals("b1", lot.buyId)
        assertEquals("p1", lot.portfolioId)
        assertEquals("AAPL", lot.ticker)
        assertEquals(LocalDate.of(2026, 1, 2), lot.buyDate)
        assertEquals(BigDecimal("10"), lot.originalQuantity)
        assertEquals(shares("10"), lot.remainingQuantity)
        assertEquals(dollars(1_000), lot.cost)
    }

    @Test
    fun lots_areInTheOrderOfDateThenEntry() {
        val transactions =
            listOf(
                bought("b3", "2026-02-01", "1", 1_000L, 2L),
                bought("b2", "2026-01-02", "1", 1_000L, 5L),
                bought("b1", "2026-01-02", "1", 1_000L, 3L),
            )
        assertEquals(listOf("b1", "b2", "b3"), Holdings.lots(transactions, today).map { it.buyId })
    }

    @Test
    fun exampleA_twoBuys_makeOneHoldingWithTheSumOfSharesAndCost() {
        val transactions =
            listOf(
                bought("b1", "2026-01-02", "10", 100_000L, 2L),
                bought("b2", "2026-02-01", "10", 150_000L, 3L),
            )
        val holdings = Holdings.holdings(transactions, today)
        assertEquals(1, holdings.size)
        assertEquals("p1", holdings[0].portfolioId)
        assertEquals("AAPL", holdings[0].ticker)
        assertEquals(shares("20"), holdings[0].shares)
        assertEquals(dollars(2_500), holdings[0].cost)
    }

    @Test
    fun fractionalQuantities_addUpExactly() {
        val transactions =
            listOf(
                bought("b1", "2026-01-02", "0.1", 1_000L, 2L),
                bought("b2", "2026-01-03", "0.2", 1_000L, 3L),
            )
        assertEquals(shares("0.3"), Holdings.holdings(transactions, today)[0].shares)
    }

    @Test
    fun holdings_areOnePerPortfolioAndTicker_inTickerOrderWithinAPortfolio() {
        val transactions =
            listOf(
                bought("b1", "2026-01-02", "1", 1_000L, 2L, ticker = "SPY"),
                bought("b2", "2026-01-02", "1", 1_000L, 3L, ticker = "AAPL"),
                bought("b3", "2026-01-02", "1", 1_000L, 4L, ticker = "AAPL", portfolioId = "p2"),
            )
        val holdings = Holdings.holdings(transactions, today)
        assertEquals(listOf("p1" to "AAPL", "p1" to "SPY", "p2" to "AAPL"), holdings.map { it.portfolioId to it.ticker })
    }

    @Test
    fun aBuyDatedAfterToday_isLeftOutUntilItsDate() {
        val transactions =
            listOf(
                bought("b1", "2026-01-02", "1", 1_000L, 2L),
                bought("b2", "2026-06-02", "1", 1_000L, 3L),
            )
        assertEquals(shares("1"), Holdings.holdings(transactions, today)[0].shares)
        assertEquals(shares("2"), Holdings.holdings(transactions, LocalDate.of(2026, 6, 2))[0].shares)
        assertEquals(emptyList<Holding>(), Holdings.holdings(listOf(transactions[1]), today))
    }

    @Test
    fun netWorth_countsHoldingsAtCost() {
        val cashEntries =
            listOf(
                CashEntry("e1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L),
                CashEntry("c2", LocalDate.of(2026, 1, 2), -100_000L, CashType.TRADE, "", 2L, transactionId = "b1"),
            )
        val transactions = listOf(bought("b1", "2026-01-02", "10", 100_000L, 2L))
        assertEquals(dollars(10_000), Valuation.netWorth(cashEntries, transactions, today))
    }

    @Test
    fun netWorth_leavesOutABuyDatedAfterToday_bothItsCashAndItsCost() {
        val cashEntries =
            listOf(
                CashEntry("e1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L),
                CashEntry("c2", LocalDate.of(2026, 6, 2), -100_000L, CashType.TRADE, "", 2L, transactionId = "b1"),
            )
        val transactions = listOf(bought("b1", "2026-06-02", "10", 100_000L, 2L))
        assertEquals(dollars(10_000), Valuation.netWorth(cashEntries, transactions, today))
    }

    @Test
    fun portfolioValue_isTheCostOfItsHoldings_whileThereIsNoPrice() {
        val transactions =
            listOf(
                bought("b1", "2026-01-02", "10", 100_000L, 2L),
                bought("b2", "2026-01-03", "1", 50_000L, 3L, ticker = "SPY"),
                bought("b3", "2026-01-03", "1", 70_000L, 4L, portfolioId = "p2"),
            )
        assertEquals(dollars(1_500), Valuation.portfolioValue(transactions, "p1", today))
        assertEquals(dollars(700), Valuation.portfolioValue(transactions, "p2", today))
        assertEquals(Fraction.ZERO, Valuation.portfolioValue(transactions, "p3", today))
    }
}
