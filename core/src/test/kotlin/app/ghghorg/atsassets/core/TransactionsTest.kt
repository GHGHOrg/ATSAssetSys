package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class TransactionsTest {
    private val portfolioP = Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L)

    private fun cash(
        date: String,
        cents: Long,
        seq: Long,
        type: CashType = CashType.DEPOSIT,
    ): CashEntry = CashEntry(
        id = "e$seq",
        date = LocalDate.parse(date),
        amountCents = cents,
        type = type,
        note = "",
        enteredSeq = seq,
    )

    private fun opening(
        date: String = "2026-01-01",
        cents: Long = 1_000_000L,
    ): CashEntry = cash(date, cents, 1L, CashType.OPENING_BALANCE)

    private fun buy(
        cashEntries: List<CashEntry>,
        date: String,
        quantity: String,
        totalCents: Long,
        ticker: String = "AAPL",
        transactions: List<Transaction> = emptyList(),
        portfolios: List<Portfolio> = listOf(portfolioP),
        portfolioId: String = "p1",
    ): BuyResult = Transactions.buy(
        cashEntries = cashEntries,
        transactions = transactions,
        portfolios = portfolios,
        transactionId = "t-new",
        cashEntryId = "c-new",
        portfolioId = portfolioId,
        ticker = ticker,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
    )

    private fun created(result: BuyResult): BuyResult.Created = result as BuyResult.Created

    private fun refusal(result: BuyResult): BuyRefusal = (result as BuyResult.Refused).reason

    @Test
    fun buy_createsTheTransactionAndItsTradeLine_withOneSeq() {
        val result = created(buy(listOf(opening()), "2026-01-02", "10", 100_000L))
        val transaction = result.transaction
        assertEquals("t-new", transaction.id)
        assertEquals("p1", transaction.portfolioId)
        assertEquals("AAPL", transaction.ticker)
        assertEquals(TransactionType.BUY, transaction.type)
        assertEquals(LocalDate.of(2026, 1, 2), transaction.date)
        assertEquals(BigDecimal("10"), transaction.quantity)
        assertEquals(100_000L, transaction.totalCents)
        assertEquals(null, transaction.reversesId)
        val line = result.cashLine
        assertEquals("c-new", line.id)
        assertEquals(CashType.TRADE, line.type)
        assertEquals(-100_000L, line.amountCents)
        assertEquals(LocalDate.of(2026, 1, 2), line.date)
        assertEquals("t-new", line.transactionId)
        assertEquals("", line.note)
        assertEquals(2L, transaction.enteredSeq)
        assertEquals(2L, line.enteredSeq)
    }

    @Test
    fun buy_quantityIsKeptAsTyped() {
        val transaction = created(buy(listOf(opening()), "2026-01-02", "2.50", 100_000L)).transaction
        assertEquals(2, transaction.quantity.scale())
    }

    @Test
    fun buy_tickerIsTrimmedAndInCapitals() {
        assertEquals("BRK.B", created(buy(listOf(opening()), "2026-01-02", "1", 50_000L, ticker = "  brk.b ")).transaction.ticker)
    }

    @Test
    fun buy_onTheOpeningBalanceDate_isAllowed() {
        assertEquals(LocalDate.of(2026, 1, 1), created(buy(listOf(opening()), "2026-01-01", "1", 1_000L)).transaction.date)
    }

    @Test
    fun buy_ofAllTheCash_isAllowed() {
        assertEquals(-1_000_000L, created(buy(listOf(opening()), "2026-01-02", "1", 1_000_000L)).cashLine.amountCents)
    }

    @Test
    fun buy_withoutAnOpeningBalance_isRefused() {
        assertEquals(BuyRefusal.NoOpeningBalance, refusal(buy(emptyList(), "2026-01-02", "1", 1_000L)))
    }

    @Test
    fun buy_inAPortfolioThatDoesNotExist_isRefused() {
        assertEquals(BuyRefusal.PortfolioNotFound, refusal(buy(listOf(opening()), "2026-01-02", "1", 1_000L, portfolioId = "p9")))
    }

    @Test
    fun buy_withABlankTicker_isRefused() {
        assertEquals(BuyRefusal.TickerEmpty, refusal(buy(listOf(opening()), "2026-01-02", "1", 1_000L, ticker = "  ")))
    }

    @Test
    fun buy_ofZeroOrLessShares_isRefused() {
        assertEquals(BuyRefusal.QuantityNotPositive, refusal(buy(listOf(opening()), "2026-01-02", "0", 1_000L)))
        assertEquals(BuyRefusal.QuantityNotPositive, refusal(buy(listOf(opening()), "2026-01-02", "-1", 1_000L)))
    }

    @Test
    fun buy_withATotalOfZeroOrLess_isRefused() {
        assertEquals(BuyRefusal.TotalNotPositive, refusal(buy(listOf(opening()), "2026-01-02", "1", 0L)))
        assertEquals(BuyRefusal.TotalNotPositive, refusal(buy(listOf(opening()), "2026-01-02", "1", -1L)))
    }

    @Test
    fun buy_beforeTheOpeningBalance_isRefused_namingThatDate() {
        val result = buy(listOf(opening("2026-01-10")), "2026-01-09", "1", 1_000L)
        assertEquals(BuyRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 10)), refusal(result))
    }

    @Test
    fun buy_overTheCash_isRefused_namingTheDateAndShortfall() {
        val result = buy(listOf(opening(cents = 100_000L)), "2026-01-02", "1", 150_000L)
        assertEquals(BuyRefusal.NegativeCash(LocalDate.of(2026, 1, 2), 50_000L), refusal(result))
    }

    @Test
    fun backdatedBuy_thatMakesALaterDateNegative_isRefused_namingThatLaterDate() {
        val existing = listOf(opening(cents = 100_000L), cash("2026-01-05", -30_000L, 2L, CashType.WITHDRAWAL))
        val result = buy(existing, "2026-01-03", "1", 80_000L)
        assertEquals(BuyRefusal.NegativeCash(LocalDate.of(2026, 1, 5), 10_000L), refusal(result))
    }

    @Test
    fun futureDatedEntries_countInTheReplay() {
        val existing = listOf(opening(cents = 100_000L))
        val later = created(buy(existing, "2026-12-01", "1", 60_000L))
        val result = buy(existing + later.cashLine, "2026-01-02", "1", 60_000L, transactions = listOf(later.transaction))
        assertEquals(BuyRefusal.NegativeCash(LocalDate.of(2026, 12, 1), 20_000L), refusal(result))
    }

    @Test
    fun exampleC_aBackdatedWithdrawal_isBlockedByTheBuy() {
        val opening = opening(cents = 100_000L)
        val bought = created(buy(listOf(opening), "2026-01-10", "1", 80_000L))
        val withdrawal =
            CashEntries.create(
                existing = listOf(opening, bought.cashLine),
                id = "w",
                type = CashType.WITHDRAWAL,
                date = LocalDate.of(2026, 1, 5),
                amountCents = 30_000L,
                note = "",
            )
        assertEquals(
            CashEntryResult.Refused(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 10_000L)),
            withdrawal,
        )
    }

    @Test
    fun buy_whenTheReplayWouldOverflow_isTooLarge() {
        val existing = listOf(opening(cents = Long.MAX_VALUE), cash("2026-01-03", 2L, 2L))
        assertEquals(BuyRefusal.TooLarge, refusal(buy(existing, "2026-01-02", "1", 1L)))
    }

    @Test
    fun nextSeq_isTheHighestOfCashEntriesAndTransactionsPlusOne() {
        val transaction =
            Transaction(
                id = "t",
                portfolioId = "p1",
                ticker = "AAPL",
                type = TransactionType.BUY,
                date = LocalDate.of(2026, 1, 2),
                enteredSeq = 7L,
                quantity = BigDecimal.ONE,
                totalCents = 1_000L,
            )
        assertEquals(8L, Transactions.nextSeq(listOf(opening()), listOf(transaction)))
        assertEquals(2L, Transactions.nextSeq(listOf(opening()), emptyList()))
    }

    @Test
    fun pricePerShare_isTheTotalOverTheQuantity_exactly() {
        assertEquals(Fraction.of(BigInteger.valueOf(150), BigInteger.ONE), Transactions.pricePerShare(150_000L, BigDecimal("10")))
        assertEquals(Fraction.of(BigInteger.valueOf(1_000), BigInteger.valueOf(3)), Transactions.pricePerShare(100_000L, BigDecimal("3")))
    }
}
